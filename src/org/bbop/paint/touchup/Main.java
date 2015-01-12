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

package org.bbop.paint.touchup;

import org.apache.log4j.Logger;
import org.bbop.paint.gaf.GafPropagator;
import org.bbop.paint.model.Family;
import org.bbop.paint.model.History;
import org.bbop.paint.panther.IDmap;
import org.bbop.paint.util.AnnotationUtil;
import org.bbop.paint.util.FileUtil;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {
	/**
	 * 
	 */
	protected Thread runner;

	private static String[] args;

	private Family family;
	private final boolean from_server = true;

	private static final Logger log = Logger.getLogger(Main.class);

	/**
	 * Method declaration
	 *
	 *
	 * @param args
	 *
	 * @see
	 */
	public static void main(final String[] args) {
		Main.args = args;
		Main theRunner = new Main();

		SwingUtilities.invokeLater(theRunner.mainRun);

		Runtime.getRuntime().addShutdownHook(new Thread(theRunner.mainRun) {
			public void run() {
				Preferences.inst().writePreferences(Preferences.inst());
			}
		});
	}

	private final Runnable mainRun =
			new Runnable() {
		// this thread runs in the AWT event queue
		public void run() {
			if (args.length == 0) {
				System.err.println("Please provide a family ID.");
				System.exit(1);
			}

			if (args.length > 1)
				Preferences.inst().setBasedir(args[1]);
			try {
				int family_count = touchup(args[0]);
				log.info("Touched up " + family_count + " PAINT families");
			System.exit(0);
			}
			catch (Exception e) { // should catch RuntimeException
				e.printStackTrace();
				System.exit(2);
			}
		}
	};

	private int touchup(String arg) {
		int family_count = 0;
		File f = new File(arg);
		List<String> families;

		if (f.isDirectory()) {
			String [] files = f.list();
			families = new ArrayList();
			for (String file : files) {
				if (file.startsWith("PTHR")) {
					families.add(file);
				}
			}
		} else if (f.canRead()) {
			families = FileUtil.readFile(arg);
			for (int i = families.size() - 1; i <= 0; i--) {
				// allow for commenting out lines in the input file
				if (families.get(i).length() == 0 || families.get(i).startsWith("//")) {
					families.remove(i);
				}
			}
		} else {
			families = new ArrayList<>();
			families.add(arg);
		}
		log.info(families.size() + " families to touch up");
		for (String family_name : families) {

			log.info("Touching up " + family_name + " (" + (families.indexOf(family_name)+1) + " of " + families.size() + ")");
			if (family_name.length() > 0 && loadPaint(family_name)) {
				savePaint();
				History.write("TouchUp 1.0", family_name);
				family_count++;
			} else {
				log.error("Unable to load " + family_name);
			}
		}
		return family_count;
	}

	private boolean loadPaint(String family_name) {
		IDmap.inst().clearGeneIDs();
		family = new Family();
		boolean loaded = family.fetch(family_name, from_server);
		if (loaded) {
			AnnotationUtil.collectExpAnnotations(family);

			/*
			 * The file may be null, in which case the following two methods
			 * simply return
			 */
			History.importPrior(family_name);

			GafPropagator.importAnnotations(family);

		} else {
			family = null;
		}
		return family != null;
	}

	private void savePaint() {
		if (family != null) {
			// Need to add the supporting experimental annotations to the withs
			family.save();
		}
	}

}
