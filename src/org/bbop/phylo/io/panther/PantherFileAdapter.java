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
package org.bbop.phylo.io.panther;

import java.io.File;

import org.apache.log4j.Logger;
import org.bbop.phylo.model.Family;
import org.bbop.phylo.model.Tree;
import org.bbop.phylo.util.FileUtil;

public class PantherFileAdapter extends PantherAdapter {
	/**
	 *
	 */

	File family_dir;
	File treeFile;
	File attrFile;
	File msaFile;
	File wtsFile;

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
	 * What the PANTHER server provides are files named thusly
	 * attr.tab	cluster.wts	tree.mia	tree.tree
	 * 
	 * What needs to be recorded are files name
	 * PTHRnnnnn.attr PTHRnnnnn.wts PTHRnnnnn.msa PTHRnnnnn.tree
	 * 
	 * @see
	 */
	public boolean fetchFamily(Family family, Tree tree) {
		boolean ok;
		System.gc();

		ok = FileUtil.validPath(family_dir);

		ok &= FileUtil.validFile(treeFile);
		ok &= FileUtil.validFile(attrFile);

		if (ok) {
			family.setTreeContent(FileUtil.readFile(treeFile));
			// Read the attribute file
			family.setAttrContent(FileUtil.readFile(attrFile));
			if (FileUtil.validFile(msaFile)) {
				family.setMsaContent(FileUtil.readFile(msaFile));
			}
			// Check for wts file
			if (FileUtil.validFile(wtsFile)) {
				family.setWtsContent(FileUtil.readFile(wtsFile));
			}
		}
		return ok;
	}

	public void setFamilyDir(String tree_dir, String family_name) {
		family_dir = new File(tree_dir, family_name);	
	}

	public void setFamilyDir(String tree_dir) {
		family_dir = new File(tree_dir);	
	}

	public void setTreeFileName(String file_name) {
		treeFile = new File(family_dir, file_name);
	}

	public void setAttrFileName(String file_name) {
		attrFile = new File(family_dir, file_name);
	}

	public void setMSAFileName(String file_name) {
		msaFile = new File(family_dir, file_name);
	}

	public void setWtsFileName(String file_name) {
		wtsFile = new File(family_dir, file_name);
	}

}


