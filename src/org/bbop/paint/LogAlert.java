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
import org.bbop.paint.model.History;
import org.bbop.paint.util.OWLutil;
import owltools.gaf.Bioentity;
import owltools.gaf.GeneAnnotation;

import java.util.ArrayList;
import java.util.List;

public class LogAlert {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static List<LogEntry> invalids;
	private static List<LogEntry> missing;
	private static List<LogEntry> obsoletes;

	private static final Logger logger = Logger.getLogger(LogAlert.class);

	public static void logMissing(Bioentity node, GeneAnnotation assoc) {
		if (missing == null) {
			missing = new ArrayList<LogEntry>();
		}
		LogEntry entry = new LogEntry(node, assoc, LogEntry.LOG_ENTRY_TYPE.MISSING);
		missing.add(entry);
	}

	public static void logInvalid(Bioentity node, GeneAnnotation assoc, LogEntry.LOG_ENTRY_TYPE type) {
		if (invalids == null) {
			invalids = new ArrayList<LogEntry>();
		}
		LogEntry entry = new LogEntry(node, assoc, type);
		invalids.add(entry);
	}

	public static void logObsolete(Bioentity node, GeneAnnotation assoc) {
		if (obsoletes == null) {
			obsoletes = new ArrayList<LogEntry>();
		}
		LogEntry entry = new LogEntry(node, assoc, LogEntry.LOG_ENTRY_TYPE.OBSOLETE_TERM);
		obsoletes.add(entry);
	}

	public static void report(List<String> contents) {
		if (invalids != null || missing != null || obsoletes != null) {
			contents.add(History.WARNING_SECTION);
			if (invalids != null) {
				contents.add("## Annotations that have been removed.");
				for (LogEntry entry : invalids) {
					GeneAnnotation annotation = entry.getLoggedAssociation();
					contents.add(annotation.getLastUpdateDate() + ": " +
							History.makeLabel(entry.getNode()) + " to " +
							OWLutil.getTermLabel(annotation.getCls()) +
							" (" + annotation.getCls() + ") " + " - " + entry.getNotes());
				}
			}
			if (missing != null) {
				contents.add("## The ancestral node is no longer a member of this family");
				for (LogEntry entry : missing) {
					GeneAnnotation annotation = entry.getLoggedAssociation();
					if (annotation.isCut())
						contents.add(annotation.getLastUpdateDate() + ": " +
								entry.getNode().getDBID() +
								" was cut, but is now officially pruned from tree");
					else
						contents.add(annotation.getLastUpdateDate() + ": " +
								entry.getNode().getDBID() +
								" can not support previous annotation to " +
								OWLutil.getTermLabel(annotation.getCls()) +
								" (" + annotation.getCls() + ") ");
				}
			}
			if (obsoletes != null) {
				contents.add("## The terms that were annotated to have been made obsolete");
				for (LogEntry entry : obsoletes) {
					GeneAnnotation annotation = entry.getLoggedAssociation();
					contents.add(annotation.getLastUpdateDate() + ": " +
							entry.getNode().getDBID() +
							" removed annotation to obsolete term - " +
							OWLutil.getTermLabel(annotation.getCls()) +
							" (" + annotation.getCls() + ") ");
				}
			}
			contents.add("");
		}
	}


}
