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
package org.bbop.phylo.panther;

import org.apache.log4j.Logger;
import org.bbop.phylo.model.MSA;
import org.bbop.phylo.model.Tree;
import org.bbop.phylo.touchup.Constant;
import org.bbop.phylo.touchup.Preferences;
import org.bbop.phylo.util.FileUtil;
import org.bbop.phylo.util.TaxonFinder;
import owltools.gaf.Bioentity;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PantherFileAdapter extends PantherAdapter {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(PantherFileAdapter.class);
	/**
	 * Constructor declaration
	 *
	 * @see
	 */

	public PantherFileAdapter(){
	}

	/**
	 * Method declaration
	 *
	 *
	 * @return
	 *
	 * @see
	 */
	public Tree fetchTree(String family_name) {
		boolean ok;
		Tree tree = null;
		System.gc();
		String family_dir = Preferences.inst().getTreedir() + family_name + File.separator;

		ok = FileUtil.validPath(family_dir);
		String treeFileName = family_dir + "tree" + Constant.TREE_SUFFIX;
		String attrFileName = family_dir + "attr" + Constant.ATTR_SUFFIX;

		ok &= FileUtil.validFile(treeFileName);
		ok &= FileUtil.validFile(attrFileName);

		if (ok) {
			tree_content = FileUtil.readFile(treeFileName);
			Bioentity root = parsePantherTree(tree_content);
			if (root != null) {
				tree = new Tree(family_name, root);

				// Read the attribute file
				attr_content = FileUtil.readFile(attrFileName);
				// Load the attr file to obtain the PTN #s
				List<List<String>> rows = ParsingHack.parsePantherAttr(attr_content);
				decorateNodes(rows, tree);

				fetchMSA(family_name);
			}

			if (tree.getRoot().getNcbiTaxonId() == null) {
				String taxon = TaxonFinder.getTaxonID("LUCA");
				tree.getRoot().setNcbiTaxonId(taxon);
			}
		}
		return tree;
	}


	private MSA fetchMSA(String family_name) {
		String family_dir = Preferences.inst().getTreedir() + family_name + File.separator;
		FileUtil.validPath(family_dir);
		String msaFileName = family_dir + "tree" + Constant.MSA_SUFFIX;
		Map<Bioentity, String> sequences = new HashMap<Bioentity, String>();
		int seq_length = 0;
		if (FileUtil.validFile(msaFileName)) {
			msa_content = FileUtil.readFile(msaFileName);
			seq_length = parseSeqs(msa_content, sequences);
		}

		// Check for wts file
		String wtsFileName = family_dir + "cluster" + Constant.WTS_SUFFIX;
		Map <Bioentity, Double> weights = new HashMap<Bioentity, Double>();
		if (FileUtil.validFile(wtsFileName)) {
			wts_content = FileUtil.readFile(wtsFileName);
			parseWts(wts_content, weights);
		}
		return new MSA(sequences, seq_length, weights);
	}
}


