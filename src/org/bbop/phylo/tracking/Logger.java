package org.bbop.phylo.tracking;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bbop.phylo.util.Constant;
import org.bbop.phylo.util.FileUtil;

import owltools.gaf.Bioentity;
import owltools.gaf.species.TaxonFinder;

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

	public static void write(String family_name, File family_dir, String comment) {
		if (FileUtil.validPath(family_dir)) {
			File logFileName = new File(family_dir, family_name + Constant.LOG_SUFFIX);
			List<String> contents = new ArrayList<>();
			contents.add("#Log " + comment + " on " + LogUtil.dateNow());
			LogAction.report(contents);
			contents.add(Logger.WARNING_SECTION);
			LogAlert.report(contents);
			contents.add(NOTES_SECTION);
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
	
	public static List<String> getLog() {
		List<String> contents = new ArrayList<>();
		LogAction.report(contents);
		contents.add(Logger.WARNING_SECTION);
		LogAlert.report(contents);
		contents.add(NOTES_SECTION);
		logNotes(contents);
		return contents;
	}

	public static void importPrior(String family_name, File family_dir) {
		if (notes == null) {
			notes = new ArrayList<>();
		} else {
			notes.clear();
		}
		if (FileUtil.validPath(family_dir)) {
			File log_file = new File(family_dir, family_name + Constant.OLDLOG_SUFFIX);
			if (!FileUtil.validFile(log_file)) {
				log_file = new File(family_dir, family_name + Constant.LOG_SUFFIX);
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
                logger.warn("No log file yet exists for " + log_file);
            }
		} else {
			logger.warn("Family directory doesn't exist yet for: " + family_dir);
		}
	}
	
	public static void updateNotes(String text) {
		String [] lines = text.split("\n");
		notes.clear();
		for (int i = 0; i < lines.length; i++) {
			notes.add(lines[i]);
		}
	}

	public static void logNotes(List<String> contents) {
		if (notes == null) {
			notes = new ArrayList<>();
		}
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
		if (species.contains("LUCA")) {
			// if this is not the root node, but the clade is unknown
			species = (node.getParent() == null) ? "root" : "node";
		}
		String prefix = species != null ? (species + '_') : "";
		return (prefix + node.getDBID());
	}
}

