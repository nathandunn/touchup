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
package org.bbop.paint.model;

import org.bbop.paint.gaf.GafRecorder;
import org.bbop.paint.panther.PantherAdapter;
import org.bbop.paint.panther.PantherFileAdapter;
import org.bbop.paint.touchup.Constant;

import java.io.Serializable;

public class Family implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Pretty much the straight content of the files or the database transfer
	 * This is the raw data which is parsed
	 */
	private String family_name;
	private PantherAdapter adapter;
	private Tree tree;

	public Family() {
	}

	public boolean fetch(String family_name) {
		/*
		 * Assumption is that all of the PANTHER families are present in files locally
		 * The update of the families is handled asynchronously and should hopefully ensure
		 * that the more up-to-date versions are available
		 */
		setFamily_name(family_name);
		adapter = new PantherFileAdapter();
		tree = adapter.fetchTree(family_name);
		if (tree == null) {
			setFamily_name(null);
		}
		// Force garbage collection after a new book is opened
		System.gc();
		return (tree != null);
	}

	public boolean save() {
		boolean saved = adapter.saveFamily(this);

		GafRecorder.record(this);

		return saved;
	}

	public Tree getTree() {
		return tree;
	}

	public String getFamily_name() {
		return family_name;
	}

	private void setFamily_name(String family_name) {
		this.family_name = family_name;
	}

	public String getReference() {
		return Constant.PAINT_REF + ':' + family_name.substring("PTHR".length());
	}
}

