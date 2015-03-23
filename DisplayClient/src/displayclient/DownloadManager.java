/*
 * Xibo - Digital Signage - http://www.xibo.org.uk
 * Copyright (C) 2008-2015 Alex Harrington
 *
 * This file is part of Xibo.
 *
 * Xibo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version. 
 *
 * Xibo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Xibo.  If not, see <http://www.gnu.org/licenses/>.
 */
package displayclient;

/**
 *
 * @author Alex Harrington - Spring Signage Ltd
 */
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

public class DownloadManager extends Thread {

    protected boolean running = true;
    protected int numThreads;
    protected int numThreadsLimit;
    private ArrayList<FileFetcher> runningDownloads;
    private final static Logger log = Logger.getLogger(DownloadManager.class.getName());
    protected final DisplayClient DC;

    public DownloadManager(DisplayClient parent) {
        log.addHandler(new ConsoleHandler());
        log.setLevel(Level.ALL);
        this.DC = parent;

        this.numThreads = 0;
        this.runningDownloads = new ArrayList<>();

        log.log(Level.INFO, "Init. Max Download threads = {0}", this.numThreadsLimit);
    }

    @Override
    public void run() {
        int updateInterval = Integer.parseInt(DisplayClient.prop.getProperty("CollectInterval"));
        log.log(Level.INFO, "Run. Collection Interval = {0}", updateInterval);

        while (running) {
                        // Update thread limits and updateInterval incase they changed on
            // instruction from the CMS
            this.numThreadsLimit = Integer.parseInt(DisplayClient.prop.getProperty("MaxConcurrentDownloads"));
            updateInterval = Integer.parseInt(DisplayClient.prop.getProperty("CollectInterval"));

            String response = "";
            try {
                response = this.DC.XMDS.RequiredFiles(DisplayClient.prop.getProperty("ServerKey"), DisplayClient.prop.getProperty("HardwareKey"));
                log.log(Level.FINE, "DownloadManager: xmds.requiredFiles => {0}", response);
            } catch (Exception ex) {
                log.log(Level.FINEST, "DownloadManager: xmds.requiredFiles => {0}", response);
                log.log(Level.WARNING, "DownloadManager: Could not connect to XMDS at {0}", DisplayClient.prop.getProperty("ServerUri"));
                log.log(Level.WARNING, "DownloadManager: xmds.requiredFiles => {0}", ex);
                continue;
            }

            Document doc;
            Iterator iter;

            if (!response.equals("")) {
                // Code here to parse the returned XML
                SAXBuilder sb = new SAXBuilder();

                try {
                    doc = sb.build(new StringReader(response));

                    // Build an iterator over the files section of the response
                    XPathFactory xFactory = XPathFactory.instance();

                    XPathExpression<Element> expr = xFactory.compile("//file", Filters.element());
                    List<Element> files = expr.evaluate(doc);
                    for (Element n : files) {
                        
                        FileFetcher f = null;
                        
                        if (n.getAttributeValue("download").equalsIgnoreCase("http")) {
                            f = new HttpFileFetcher(this, n); 
                        }
                        else {
                            f = new XmdsFileFetcher(this, n);
                        }

                        Boolean skip = false;

                        // Check to see if there is already a download
                        // running for this file ID. If there is, skip it.
                        for (FileFetcher tmp : runningDownloads) {
                            try {
                                if (f.getID() == tmp.getID()) {
                                    skip = true;
                                    log.log(Level.FINEST, "DownloadManager: FileFetcher {0} is still running from previous collection. Skipping.", f.getID());
                                }
                            } catch (NullPointerException e) {
                                                    // The download completed while we were testing for it.
                                // Ignore it.
                                skip = true;
                            }
                        }

                        if (!skip) {
                            f.start();
                            this.runningDownloads.add(f);
                            this.numThreads++;
                        }

                        while (this.numThreads >= (this.numThreadsLimit - 1)) {
							// Thread throttling
                            // Sleep until there is a spare space for a new thread.
                            try {
                                log.log(Level.FINE, "DownloadManager: {0} downloads in progress. Sleeping.", this.numThreadsLimit);
                                sleep(5000);
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                } catch (JDOMException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Make sure updateInterval is sane.
            if (updateInterval < 30) {
                updateInterval = 30;
            }

            // Sleep for "updateInterval" seconds before trying again.
            try {
                sleep(updateInterval * 1000);
            } catch (InterruptedException e) {
            }
        }
    }

    protected void threadCompleteNotify(FileFetcher f) {
        log.log(Level.FINE, "DownloadManager: FileFetcher {0} completed.", f.getID());
        if (this.numThreads > 0) {
            this.numThreads = this.numThreads - 1;
        }
        this.runningDownloads.remove(f);
    }
}
