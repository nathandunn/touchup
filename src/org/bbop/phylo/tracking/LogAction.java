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

package org.bbop.phylo.tracking;

import java.util.ArrayList;
import java.util.List;

import org.bbop.phylo.annotate.PaintAction;
import org.bbop.phylo.model.Family;
import org.bbop.phylo.touchup.Constant;
import org.bbop.phylo.tracking.LogEntry.LOG_ENTRY_TYPE;
import org.bbop.phylo.util.OWLutil;

import owltools.gaf.Bioentity;
import owltools.gaf.GeneAnnotation;


public class LogAction {

	private static List<LogEntry> done_log;
	private static List<LogEntry> undone_log;

	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(LogAction.class);

	/*
	 * Separated into sections by aspect ?
	 * Include dates?
	 * Section for References
	 * 	http://en.wikipedia.org/wiki/SKI_protein
	 * 	PMID: 19114989 
	 * Phylogeny
	 * 	Two main clades, SKOR and SKI/SKIL, plus an outlier from Tetrahymena which aligns poorly, so I have not annotated AN0.

	 * Propagate GO:0004647 "phosphoserine phosphatase activity" to AN1 and GO:0016791 "phosphatase activity" to AN0.
	 * -Propagate "cytoplasm" to AN1 based on 3 annotations.
	 * -Propagate "chloroplast" to plants/chlamy.
	 * 
	 * Need to also log all NOTs
	 * 
	 * Challenge mechanism
	 * 	
	 */
	public static void clearLog() {
		if (done_log == null) {
			done_log = new ArrayList<>();
		} else {
			done_log.clear();
		}
		if (undone_log == null) {
			undone_log = new ArrayList<>();
		} else {
			undone_log.clear();
		}
	}

	public static void logAssociation(Bioentity node, GeneAnnotation assoc) {
		LogEntry entry = new LogEntry(node, assoc, LOG_ENTRY_TYPE.ASSOC, null);
		done_log.add(entry);
	}

//	public static void logDisassociation(Bioentity node, String go_id) {
//		//		LogEntry remove = LogAction.findEntry(node, go_id, LogEntry.LOG_ENTRY_TYPE.ASSOC);
//		//		if (remove != null) {
//		//			done_log.remove(remove);
//		//			undone_log.add(remove);
//		//		}
//	}
//
	public static void logNot(GeneAnnotation annotation) {
		LogEntry entry = new LogEntry(annotation.getBioentityObject(), annotation, LOG_ENTRY_TYPE.NOT, null);
		done_log.add(entry);
	}

//	public static void logUnNot(Bioentity node, String go_id) {
//		//		LogEntry remove = findEntry(node, go_id, LogEntry.LOG_ENTRY_TYPE.NOT);
//		//		if (remove != null) {
//		//			done_log.remove(remove);
//		//			undone_log.add(remove);
//		//		}
//	}

	public static void logPruning(Bioentity node, String date, List<GeneAnnotation> purged) {
		LogEntry branch = findEntry(node, null, LogEntry.LOG_ENTRY_TYPE.PRUNE);
		if (branch != null) {
			done_log.remove(branch);
			undone_log.add(branch);
		} else {
			branch = new LogEntry(node, null, LOG_ENTRY_TYPE.PRUNE, purged);
			done_log.add(branch);
		}
	}

	public static void logGrafting(Family family, Bioentity node) {
		LogEntry branch = findEntry(node, null, LogEntry.LOG_ENTRY_TYPE.PRUNE);
		if (branch != null) {
			done_log.remove(branch);
			PaintAction.inst().graftBranch(family, node, branch.getRemovedAssociations(), false);
			undone_log.add(branch);
		}
	}

	public static LogEntry undo(Family family, GeneAnnotation assoc) {
		LogEntry entry = null;
		for (int i = 0; i < done_log.size() && entry  == null; i++) {
			LogEntry check = done_log.get(i);
			if (check.getNode().equals(assoc.getBioentityObject()) &&
					check.getTerm().equals(assoc.getCls()))
				entry = check;
		}
		return undo(family, entry);
	}

	public static LogEntry undo(Family family) {
		int index = done_log.size() - 1;
		LogEntry entry = done_log.get(index);
		return undo(family, entry);
	}

	private static LogEntry undo (Family family, LogEntry entry) {
		done_log.remove(entry);
		takeAction(family, entry, true);
		undone_log.add(entry);
		return entry;
	}

	public static LogEntry redo(Family family) {
		int index = undone_log.size() - 1;
		LogEntry entry = undone_log.remove(index);
		takeAction(family, entry, false);
		done_log.add(entry);
		return entry;
	}

	public static int report(List<String> contents) {
		int pruned = 0;

		if (done_log != null) {
			contents.add(Logger.MF_SECTION);
			reportMF(contents, Constant.MF);
			contents.add(Logger.CC_SECTION);
			reportCC(contents, Constant.CC);
			contents.add(Logger.BP_SECTION);
			reportBP(contents, Constant.BP);
			pruned = reportPruned(contents);
			if (pruned > 0) {
				contents.add(contents.size() - pruned, Logger.PRUNED_SECTION);
			}

		}
		return pruned;
	}

	public static void reportMF(List<String> contents, String aspect) {
		for (LogEntry entry : done_log) {
			reportAspect(entry, contents, aspect, " has function ", " has lost/modified function ");
		}
		contents.add("");
	}

	public static void reportCC(List<String> contents, String aspect) {
		for (LogEntry entry : done_log) {
			reportAspect(entry, contents, aspect, " is found in ", " is not found in ");
		}
		contents.add("");
	}

	public static void reportBP(List<String> contents, String aspect) {
		for (LogEntry entry : done_log) {
			reportAspect(entry, contents, aspect, " participates in ", " does not participate in ");
		}
		contents.add("");
	}

	public static int reportPruned(List<String> contents) {
		int pruned = 0;
		for (LogEntry entry : done_log) {
			if (entry.getAction() == LogEntry.LOG_ENTRY_TYPE.PRUNE) {
				pruned++;
				reportPruned(entry, contents);
			}
		}
		if (pruned > 0)
			contents.add("");
		return pruned;
	}
	
	private static void reportPruned(LogEntry entry, List<String> contents) {
		contents.add(entry.getDate() + ": " +
				Logger.makeLabel(entry.getNode()) +
				" cut from the tree");		
	}

	private static void reportAspect(LogEntry entry,
			List<String> contents,
			String aspect,
			String syntax_candy_4has,
			String syntax_candy_4not) {
		GeneAnnotation annotation = entry.getLoggedAssociation();
		if (annotation != null && annotation.getAspect().equals(aspect)) {
			switch (entry.getAction()) {
			case ASSOC: {
				if (annotation.isContributesTo()) {
					syntax_candy_4has = syntax_candy_4has.replace("has", "contributes to");
				}
				if (annotation.isColocatesWith()) {
					syntax_candy_4has = " co-localizes with ";
				}
				if (annotation.isIntegralTo()) {
					syntax_candy_4has = syntax_candy_4has.replace("has", "is integral to");
				}
				contents.add(annotation.getLastUpdateDate() + ": " +
						Logger.makeLabel(entry.getNode()) +
						syntax_candy_4has +
						OWLutil.inst().getTermLabel(annotation.getCls()) +
						" (" + annotation.getCls() + ") ");
				break;
			}
			case NOT: {
				contents.add(annotation.getLastUpdateDate() + ": " +
						Logger.makeLabel(entry.getNode()) +
						syntax_candy_4not +
						OWLutil.inst().getTermLabel(annotation.getCls()) +
						" (" + annotation.getCls() + ") ");
				break;
			}
			default:
				logger.info("Not logging " + entry.getNode());
				break;
			}
		}
	}

	private static void takeAction(Family family, LogEntry entry, boolean undo) {
		PaintAction dab = PaintAction.inst();
		switch (entry.getAction()) {
		case ASSOC: {
			if (undo) {
				dab.undoAssociation(family, entry.getLoggedAssociation());
			} else {
				dab.propagateAssociation(family, entry.getLoggedAssociation());
			}
			break;
		}
		case NOT: {
			if (undo) {
				dab.unNot(entry.getLoggedAssociation());
			} else {
				dab.setNot(family, entry.getNode(), entry.getLoggedAssociation(), entry.getLoggedAssociation().getShortEvidence(), false);
			}
			break;
		}
		case PRUNE: {
			entry.getNode().setPrune(!entry.getNode().isPruned());
			if (undo) {
				dab.graftBranch(family, entry.getNode(), entry.getRemovedAssociations(), false);
			} else {
				dab.pruneBranch(entry.getNode(), false);
			}
			break;
		}
		default:
			break;
		}
	}

	public static LogEntry findEntry(Bioentity node, String go_id, LogEntry.LOG_ENTRY_TYPE action) {
		LogEntry found = null;
		for (LogEntry entry : done_log) {
			if (found == null) {
				if (entry.getNode() == node && ((go_id != null && entry.getTerm().equals(go_id) && action == entry.getAction())
						|| (go_id == null && action == entry.getAction())))
					found = entry;
			}
		}
		return found;
	}

	public static String doneString() {
		return actionString(done_log);
	}

	public static String undoneString() {
		return actionString(undone_log);
	}

	private static String actionString(List<LogEntry> log) {
		if (log.size() > 0) {
			List<String> contents = new ArrayList<String>();
			int index = log.size() - 1;
			LogEntry entry = log.get(index);
			GeneAnnotation assoc = entry.getLoggedAssociation();
			if (assoc != null) {
				String aspect = assoc.getAspect();
				if (aspect.equals(Constant.MF)) {
					reportAspect(entry, contents, aspect, " has function ", " has lost/modified function ");
				} else if (aspect.equals(Constant.CC)) {
					reportAspect(entry, contents, aspect, " is found in ", " is not found in ");
				} if (aspect.equals(Constant.BP)) {
					reportAspect(entry, contents, aspect, " participates in ", " does not participate in ");
				}
			} else { // Pruning/grafting
				reportPruned(entry, contents);
			}
			return contents.get(0);
		} else
			return "";
	}
}
