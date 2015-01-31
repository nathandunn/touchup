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

package org.bbop.phylo.touchup;

import org.apache.log4j.Logger;

import java.awt.*;
import java.beans.DefaultPersistenceDelegate;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;

/**
 * Used for reading previous or default user settings from property file and storing current user settings
 */

public class Preferences {
	/**
	 *
	 */
	private static final long serialVersionUID = -5472475387423113108L;

	private String gafdir = "/Users/suzi/projects/go/gene-associations/submission/paint/";
	private String treedir = "/Users/suzi/projects/go/data/trees/panther/";

	private static final Logger log = Logger.getLogger(Preferences.class);

	private static Preferences preferences;

	/**
	 * Constructor declaration
	 * @throws Exception
	 *
	 *
	 * @see
	 */
	public Preferences() { //throws Exception {
	}

	public static Preferences inst() {
		if (preferences == null) {
			XMLDecoder d;
			try {
				d = new XMLDecoder(new BufferedInputStream(new FileInputStream(
						Preferences.getPrefsXMLFile())));
				preferences = (Preferences) d.readObject();
				d.close();
			} catch (Exception e) {
				log.info("Could not read preferences file from "
						+ Preferences.getPrefsXMLFile());
			}
			if (preferences == null)
				preferences = new Preferences();
		}
		return preferences;
	}

	void writePreferences(Preferences preferences){
		try {
			XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
					new FileOutputStream(getPrefsXMLFile())));
			log.info("Writing preferences to " + getPrefsXMLFile());
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
			encoder.writeObject(preferences);
			encoder.close();
		} catch (IOException ex) {
			log.info("Could not write verification settings!");
			ex.printStackTrace();
		}
	}

	private static File getPrefsXMLFile() {
		return new File(getPaintPrefsDir(), "preferences.xml");
	}

	private static File getPaintPrefsDir() {
		return new File("config");
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
