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
import org.bbop.paint.touchup.Preferences;
import org.bbop.paint.util.FileUtil;
import owltools.gaf.Bioentity;
import owltools.gaf.GeneAnnotation;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class LogAlert {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static List<LogEntry> invalids;
	private static List<LogEntry> missing;
	private static List<LogEntry> obsoletes;

	private static Logger logger = Logger.getLogger(LogAlert.class);

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

	public static void report(String family_name) {
		String family_dir = Preferences.inst().getTreedir() + family_name + '/';

		boolean ok = FileUtil.validPath(family_dir);
		String logFileName = family_dir + File.separator + family_name + ".rpt";
		List<String> contents = new ArrayList<String>();
		if (invalids != null && missing != null && obsoletes != null) {
			contents.add("Annotation updates for " + dateNow());
			if (invalids != null) {
				for (LogEntry entry : invalids) {
					contents.add("INVALID & REMOVED\n");
					contents.add(entry.getNode().getDBID() + " to " + entry.getLoggedAssociation().getCls() + ":\t" + entry.getNotes());
				}
			}
			if (missing != null) {
				for (LogEntry entry : missing) {
					contents.add("NODES NO LONGER IN " + family_name + " TREE\n");
					contents.add(entry.getNode().getDBID() + " to " + entry.getLoggedAssociation().getCls());
				}
			}
			if (obsoletes != null) {
				for (LogEntry entry : obsoletes) {
					contents.add("OBSOLETE TERM ANNOTATIONS REMOVED\n");
					contents.add(entry.getNode().getDBID() + " to " + entry.getLoggedAssociation().getCls());
				}
			}
			try {
				FileUtil.writeFile(logFileName, contents);
			} catch (IOException e) {
				logger.error("Unable to log updates for " + family_name);
				logger.error(e.getMessage());
			}
		}
	}

	private static String dateNow() {
		long timestamp = System.currentTimeMillis();
		/* Date appears to be fixed?? */
		Date when = new Date(timestamp);
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
		sdf.setTimeZone(TimeZone.getDefault()); // local time
		return sdf.format(when);
	}

}
