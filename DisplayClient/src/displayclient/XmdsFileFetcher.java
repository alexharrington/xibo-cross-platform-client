/**
 * Copyright Alex Harrington, 2008.
 * 
 * This file is part of Xibo.
 * 
 * Xibo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Xibo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with Xibo. If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package displayclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jdom2.Element;

public class XmdsFileFetcher extends FileFetcher {

	private String type;
	private String path;
	private String id;
	private int size;
	private String md5;
	private DownloadManager parent;
	private int offset;
	private int chunk;
		
	public XmdsFileFetcher(DownloadManager parent, Element n) {
		this.parent = parent;
		this.type = n.getAttributeValue("type");
		this.path = n.getAttributeValue("path");
		this.id = n.getAttributeValue("id");
		if (n.getAttribute("size") != null) {
			try { this.size = n.getAttribute("size").getIntValue(); } catch (DataConversionException e) {}
		}
		this.md5 = n.getAttributeValue("md5");
		offset = 0;
		chunk = 512000;
	}
	
	public void run() {
		DisplayClient.debug.log(10, "FileFetcher: (type=" + type + ") (path=" + path + ") (id=" + id + ") (size=" + size + ") + (md5=" + md5 + ")");
		
		if (type.equals("media")) {
			// TODO: Write actual downloading
			File mediaFile = new File(DisplayClient.config.getString("storage.layouts") +"/" + path);
			if (! md5FromFile(mediaFile).equals(this.md5)) {
				// The current file differs from the one being sent
				// or does not exist already.
				// Download it.
				downloadMedia(mediaFile);
			}
		}
		else if (type.equals("layout")) {
			File layoutFile = new File(DisplayClient.config.getString("storage.layouts") +"/" + path + ".xlf");
			if (! md5FromFile(layoutFile).equals(this.md5)) {
				// The current file differs from the one being sent
				// or does not exist already.
				// Download it.
				downloadXLF(layoutFile);
			}
		}
		else if (type.equals("blacklist")) {
			// TODO: Write actual downloading
		}
		parent.threadCompleteNotify(this);
	}

	public String md5FromFile(File f) {
		String output = "";
		InputStream is;
		
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			is = new FileInputStream(f);
			byte[] buffer = new byte[8192];
			int read = 0;
			
			while ((read = is.read(buffer)) > 0) {
				digest.update(buffer, 0, read);	
			}
			byte[] md5sum = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5sum);
			output = bigInt.toString(16);
			is.close();
		} 
		catch (NoSuchAlgorithmException e) {
			// Unable to compute an MD5. If this is the case, I
			// suspect that's the least of our worries.
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			// File Disappeared from under us. Pretend that the MD5 didn't match
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return output;
	}

	public String md5FromString(String s) {
		String output = "";
				
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(s.getBytes(), 0, s.length());	
			byte[] md5sum = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5sum);
			output = bigInt.toString(16);
		} 
		catch (NoSuchAlgorithmException e) {
			// Unable to compute an MD5. If this is the case, I
			// suspect that's the least of our worries.
			e.printStackTrace();
		}

		return output;
	}
	
	public void downloadMedia(File f) {
		byte[] response;
		boolean finished = false;
		int tries = 0;
		
		if (f.exists()) {
			f.delete();
		}
		
		boolean append = true;
		FileOutputStream out = null;
		
		try {
			f.createNewFile();
			out = new FileOutputStream(f,append);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		while (tries < 5 && !finished) {
			tries++;
			while (this.offset < this.size) {
				if ((this.offset + this.chunk) > this.size) {
					this.chunk = this.size - this.offset;
				}
				try {
					response = DisplayManager.xmds.getFile(DisplayClient.config.getString("webservice.serverKey"), DisplayManager.getHardwareKey(), this.path, this.type, this.offset, this.chunk, "" + DisplayClient.XMDSSchemaVersion);
					out.write(response);
					out.flush();
					this.offset = this.offset + this.chunk;
				} catch (RemoteException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
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
				DisplayClient.debug.log(10, "FileFetcher: xmds.getFile(" + DisplayClient.config.getString("webservice.serverKey") + "," + DisplayManager.getHardwareKey() + "," + this.path + "," + this.type + ",0,0," + DisplayClient.XMDSSchemaVersion + ") Try: " + tries);
				response = DisplayManager.xmds.getFile(DisplayClient.config.getString("webservice.serverKey"), DisplayManager.getHardwareKey(), this.path, this.type, 0, 0, "" + DisplayClient.XMDSSchemaVersion);
				DisplayClient.debug.log(10, "FileFetcher: xmds.getFile => " + response.length + " bytes.");
				
				// Decode the Base64 representation
				String tmp = new String(response);
								
				if (md5FromString(tmp + "\n").equals(this.md5)) {
					if (!f.exists()) {
						try {
							FileWriter out = new FileWriter(f);
							out.write(tmp + "\n");
							out.close();
							finished = true;
						} catch (IOException e) {
							DisplayClient.debug.log(10,	"FileFetcher: xmds.getFile => Could not create/write file " + f.getAbsolutePath());
						}
					} else {
						try {
							FileWriter out = new FileWriter(f,false);
							out.write(tmp + "\n");
							out.close();
							finished = true;
						} catch (IOException e) {
							DisplayClient.debug.log(10, "FileFetcher: xmds.getFile => Could not write file " + f.getAbsolutePath());
						}
					}
				}
				else {
					DisplayClient.debug.log(10, "FileFetcher: xmds.getFile => Download completed but MD5s did not match.");
				}
			}
			catch (RemoteException e1) {
				// Webservice errored on our call for this file.
				DisplayClient.debug.log(0, "FileFetcher: Could not connect to XMDS at " + DisplayClient.config.getString("webservice.url") + " " + e1);
				
			}
		}
		
		if (! finished) {
			// TODO: Blacklist the item as we couldn't download it.
		}
	}

	public String getID() {
		return this.id;
	}

}