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

import java.io.File;
import java.util.List;

import org.bbop.phylo.config.TouchupConfig;
import org.bbop.phylo.model.Family;
import org.bbop.phylo.model.Tree;
import org.bbop.phylo.util.Constant;
import org.bbop.phylo.util.FileUtil;

import owltools.gaf.Bioentity;
import owltools.gaf.species.TaxonFinder;

public class PantherFileAdapter extends PantherAdapter {
	/**
	 *
	 */

//	private static Logger log = Logger.getLogger(PantherFileAdapter.class);
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
	 * What the PANTHER server provides are files named thusly
	 * attr.tab	cluster.wts	tree.mia	tree.tree
	 * 
	 * What needs to be recorded are files name
	 * PTHRnnnnn.attr PTHRnnnnn.wts PTHRnnnnn.msa PTHRnnnnn.tree
	 * 
	 * @see
	 */
	public boolean fetchTree(Family family, Tree tree) {
		boolean ok;
		System.gc();
		File family_dir = new File(TouchupConfig.inst().treedir, tree.getId());

		ok = FileUtil.validPath(family_dir);
		File treeFileName = new File(family_dir, "tree" + Constant.TREE_SUFFIX);
		File attrFileName = new File(family_dir, "attr" + Constant.TAB_SUFFIX);

		ok &= FileUtil.validFile(treeFileName);
		ok &= FileUtil.validFile(attrFileName);

		if (ok) {
			family.setTreeContent(FileUtil.readFile(treeFileName));
			Bioentity root = parsePantherTree(family.getTreeContent());
			if (root != null) {
				tree.growTree(root);

				// Read the attribute file
				family.setAttrContent(FileUtil.readFile(attrFileName));
				// Load the attr file to obtain the PTN #s
				List<List<String>> rows = ParsingHack.parsePantherAttr(family.getAttrContent());
				decorateNodes(rows, tree);

				fetchMSA(family);
			}

			if (tree.getRoot().getNcbiTaxonId() == null) {
				String taxon = TaxonFinder.getTaxonID("LUCA");
				tree.getRoot().setNcbiTaxonId(taxon);
			}
		}
		return ok;
	}


	private void fetchMSA(Family family) {
		File family_dir = new File(TouchupConfig.inst().treedir, family.getFamily_name());
		FileUtil.validPath(family_dir);
		File msaFileName = new File(family_dir, "tree" + Constant.MIA_SUFFIX);
		if (FileUtil.validFile(msaFileName)) {
			family.setMsaContent(FileUtil.readFile(msaFileName));
		}

		// Check for wts file
		File wtsFileName = new File(family_dir, "cluster" + Constant.WTS_SUFFIX);
		if (FileUtil.validFile(wtsFileName)) {
			family.setWtsContent(FileUtil.readFile(wtsFileName));
		}
	}
}


