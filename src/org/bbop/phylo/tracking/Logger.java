package org.bbop.phylo.tracking;

import org.bbop.phylo.touchup.Constant;
import org.bbop.phylo.touchup.Preferences;
import org.bbop.phylo.util.FileUtil;
import org.bbop.phylo.util.ResourceLoader;
import org.bbop.phylo.util.TaxonFinder;
import owltools.gaf.Bioentity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Logger {

	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Logger.class);

	public static final String MF_SECTION = "# molecular_function";
	public static final String CC_SECTION = "# cellular_component";
	public static final String BP_SECTION = "# biological_process";
	public static final String PRUNED_SECTION = "# Pruned";
	public static final String WARNING_SECTION = "# WARNINGS - THE FOLLOWING HAVE BEEN REMOVED FOR THE REASONS NOTED";
	private static final String DATE_PREFIX = "201";
	private static final String NOTES_SECTION = "# Notes";
	private static final String REF_SECTION = "# Reference";

	private static List<String> notes;

	public static void write(String family_name) {
		String family_dir = Preferences.inst().getGafdir() + family_name + File.separator;

		if (FileUtil.validPath(family_dir)) {
			String logFileName = family_dir + family_name + Constant.LOG_SUFFIX;
			List<String> contents = new ArrayList<>();
			String program_name = ResourceLoader.inst().loadVersion();
			contents.add("# " + program_name + " Log Report for " + LogUtil.dateNow());
			LogAction.report(contents);
			LogAlert.report(contents);
			logNotes(contents);
			logBoilerplate(contents);
			try {
				FileUtil.writeFile(logFileName, contents);
			} catch (IOException e) {
				logger.error("Unable to log updates for " + family_name);
				logger.error(e.getMessage());
			}
		}
	}

	public static void importPrior(String family_name) {
		String family_dir = Preferences.inst().getGafdir() + family_name + File.separator;
		if (notes == null) {
			notes = new ArrayList<>();
		} else {
			notes.clear();
		}
		if (FileUtil.validPath(family_dir)) {
			String log_file = family_dir + family_name + Constant.OLDLOG_SUFFIX;
			if (!FileUtil.validFile(log_file)) {
				log_file = family_dir + family_name + Constant.LOG_SUFFIX;
			}
			if (FileUtil.validFile(log_file)) {
				List<String> log_content = FileUtil.readFile(log_file);
				if (log_content != null) {
					clearBoilerPlate(log_content);
					clearEditLog(log_content);
					for (int i = 0; i < log_content.size(); i++) {
						String line = log_content.get(i).trim();
						notes.add(line);
					}
				} else {
					logger.error("Couldn't read" + log_file);
				}
			} else {
				logger.error("Invalid path for log file " + log_file);
			}
		} else {
			logger.error("Invalid path for family directory " + family_dir);
		}
	}

	private static void logNotes(List<String> contents) {
		contents.add(NOTES_SECTION);
		contents.addAll(notes);
		contents.add("");
	}

	private static void logBoilerplate(List<String> contents) {
		contents.add(REF_SECTION);
		contents.add(Constant.GO_PUBLICATION);
		contents.add("");
	}

	private static void clearBoilerPlate(List<String> log_content) {
		for (int i = log_content.size() - 1; i >= 0; i--) {
			String line = log_content.get(i).trim();
			if (line.length() == 0) {
				log_content.remove(i);
			} else if (line.startsWith(REF_SECTION) ||
					line.contains(Constant.GO_REF_TITLE) ||
					line.contains(Constant.GO_REF_SW)) {
				log_content.remove(i);
			}
		}
	}

	private static void clearEditLog(List<String> log_content) {
		for (int i = log_content.size() - 1; i >= 0; i--) {
			String line = log_content.get(i).trim();
			if (line.contains(NOTES_SECTION) && !line.startsWith(NOTES_SECTION)) {
				line = line.substring(line.indexOf(NOTES_SECTION));
				log_content.set(i, line);
			}
			if (line.startsWith(NOTES_SECTION) && line.length() > NOTES_SECTION.length()) {
				line = line.substring(NOTES_SECTION.length());
				log_content.set(i, line);
			}
			if (line.startsWith(NOTES_SECTION) ||
					line.startsWith(DATE_PREFIX) ||
					line.startsWith(MF_SECTION) ||
					line.startsWith(CC_SECTION) ||
					line.startsWith(BP_SECTION) ||
					line.startsWith(PRUNED_SECTION) ||
					line.startsWith(WARNING_SECTION) ||
					line.startsWith("##")) {
				log_content.remove(i);
			}
		}
	}

	protected static String makeLabel(Bioentity node) {
		String species = TaxonFinder.getSpecies(node.getNcbiTaxonId());
		if (species == null) {
			species = node.getSpeciesLabel();
		}
		if (node.getParent() != null && (species.equals("LUCA") || species.equals("root"))) {
			// if this is not the root node, but the clade is unknown
			species = "node";
		}
		return (species + '_' + node.getDBID());
	}
}

