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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.bbop.phylo.annotate.AnnotationUtil;
import org.bbop.phylo.config.TouchupConfig;
import org.bbop.phylo.config.TouchupYaml;
import org.bbop.phylo.gaf.GafPropagator;
import org.bbop.phylo.model.Family;
import org.bbop.phylo.model.Tree;
import org.bbop.phylo.panther.IDmap;
import org.bbop.phylo.panther.PantherAdapter;
import org.bbop.phylo.panther.PantherFileAdapter;
import org.bbop.phylo.tracking.LogAction;
import org.bbop.phylo.tracking.LogAlert;
import org.bbop.phylo.tracking.LogUtil;
import org.bbop.phylo.tracking.Logger;
import org.bbop.phylo.util.Constant;
import org.bbop.phylo.util.FileUtil;
import org.bbop.phylo.util.OWLutil;
import org.bbop.phylo.util.TaxonChecker;

import owltools.gaf.io.ResourceLoader;

public class Touchup {
	/**
	 *
	 */
	protected Thread runner;

	private static String[] args;

	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Touchup.class);

	private static final String yaml_file = "config/preferences.yaml";
	/**
	 * Method declaration
	 *
	 *
	 * @param args either a family name, a file containing a list of family names, or a directory where the families are
	 *
	 * @see
	 */
	public static void main(final String[] args) {
		Touchup.args = args;
		Touchup theRunner = new Touchup();

		Runtime.getRuntime().addShutdownHook(new Thread(theRunner.mainRun) {
			public void run() {
				TouchupConfig.inst().save(yaml_file);
			}
		});
		
		SwingUtilities.invokeLater(theRunner.mainRun);

	}

	private final Runnable mainRun =
			new Runnable() {
		// this thread runs in the AWT event queue
		public void run() {
			int family_count;
			if (args.length == 0 || (args.length % 2) == 1) {
				provideHelp();
				System.exit(0);
			}
			TouchupYaml configManager = new TouchupYaml();
			// Attempt to parse the given config file.
			configManager.loadConfig(yaml_file);

			List<String> families;
			String start_with_family = null;
			String family_file = null;
			String family_dir = TouchupConfig.inst().gafdir;
			String family_list = null;
			for (int i = 0; i < args.length; i += 2) {
				if (args[i].contains("t")) {
					TouchupConfig.inst().treedir = args[i + 1];
					log.info(TouchupConfig.inst().treedir);
				} else if (args[i].contains("s")) {
					start_with_family = args[i + 1];
				} else if (args[i].contains("f")) {
					family_file = args[i + 1];
				} else if (args[i].contains("d")) {
					File f = new File(args[i + 1]);
					if (f.isDirectory()) {
						TouchupConfig.inst().gafdir = args[i + 1];
						family_dir = args[i + 1];
					}
				} else if (args[i].contains("l")) {
					family_list = args[i + 1];
				} else {
					provideHelp();
					System.exit(0);
				}
			}

			if (family_file == null && family_list == null) {
				log.info("Retrieving families from this directory: " + TouchupConfig.inst().gafdir);
				File f = new File(family_dir);
				String [] files = f.list();
				Arrays.sort(files);
				families = new ArrayList<>();
				boolean start = false;
				for (String file : files) {
					if (file.startsWith("PTHR")) {
						start |= start_with_family == null || file.startsWith(start_with_family);
						if (start) {
							families.add(file);
						}
					}
				}
			} else if (family_list != null) {
				families = FileUtil.readFile(new File(family_file));
				for (int i = families.size() - 1; i >= 0; i--) {
					// allow for commenting out lines in the input file
					if (families.get(i).length() == 0 || families.get(i).startsWith("//")) {
						families.remove(i);
					}
				}
			} else {
				families = new ArrayList<>();
				if (family_file.contains("PTHR")) {
					families.add(family_file);
				}
			}
			try {
				family_count = touchup(families);
				log.info("Touched up " + family_count + " PAINT families");
				System.exit(0);
			}
			catch (Exception e) { // should catch RuntimeException
				e.printStackTrace();
				System.exit(1);
			}
		}
	};

	private int touchup(List<String> families) {
		log.info(families.size() + " families to touch up");
		Map<String, List<String>> run_summary = new HashMap<>();
		int family_count = 0;
		int gaf_count = 0;
		int review_count = 0;
		int tree_count = 0;
		for (String family_name : families) {
			log.info("Touching up " + family_name + " (" + (families.indexOf(family_name) + 1) + " of " + families.size() + ")");
			clear();
			boolean available = gafFileExists(family_name);
			if (!available) {
				log.info("Missing GAF file for " + family_name);
			} else {
				gaf_count++;
				Family family = new Family(family_name);
				Tree tree = new Tree(family_name);
				PantherAdapter adapter = new PantherFileAdapter();
				available &= family.fetch(tree, adapter);
				if (available) {
					boolean proceed = TaxonChecker.isLive();
					if (proceed) {
						proceed &= AnnotationUtil.loadExperimental(family);
						if (proceed) {
							/*
							 * The file may be null, in which case the following two methods
							 * simply return
							 */
							File family_dir = new File(TouchupConfig.inst().gafdir, family_name);

							Logger.importPrior(family_name, family_dir);

							GafPropagator.importAnnotations(family, family_dir);
							
							family.save(family_dir, "Updated by: " + ResourceLoader.inst().loadVersion());
							int alert_count = LogAlert.getAlertCount();
							if (alert_count > 0)  {
								run_summary.put(family_name, LogAlert.report());                            	
								review_count++;
							} else {
								run_summary.put(family_name, null);
							}
						} else {
							log.error("Unable to load annotations for " + family_name);
						}
					} else {
						log.error("TaxonChecker is down");
					}
					if (!proceed) {
						logSummary(run_summary, families.size(), family_count, tree_count, gaf_count, review_count);
						return run_summary.size();
					}
					tree_count++;
				} else {
					log.error("Unable to load tree for " + family_name);
				}
			}
			family_count++;
		}
		logSummary(run_summary, families.size(), family_count, tree_count, gaf_count, review_count);
		return run_summary.size();
	}

	private void clear() {
		IDmap.inst().clearGeneIDs();
		LogAction.clearLog();
		LogAlert.clearLog();
		OWLutil.inst().clearTerms();
		System.gc();
	}
	
	private void logSummary(Map<String, List<String>> summaries, int total_fams, int family_count, int tree_count, int gaf_count, int review_count) {
		String program_name = ResourceLoader.inst().loadVersion();
		File log_dir = new File(TouchupConfig.inst().gafdir);
		if (FileUtil.validPath(log_dir)) {
			File logFileName = new File(log_dir, program_name + Constant.LOG_SUFFIX);
			List<String> contents = new ArrayList<>();
			contents.add("# " + program_name + " Log Report for " + LogUtil.dateNow());
			contents.add("");
			contents.add("\n");
			Set<String> families = summaries.keySet();
			String[] fam_array = families.toArray(new String[families.size()]);
			Arrays.sort(fam_array);
			for (String family_name : families) {
				List<String> alerts = summaries.get(family_name);
				if (alerts != null) {
					contents.add(family_name +  " needs review ---\n");
					contents.addAll(alerts);
				}
			}
			try {
				contents.set(1, "Touched up " + summaries.size() + " of " + total_fams + " PAINT families " + 
						(total_fams - family_count) + " no longer have trees, " +
						(family_count - gaf_count) + " are missing GAF files, and " + 
						review_count + " need reviewing.");
				FileUtil.writeFile(logFileName, contents);
			} catch (IOException e) {
				log.error("Unable to log touchup summary: " + e.getMessage());
			}
		}
	}

	private boolean gafFileExists(String family_name) {
		File family_dir = new File(TouchupConfig.inst().gafdir, family_name);
		boolean ok = FileUtil.validPath(family_dir);
		if (ok) {
			File gaf_file = new File(family_dir, family_name + Constant.GAF_SUFFIX);
			ok = FileUtil.validFile(gaf_file);
			if (!ok) {
				log.info("Can't read: " + gaf_file);
			}
		} else {
			log.info("Invalid path: " + family_dir);
		}
		return ok;
	}

	private void provideHelp() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("USAGE:\n");
		buffer.append("\t -f <familyname> // touches up a single PAINT family\n");
		buffer.append("\t -l <filename> // obtains family names from contents of the file\n");
		buffer.append("\t -d <directoryname> // touches up families listed in this directory\n");
		buffer.append("\t -d <directoryname> -s <starting family name> // as above, but solely the alphabetically latter part of directory\n");
		buffer.append("\n\t // optionally a -t argument may be added to any of the above to indicate the correct PANTHER tree directory\n");
		log.error(buffer);
	}
}
