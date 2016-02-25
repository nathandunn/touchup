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
	public static final String PRUNED_SECTION = "# PRUNED";
	public static final String CHALLENGED_SECTION = "# CHALLENGED";
	public static final String WARNING_SECTION = "# WARNINGS - THE FOLLOWING HAVE BEEN REMOVED FOR THE REASONS NOTED";
	private static final String NOTES_SECTION = "# NOTES";
	private static final String REF_SECTION = "# REFERENCE";
	private static final String HISTORY_SECTION = "# HISTORY";
	protected static final String NONE = "- None -";

	private static List<String> notes;
	private static List<String> history;

	public static void write(String family_name, File family_dir, String comment, String date) {
		if (FileUtil.validPath(family_dir)) {
			File logFileName = new File(family_dir, family_name + Constant.LOG_SUFFIX);
			List<String> contents = new ArrayList<>();
			contents.add(HISTORY_SECTION);
			logHistory(contents, comment, date);
			LogAction.inst().report(contents);
			contents.add(Logger.WARNING_SECTION);
			LogAlert.report(contents);
			contents.add(Logger.CHALLENGED_SECTION);
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

	public static void importPrior(String family_name, File family_dir) {
		if (notes == null) {
			notes = new ArrayList<>();
		} else {
			notes.clear();
		}
		if (history == null) {
			history = new ArrayList<>();
		} else {
			history.clear();
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
					captureHistory(log_content);
					clearLogHeaders(log_content);
					for (int i = 0; i < log_content.size(); i++) {
						String line = log_content.get(i).trim();
						if (line.length() > 0) {
							notes.add(line);							
						}
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
			if (lines[i].trim().length() > 0) {
				notes.add(lines[i]);
			}
		}
	}

	public static void setNotes(List<String> text) {
		notes = text;
	}

	public static void logNotes(List<String> contents) {
		if (notes == null) {
			notes = new ArrayList<>();
		}
		if (!notes.isEmpty()) {
			contents.addAll(notes);
		}
		contents.add("");
	}

	private static void logHistory(List<String> contents, String comment, String date) {
		contents.add(date + ": " + comment);
		if (history != null) {
			for (int i = history.size() - 1; i >= 0; i--) {
				contents.add(history.get(i));
			}
		}
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

	private static void captureHistory(List<String> log_content) {
		boolean captured_history = false;
		boolean capturing = false;
		for (int i = 0; i < log_content.size() && !captured_history;) {
			String content_line = log_content.get(i).trim();
			String line = content_line.toLowerCase();
			if (line.startsWith(HISTORY_SECTION.toLowerCase())) {
				capturing = true;
				i++;
			} else if (capturing) {
				if (line.startsWith(NOTES_SECTION.toLowerCase()) ||
						line.startsWith(MF_SECTION.toLowerCase()) ||
						line.startsWith(CC_SECTION.toLowerCase()) ||
						line.startsWith(BP_SECTION.toLowerCase()) ||
						line.startsWith(PRUNED_SECTION.toLowerCase()) ||
						line.startsWith(WARNING_SECTION.toLowerCase()) ||
						line.startsWith(CHALLENGED_SECTION.toLowerCase()) ||
						line.matches("^\\d{8}:.*$") ||
						line.startsWith("##")) {
					captured_history = true;
				} else {
					if (content_line.length() > 0) {
						history.add(content_line);
					}
					log_content.remove(i);
				}
			} else {
				i++;
			}
		}
	}

	private static void clearLogHeaders(List<String> log_content) {
		for (int i = log_content.size() - 1; i >= 0; i--) {
			String line = log_content.get(i).trim().toLowerCase();
			String note_cue = NOTES_SECTION.toLowerCase();
			if (line.contains(note_cue) && !line.startsWith(note_cue)) {
				line = line.substring(line.indexOf(note_cue));
				log_content.set(i, line);
			}
			if (line.startsWith(note_cue) && line.length() > note_cue.length()) {
				line = line.substring(note_cue.length());
				log_content.set(i, line);
			}			
			if (line.startsWith(note_cue) ||
					line.startsWith(HISTORY_SECTION.toLowerCase()) ||
					line.matches("^\\d{8}:.*$") ||
					line.startsWith(NONE.toLowerCase()) ||
					line.startsWith(MF_SECTION.toLowerCase()) ||
					line.startsWith(CC_SECTION.toLowerCase()) ||
					line.startsWith(BP_SECTION.toLowerCase()) ||
					line.startsWith(PRUNED_SECTION.toLowerCase()) ||
					line.startsWith(WARNING_SECTION.toLowerCase()) ||
					line.startsWith(CHALLENGED_SECTION.toLowerCase()) ||
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

