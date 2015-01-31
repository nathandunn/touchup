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

import org.bbop.phylo.annotate.AnnotationUtil;
import org.bbop.phylo.gaf.GafPropagator;
import org.bbop.phylo.model.Family;
import org.bbop.phylo.tracking.LogAction;
import org.bbop.phylo.tracking.LogAlert;
import org.bbop.phylo.tracking.LogUtil;
import org.bbop.phylo.tracking.Logger;
import org.bbop.phylo.panther.IDmap;
import org.bbop.phylo.util.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main {
	/**
	 * 
	 */
	protected Thread runner;

	private static String[] args;

	private Family family;

	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Main.class);

	/**
	 * Method declaration
	 *
	 *
	 * @param args either a family name, a file containing a list of family names, or a directory where the families are
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
			for (int i = families.size() - 1; i >= 0; i--) {
				// allow for commenting out lines in the input file
				if (families.get(i).length() == 0 || families.get(i).startsWith("//")) {
					families.remove(i);
				}
			}
		} else {
			families = new ArrayList<>();
			if (arg.startsWith("PTHR")) {
				families.add(arg);
			}
		}

		log.info(families.size() + " families to touch up");
		Map<String, FamilySummary> run_summary = new HashMap<>();
		for (String family_name : families) {
			log.info("Touching up " + family_name + " (" + (families.indexOf(family_name)+1) + " of " + families.size() + ")");
			clear();
			if (loadPaint(family_name)) {
				savePaint();
				Logger.write(family_name);
				FamilySummary family_summary = new FamilySummary();
				run_summary.put(family_name, family_summary);

			} else {
				log.error("Unable to load " + family_name);
			}
		}
		logSummary(run_summary);
		return run_summary.size();
	}

	private void clear() {
		IDmap.inst().clearGeneIDs();
		LogAction.clearLog();
		LogAlert.clearLog();
		OWLutil.clearTerms();
	}

	private boolean loadPaint(String family_name) {
	family = new Family();
		boolean available = gafFileExists(family_name) && family.fetch(family_name);
		if (available) {
			AnnotationUtil.collectExpAnnotations(family);

			/*
			 * The file may be null, in which case the following two methods
			 * simply return
			 */
			Logger.importPrior(family_name);

			GafPropagator.importAnnotations(family);

		} else {
			family = null;
			log.info("Unable to touchup " + family_name);
		}
		System.gc();
		return family != null;
	}

	private void savePaint() {
		if (family != null) {
			// Need to add the supporting experimental annotations to the withs
			family.save();
		}
	}

	private void logSummary(Map<String, FamilySummary> summaries) {
		String program_name = ResourceLoader.inst().loadVersion();
		String log_dir = Preferences.inst().getGafdir();

		if (FileUtil.validPath(log_dir)) {
			String logFileName = log_dir + program_name + Constant.LOG_SUFFIX;
			List<String> contents = new ArrayList<>();
			contents.add("# " + program_name + " Log Report for " + LogUtil.dateNow());
			contents.add("Touched up " + summaries.size() + " PAINT families");
			Set<String> families = summaries.keySet();
			for (String family_name : families) {
				FamilySummary report = summaries.get(family_name);
				report.summarize(family_name, contents);
			}
			try {
				FileUtil.writeFile(logFileName, contents);
			} catch (IOException e) {
				log.error("Unable to log touchup summary: " + e.getMessage());
			}
		}
	}

	private boolean gafFileExists(String family_name) {
		String family_dir = Preferences.inst().getGafdir() + family_name + File.separator;
		boolean ok = FileUtil.validPath(family_dir);
		if (ok) {
//			String prefix = Preferences.panther_files[0].startsWith(".") ? family_name : "";
			String gaf_file = family_dir + family_name + Constant.GAF_SUFFIX;
			File f = new File(gaf_file);
			ok = f.exists() && f.isFile() && f.canRead();
			if (!ok) {
				log.info("Can't read: " + gaf_file);
			}

		} else {
			log.info("Invalid path: " + family_dir);
		}
		return ok;
	}
}
