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

import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

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

	protected String gafdir = "/Users/yoyo/projects/go/gene-associations/submission/paint/";

	protected String treedir = "/Users/yoyo/projects/go/data/trees/panther/";

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

	protected static ClassLoader getExtensionLoader() {
		return DirectoryUtil.class.getClassLoader();
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public void setGafdir(String gafdir) {
		this.gafdir = gafdir;
	}

	public void setTreedir(String treedir) {
		this.treedir = treedir;
	}

	public String getGafdir() {
		return gafdir;
	}

	public String getTreedir() {
		return treedir;
	}

}
