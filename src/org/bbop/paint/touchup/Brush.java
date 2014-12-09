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
package org.bbop.paint.touchup;

import org.apache.log4j.Logger;
import org.bbop.paint.gaf.GafPropagator;
import org.bbop.paint.model.Family;
import org.bbop.paint.model.Tree;
import org.bbop.paint.panther.IDmap;
import org.bbop.paint.util.AnnotationUtil;

public class Brush {
	/**
	 * 
	 */
	private static Brush INSTANCE = null;

	private static final long serialVersionUID = 1L;

	private Family family;

	private static final Logger log = Logger.getLogger(Brush.class);

	private Brush() {
		// Exists only to defeat instantiation.
	}

	public static synchronized Brush inst() {
		if (INSTANCE == null) {
			INSTANCE = new Brush();
		}
		return INSTANCE;
	}

	/**
	 * Method declaration
	 * 
	 * @param family_name
	 * 
	 * @see
	 */
	public boolean loadPaint(String family_name) {
		IDmap.inst().clearGeneIDs();
		family = new Family();
		boolean loaded = family.fetch(family_name);
		if (loaded) {
			AnnotationUtil.collectExpAnnotations(family);

			/*
			 * The file may be null, in which case the following two methods
			 * simply return
			 */
			//			EvidenceAdapter.importEvidence(path);

			GafPropagator.importAnnotations(family_name);

		} else {
			family = null;
		}
		return family != null;
	}
	
	public boolean savePaint() {	
		boolean ok = family != null;
		if (ok) {
			// Need to add the supporting experimental annotations to the withs
			ok = family.save();
		}
		return ok;
	}

	public Family getFamily() {
		return family;
	}

	public Tree getTree() {
		return family.getTree();
	}

}
