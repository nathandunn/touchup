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
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.bbop.phylo.model.Family;
import org.bbop.phylo.util.Constant;
import org.bbop.phylo.util.FileUtil;

/**
 * Created by suzi on 12/15/14.
 */
public abstract class PantherAdapter implements PantherAdapterI {

	private static final Logger log = Logger.getLogger(PantherAdapter.class);

	private static final String MSG_ERROR_WRITING_FILE = "Error writing file ";

	public boolean saveFamily(Family family, File family_dir) {
		String family_name = family.getFamily_name();

		if (!family_dir.getName().equals(family_name)) {
			family_dir = new File(family_dir, family_name);
			family_dir.mkdir();
		}
		boolean ok = FileUtil.validPath(family_dir);

		File treeFileName = new File(family_dir, family_name + Constant.TREE_SUFFIX);
		File attrFileName = new File(family_dir, family_name + Constant.ATTR_SUFFIX);

		ok &= writeData(treeFileName, family.getTreeContent());
		ok &= writeData(attrFileName, family.getAttrContent());

		if (family.getMsaContent() != null && ok) {
			File msaFileName = new File(family_dir, family_name + Constant.MSA_SUFFIX);
			ok &= writeData(msaFileName, family.getMsaContent());
		}
		if (family.getWtsContent() != null && ok) {
			File wtsFileName = new File(family_dir, family_name + Constant.WTS_SUFFIX);
			ok &= writeData(wtsFileName, family.getWtsContent());
		}
		return ok;
	}

	private boolean writeData(File file_name, List<String> data) {
		try {
			FileUtil.writeFile(file_name, data);
			return true;
		} catch (IOException e) {
			log.info(MSG_ERROR_WRITING_FILE + file_name);
			return false;
		}
	}
}

