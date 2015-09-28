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
package org.bbop.phylo.model;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import org.bbop.phylo.gaf.GafRecorder;
import org.bbop.phylo.panther.PantherAdapter;
import org.bbop.phylo.tracking.Logger;
import org.bbop.phylo.util.Constant;

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
    private List<String> tree_content;
    private List<String> attr_content;
    private List<String> msa_content;
    private List<String> wts_content;

	public Family(String family_name) {
		setFamily_name(family_name);
	}

	public boolean fetch(Tree seed, PantherAdapter adapter) {
		/*
		 * Assumption for touchup is that all of the PANTHER families are present in files locally
		 * The update of the families is handled asynchronously and should hopefully ensure
		 * that the more up-to-date versions are available
		 */
		this.adapter = adapter;
		boolean got_tree = adapter.fetchTree(this, seed);
		if (got_tree) {
			this.tree = seed;
		} else {
			setFamily_name(null);
		}
		// Force garbage collection after a new book is opened
		System.gc();
		return (got_tree);
	}

	public List<String> getTreeContent() {
		return tree_content;
	}

	public void setTreeContent(List<String> tree_content) {
		this.tree_content = tree_content;
	}

	public List<String> getAttrContent() {
		return attr_content;
	}

	public void setAttrContent(List<String> attr_content) {
		this.attr_content = attr_content;
	}

	public List<String> getMsaContent() {
		return msa_content;
	}

	public void setMsaContent(List<String> msa_content) {
		this.msa_content = msa_content;
	}

	public List<String> getWtsContent() {
		return wts_content;
	}

	public void setWtsContent(List<String> wts_content) {
		this.wts_content = wts_content;
	}

	public boolean save(File family_dir, String comment) {
		boolean saved = adapter.saveFamily(this, family_dir);
		GafRecorder.record(this, family_dir, comment);
		Logger.write(family_name, family_dir);
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

