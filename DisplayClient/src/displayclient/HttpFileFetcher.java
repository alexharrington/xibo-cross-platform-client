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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.MalformedInputException;
import com.sun.net.ssl.internal.www.protocol.https.*;
import java.security.Security;

import org.jdom2.Element;

public class HttpFileFetcher extends FileFetcher {

    private final String saveAs;
    
    public HttpFileFetcher(DownloadManager parent, Element n) {
        super(parent, n);
        this.saveAs = n.getAttributeValue("saveAs");
    }

    @Override
    public void run() {
        log.log(Level.FINER, "FileFetcher: (type={0}) (path={1}) (id={2}) (size={3}) + (md5={4})", new Object[]{this.type, this.path, this.id, this.size, this.md5});

        switch (this.type) {
            case "media":
                File mediaFile = new File(DisplayClient.prop.getProperty("LibraryPath") + "/" + this.saveAs);
                if (!md5FromFile(mediaFile).equals(this.md5)) {
                    // The current file differs from the one being sent
                    // or does not exist already.
                    // Download it.
                    downloadMedia(mediaFile);
                }
                break;
            case "layout":
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

        if (f.exists()) {
            f.delete();
        }

        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        
        System.out.println("Downloading " + this.path + " to " + f.getAbsolutePath());
        
        URL url = null;
	URLConnection con = null;
	int i;
	try {
            url = new URL(this.path);
            con = url.openConnection();
            BufferedInputStream bis = new BufferedInputStream(
					con.getInputStream());
            BufferedOutputStream bos = new BufferedOutputStream(
					new FileOutputStream(f.getAbsolutePath()));
            while ((i = bis.read()) != -1) {
			bos.write(i);
            }
            bos.flush();
            bis.close();
            } catch (MalformedInputException malformedInputException) {
            	malformedInputException.printStackTrace();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
    }

    public void downloadXLF(File f) {
        // XLF always comes down over XMDS not HTTP
        return;
    }
}
