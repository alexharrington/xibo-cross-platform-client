/**
 * Copyright Alex Harrington, 2008.
 *
 * This file is part of Xibo.
 *
 * Xibo is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Xibo is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Xibo. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package displayclient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom2.Element;

public class XmdsFileFetcher extends FileFetcher {

    public XmdsFileFetcher(DownloadManager parent, Element n) {
        super(parent, n);
    }

    @Override
    public void run() {
        log.log(Level.FINER, "FileFetcher: (type={0}) (path={1}) (id={2}) (size={3}) + (md5={4})", new Object[]{this.type, this.path, this.id, this.size, this.md5});

        switch (this.type) {
            case "media":
                // TODO: Write actual downloading
                File mediaFile = new File(DisplayClient.prop.getProperty("LibraryPath") + "/" + this.path);
                if (!md5FromFile(mediaFile).equals(this.md5)) {
                    // The current file differs from the one being sent
                    // or does not exist already.
                    // Download it.
                    downloadMedia(mediaFile);
                }
                break;
            case "layout":
                File layoutFile = new File(DisplayClient.prop.getProperty("LibraryPath") + "/" + this.path + ".xlf");
                if (!md5FromFile(layoutFile).equals(this.md5)) {
                        // The current file differs from the one being sent
                    // or does not exist already.
                    // Download it.
                    downloadXLF(layoutFile);
                }
                break;
            case "blacklist":
                break;
            case "resource":
                break;
        }
        parent.threadCompleteNotify(this);
    }

    /**
     *
     * @param f
     */
    @Override
    public void downloadMedia(File f) {
        byte[] response;
        boolean finished = false;
        int tries = 0;

        if (f.exists()) {
            f.delete();
        }

        boolean append = true;
        FileOutputStream out = null;
        
        System.out.println(f);

        try {
            f.createNewFile();
            out = new FileOutputStream(f, append);
        } catch (IOException ex) {
            Logger.getLogger(XmdsFileFetcher.class.getName()).log(Level.SEVERE, null, ex);
        }

        while (tries < 5 && !finished) {
            tries++;
            while (this.offset < this.size) {
                if ((this.offset + this.chunk) > this.size) {
                    this.chunk = this.size - this.offset;
                }
                try {
                    response = this.parent.DC.XMDS.GetFile(DisplayClient.prop.getProperty("ServerKey"), DisplayClient.prop.getProperty("HardwareKey"), this.id, this.type, this.offset, this.chunk);
                    out.write(response);
                    out.flush();
                    this.offset = this.offset + this.chunk;
                } catch (RemoteException ex) {
                    Logger.getLogger(XmdsFileFetcher.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(XmdsFileFetcher.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception ex) {
                    Logger.getLogger(XmdsFileFetcher.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            try {
                out.close();
            } catch (IOException e) {
            }

            finished = true;
        }
    }

    public void downloadXLF(File f) {
        byte[] response;
        boolean finished = false;
        int tries = 0;

        while (tries < 5 && !finished) {
            tries++;
            try {
                Logger.getLogger(XmdsFileFetcher.class.getName()).log(Level.INFO, null, "FileFetcher: xmds.getFile(" + DisplayClient.prop.getProperty("ServerKey") + "," + DisplayClient.prop.getProperty("HardwareKey") + "," + this.path + "," + this.type + ",0,0) Try: " + tries);
                response = this.parent.DC.XMDS.GetFile(DisplayClient.prop.getProperty("ServerKey"), DisplayClient.prop.getProperty("HardwareKey"), this.id, this.type, 0.0, 0.0);
                Logger.getLogger(XmdsFileFetcher.class.getName()).log(Level.INFO, null, "FileFetcher: xmds.getFile => " + response.length + " bytes.");

                // Decode the Base64 representation
                String tmp = new String(response);

                if (md5FromString(tmp).equals(this.md5)) {
                    if (!f.exists()) {
                        try {
                            FileWriter out = new FileWriter(f);
                            out.write(tmp);
                            out.close();
                            finished = true;
                        } catch (IOException ex) {
                            Logger.getLogger(XmdsFileFetcher.class.getName()).log(Level.INFO, null, "FileFetcher: xmds.getFile => Could not create/write file " + f.getAbsolutePath());
                        }
                    } else {
                        try (FileWriter out = new FileWriter(f, false)) {
                            out.write(tmp);
                            finished = true;
                        }
                        catch (IOException ex) {
                            Logger.getLogger(XmdsFileFetcher.class.getName()).log(Level.INFO, null, "FileFetcher: xmds.getFile => Could not write file " + f.getAbsolutePath());
                        }
                    }
                } else {
                    Logger.getLogger(XmdsFileFetcher.class.getName()).log(Level.SEVERE, null, "FileFetcher: xmds.getFile => Download completed but MD5s did not match.");
                }
            }
            catch (RemoteException ex) {
                // Webservice errored on our call for this file.
                Logger.getLogger(XmdsFileFetcher.class.getName()).log(Level.INFO, null, "FileFetcher: Could not connect to XMDS at " + DisplayClient.prop.getProperty("ServerUri") + " " + ex);
            }
            catch (Exception ex) {
                Logger.getLogger(XmdsFileFetcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (!finished) {
            // TODO: Blacklist the item as we couldn't download it.
        }
    }
}
