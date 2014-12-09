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

package org.bbop.paint;

import org.apache.log4j.Logger;
import org.bbop.paint.LogEntry.LOG_ENTRY_TYPE;
import org.bbop.paint.touchup.Preferences;
import org.bbop.paint.util.FileUtil;
import owltools.gaf.Bioentity;
import owltools.gaf.GeneAnnotation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class LogAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected static LogAction singleton;

	private static List<LogEntry> done_log;
	private static List<LogEntry> undone_log;

	private static Logger logger = Logger.getLogger(LogAction.class);

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
	public LogAction() {
		done_log = new ArrayList<LogEntry>();
		undone_log = new ArrayList<LogEntry>();
	}

	public static LogAction inst() {
		if (singleton == null) 
			singleton = new LogAction();
		return (LogAction) singleton;
	}

	public void logAssociation(Bioentity node, GeneAnnotation assoc, List<GeneAnnotation> removed) {
		LogEntry entry = new LogEntry(node, assoc, removed);
		done_log.add(entry);
	}

	//	public void logDisassociation(Bioentity node, String removed) {
	//		LogEntry remove = findEntry(node, removed, LogEntry.Action.ASSOC);
	//		if (remove != null) {
	//			PaintAction.inst().redoDescendentAssociations(remove.getRemovedAssociations());
	//			done_log.remove(remove);
	//			undone_log.add(remove);
	//		}
	//	}
	//
	//	public void logNot(Bioentity node, String go_id, String ev_code) {
	//		LogEntry entry = new LogEntry(node, go_id, LogEntry.Action.NOT);
	//		done_log.add(entry);
	//	}
	//
	public void logUnNot(Bioentity node, String go_id) {
		LogEntry remove = findEntry(node, go_id, LogEntry.LOG_ENTRY_TYPE.NOT);
		if (remove != null) {
			done_log.remove(remove);
			undone_log.add(remove);
		} 
	}

	public void logPruning(Bioentity node, List<GeneAnnotation> purged) {
		LogEntry branch = findEntry(node, null, LogEntry.LOG_ENTRY_TYPE.PRUNE);
		if (branch != null) {
			done_log.remove(branch);
			undone_log.add(branch);			
		} else {
			branch = new LogEntry(node, purged);
			done_log.add(branch);
		}
	}

	public void logGrafting(Bioentity node) {
		LogEntry branch = findEntry(node, null, LogEntry.LOG_ENTRY_TYPE.PRUNE);
		if (branch != null) {
			//			PaintAction.inst().graftBranch(branch.getNode(), branch.getRemovedAssociations(), false);
			done_log.remove(branch);
			undone_log.add(branch);
		}
	}

	public void undo(GeneAnnotation assoc) {
		LogEntry entry = null;
		for (int i = 0; i < done_log.size() && entry  == null; i++) {
			LogEntry check = done_log.get(i);
			if (check.getNode().equals(assoc.getBioentityObject()) &&
					check.getTerm().equals(assoc.getCls()))
				entry = check;
		}
		done_log.remove(entry);
		takeAction(entry, true);
		undone_log.add(entry);
	}

	public void undo() {
		int index = done_log.size() - 1;
		LogEntry entry = done_log.get(index);
		done_log.remove(index);
		takeAction(entry, true);
		undone_log.add(entry);		
	}

	public void redo() {
		int index = undone_log.size() - 1;
		LogEntry entry = undone_log.remove(index);
		takeAction(entry, false);
		done_log.add(entry);
	}

	public static void report(String family_name) {
		String family_dir = Preferences.inst().getTreedir() + family_name + '/';

		boolean ok = FileUtil.validPath(family_dir);
		String logFileName = family_dir + File.separator + family_name + ".log";
		List<String> contents = new ArrayList<String>();
		for (LogEntry entry : done_log) {
			GeneAnnotation assoc = entry.getLoggedAssociation();
			switch (entry.getAction()) {
			case ASSOC: {
				contents.add("Annotated " + entry.getNode().getDBID() + " to " + entry.getLoggedAssociation().getCls());
				break;
			}
			case NOT: {
				contents.add("Negated " + entry.getNode().getDBID() + " for " + entry.getLoggedAssociation().getCls());
				break;
			}
			case PRUNE: {
				contents.add("Pruned " + entry.getNode().getDBID());
				break;
			}
			default:
				logger.info("Not logging " + entry.getNode());
				break;
			}
		}
		try {
			FileUtil.writeFile(logFileName, contents);
		} catch (IOException e) {
			logger.error("Unable to log annotation actions for " + family_name);
			logger.error(e.getMessage());
		}

	}

	private void takeAction(LogEntry entry, boolean undo) {
		PaintAction stroke = PaintAction.inst();
		switch (entry.getAction()) {
		case ASSOC: {
			if (undo) {
				//				stroke.undoAssociation(entry.getNode(), entry.getTerm());
				//				stroke.redoDescendentAssociations(entry.getRemovedAssociations());
			} else {
				//				entry.getRemovedAssociations().clear();
				//				stroke.redoAssociation(entry.getLoggedAssociation(), entry.getRemovedAssociations());
			}
			break;
		}
		case NOT: {
			if (undo) {
				//				stroke.unNot(entry.getEvidence(), entry.getNode(), false);
			} else {
				//				stroke.setNot(entry.getEvidence(), entry.getNode(), entry.getEvidence().getCode(), false);
			}
			break;
		}
		case PRUNE: {
			entry.getNode().setPrune(!entry.getNode().isPruned());
			if (undo) {
				//				stroke.graftBranch(entry.getNode(), entry.getRemovedAssociations(), false);
			} else {
				//				stroke.pruneBranch(entry.getNode(), false);
			}
			break;
		}
		}
	}

	private LogEntry findEntry(Bioentity node, String go_id, LOG_ENTRY_TYPE action) {
		LogEntry found = null;
		for (LogEntry entry : done_log) {
			if (found == null) {
				if (entry.getNode() == node && ((go_id != null && entry.getTerm().equals(go_id) && action == entry.getAction()) || (go_id == null && action == entry.getAction())))
					found = entry;	
			}
		}
		return found;
	}
}
