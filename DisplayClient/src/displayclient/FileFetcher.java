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
import java.util.logging.Logger;

import org.jdom2.DataConversionException;
import org.jdom2.Element;

public class FileFetcher extends Thread {

	private String type;
	private String path;
	private String id;
	private int size;
	private String md5;
	private DownloadManager parent;
	private int offset;
	private int chunk;
        protected final static Logger log = Logger.getLogger(DisplayClient.class.getName());
		
	public FileFetcher(DownloadManager parent, Element n) {
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
	
        @Override
	public void run() {
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
		catch (NoSuchAlgorithmException | IOException e) {
			// Unable to compute an MD5. If this is the case, I
			// suspect that's the least of our worries.
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
	}
	
	public void downloadXLF(File f) {
	}

	public String getID() {
		return this.id;
	}

}