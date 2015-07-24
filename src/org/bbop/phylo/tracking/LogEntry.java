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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import owltools.gaf.Bioentity;
import owltools.gaf.GeneAnnotation;

public class LogEntry {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(LogEntry.class);

	private final LOG_ENTRY_TYPE type;

	private final Bioentity node;
	private final GeneAnnotation assoc;
	private List<GeneAnnotation> removed;
	private String date;

	public enum LOG_ENTRY_TYPE {
		ASSOC,
		NOT,
		PRUNE,
		MISSING,
		OLD_TREE,
		OBSOLETE_TERM,
		ALREADY_ASSOCIATED,
		UNSUPPORTED,
		WRONG_TAXA,
		TOO_SPECIFIC,
		EXCLUDED;

		public String toString() {
			return super.toString().toLowerCase();
		}
	}

	/*
	 * Used for actual annotations that have been created in PAINT
	 */
	public LogEntry(Bioentity node, GeneAnnotation assoc, List<GeneAnnotation> removed) {
		this.node = node;
		this.assoc = assoc;
		this.removed = removed;
		this.date = assoc.getLastUpdateDate();
		this.type = LOG_ENTRY_TYPE.ASSOC;
	}

	/*
	 * Used for NOT, negation of associations at a fixed node in the tree
	 */
	public LogEntry(Bioentity node, GeneAnnotation assoc) {
		this.node = node;
		this.assoc = assoc;
		this.date = assoc.getLastUpdateDate();
		this.removed = null;
		this.type = LOG_ENTRY_TYPE.NOT;
	}

	/*
	 * pruned list is long those with direction annotations to the pruned node
	 * or directly 'not'ted.
	 */
	public LogEntry(Bioentity node, String date, List<GeneAnnotation> pruned) {
		this.node = node;
		this.assoc = null;
		this.removed = pruned;
		this.date = date;
		this.type = LOG_ENTRY_TYPE.PRUNE;
	}

	public LogEntry(Bioentity node, GeneAnnotation assoc, LOG_ENTRY_TYPE action) {
		this.node = node;
		this.assoc = assoc;
		this.type = action;
		this.date = assoc.getLastUpdateDate();
	}

	public String getTerm() {
		if (assoc != null && type != LOG_ENTRY_TYPE.PRUNE) {
			return assoc.getCls();
		} else {
			return null;
		}
	}

	public Bioentity getNode() {
		return node;
	}

	public String getDate() {
		return this.date;
	}

	public GeneAnnotation getLoggedAssociation() {
		if (type != LOG_ENTRY_TYPE.PRUNE) {
			return assoc;
		} else {
			return null;
		}
	}

	public List<GeneAnnotation> getRemovedAssociations() {
		return removed;
	}

	public void setRemovedAssociations(List<GeneAnnotation> removed) {
		this.removed = removed;
	}

	public LOG_ENTRY_TYPE getAction() {
		return type;
	}

	protected String getEvidence() {
		if (assoc != null)
			return assoc.getShortEvidence();
		else
			return null;
	}

	String getNotes() {
		String note = "";
		switch (getAction()) {
		case ALREADY_ASSOCIATED: {
			note = "already annotated to this term";
			break;
		}
		case UNSUPPORTED: {
			note = "lacks supporting evidence";
			break;
		}
		case TOO_SPECIFIC: {
			note = "all descendant's annotations are to more general terms";
			break;
		}
		case WRONG_TAXA: {
			note = "doesn't pass taxon checks";
			break;
		}
		case EXCLUDED: {
			note = "is an excluded term and not to be used for annotation";
			break;
		}
		default: {
			log.info("Unknown logging type");
			break;
		}
		}
		return note;
	}

	private String dateNow() {
		long timestamp = System.currentTimeMillis();
		/* Date appears to be fixed?? */
		Date when = new Date(timestamp);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		sdf.setTimeZone(TimeZone.getDefault()); // local time
		return sdf.format(when);
	}
}
