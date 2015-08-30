/* 
 * 
 * Copyright (c) 2010, Regents of the University of California 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Neither the name of the Lawrence Berkeley National Lab nor the names of its contributors may be used to endorse 
 * or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.bbop.phylo.util;

import java.awt.Color;
import java.awt.Font;
import java.beans.DefaultPersistenceDelegate;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * Used for reading previous or default user settings from property file and storing current user settings
 */

public class DirectoryUtil {
	/**
	 *
	 */

	protected static Logger log = Logger.getLogger("org.bbop.phylo.DirectoryUtil");

	private static DirectoryUtil directory;

	private String gafdir = "/Users/suzi/projects/go/gene-associations/submission/paint/";
	private String treedir = "/Users/suzi/projects/go/data/trees/panther/";

	/**
	 * Constructor declaration
	 * @throws Exception
	 *
	 *
	 * @see
	 */
	public DirectoryUtil() { //throws Exception {
	}

	public static DirectoryUtil inst() {
		if (directory == null) {
			XMLDecoder d;
			try {
				d = new XMLDecoder(new BufferedInputStream(new FileInputStream(
						DirectoryUtil.getPrefsXMLFile())));
				DirectoryUtil p = (DirectoryUtil) d.readObject();
				directory = (DirectoryUtil) p;
				d.close();
			} catch (Exception e) {
				log.info("Could not read preferences file from "
						+ DirectoryUtil.getPrefsXMLFile());
			}
			if (directory == null) {
				synchronized (DirectoryUtil.class) {
					if (directory == null) {
						directory = new DirectoryUtil();
					}
				}
			}
		}
		return directory;
	}

	protected static File getPrefsXMLFile() {
		return new File(getPaintPrefsDir(), "directoryutil.xml");
	}

	protected static File getPaintPrefsDir() {
		File f = new File("config");
		f.mkdirs();
		return f;
	}

	public static void writePreferences(DirectoryUtil dir_util) {
		try {
			XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
					new FileOutputStream(getPrefsXMLFile())));
			log.info("Writing directory preferences to " + getPrefsXMLFile());
			encoder.setPersistenceDelegate(Font.class,
					new DefaultPersistenceDelegate(
							new String[]{ "name",
									"style",
							"size" }) );
			encoder.setPersistenceDelegate(Color.class,
					new DefaultPersistenceDelegate(
							new String[]{ "red",
									"green",
							"blue" }) );
			encoder.writeObject(dir_util);
			encoder.close();

		} catch (IOException ex) {
			log.info("Could not write verification settings!");
			ex.printStackTrace();
		}
	}

	protected static ClassLoader getExtensionLoader() {
		return DirectoryUtil.class.getClassLoader();
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public void setGafDir(String gafdir) {
		this.gafdir = gafdir;
	}

	public void setTreeDir(String treedir) {
		this.treedir = treedir;
	}

	public String getGafDir() {
		return gafdir;
	}

	public String getTreeDir() {
		return treedir;
	}

}
