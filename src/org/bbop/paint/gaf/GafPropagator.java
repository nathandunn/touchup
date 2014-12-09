package org.bbop.paint.gaf;
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


import org.apache.log4j.Logger;
import org.bbop.paint.LogAlert;
import org.bbop.paint.LogEntry;
import org.bbop.paint.PaintAction;
import org.bbop.paint.panther.IDmap;
import org.bbop.paint.touchup.Constant;
import org.bbop.paint.touchup.Preferences;
import org.bbop.paint.util.FileUtil;
import org.bbop.paint.util.OWLutil;
import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.parser.GafObjectsBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

public class GafPropagator {

	protected static Logger log = Logger.getLogger(GafPropagator.class);

	/**
	 * Method declaration
	 *
	 * @param gafdoc gaf_file
	 * @throws IOException
	 *
	 * @see
	 */
	private static void propagate(GafDocument gafdoc) throws IOException {
		List<GeneAnnotation> annotations = gafdoc.getGeneAnnotations();

		HashSet<Bioentity> pruned_list = new HashSet<Bioentity>();
		HashMap<Bioentity, List<GeneAnnotation>> negate_list = new HashMap<Bioentity, List<GeneAnnotation>>();

		IDmap mapper = IDmap.inst();

		for (GeneAnnotation annotation : annotations) {
			Bioentity gaf_node = annotation.getBioentityObject();
			/*
			 * Backwards compatibility
			 */
			if (annotation.getAssignedBy().equals(Constant.OLD_SOURCE))
				annotation.setAssignedBy(Constant.PAINT_AS_SOURCE);
			/*
			 * Next step is to find the corresponding gene node
			 */
			List<Bioentity> seqs = null;
			Bioentity node = mapper.getGeneByDbId(gaf_node.getId());
			if (node == null) {
				seqs = mapper.getGenesBySeqId(gaf_node.getSeqDb() + ':' + gaf_node.getSeqId());
				if (seqs == null || (seqs != null && seqs.size() == 0)) {
					/*
					 * If a node can't be found it is likely not a big deal
					 * The only really important nodes are the ancestors from which everything else is propagated.
					 */
					seqs = mapper.getGenesBySeqId("UniProtKB", gaf_node.getLocalId());
				}
			}
			else {
				seqs = new ArrayList<Bioentity>();
				seqs.add(node);
			}
			if (seqs == null || (seqs != null && seqs.size() == 0)) {
				if (gaf_node.getDb().equals(Constant.PANTHER_DB)) {
					LogAlert.logMissing(node, annotation);
				}
			} else {
				for (Bioentity seq_node : seqs) {
					parseAnnotations(seq_node, annotation, pruned_list, negate_list);
				}
			} // end for loop going through gaf file contents
		}
		for (Bioentity node : pruned_list) {
			node.setPrune(true);
//			PaintAction.inst().pruneBranch(node, true);
		}
		if (!negate_list.isEmpty()) {
			applyNots(negate_list);
		}
	}

	private static void parseAnnotations (Bioentity node,
										  GeneAnnotation annotation,
										  HashSet<Bioentity> pruned_list,
										  HashMap<Bioentity, List<GeneAnnotation>> negate_list) {

		if (annotation.isCut()) {
			pruned_list.add(node);
		}
		/*
		 * Ignore the rows (from older GAFs) that are for descendant nodes 
		 * in the tree. These will be propagated from the ancestral nodes
		 * that were directly annotated
		 */
		else if (!annotation.getShortEvidence().equals(Constant.ANCESTRAL_EVIDENCE_CODE)) {
			boolean negation = annotation.isNegated();
			if (negation) {
				List<GeneAnnotation> not_annots = negate_list.get(node);
				if (not_annots == null) {
					not_annots = new ArrayList<GeneAnnotation>();
					negate_list.put(node, not_annots);
				}
				not_annots.add(annotation);
			}
			else {
				String go_id = annotation.getCls();
				if (OWLutil.inst().isObsolete(go_id)) {
					LogAlert.logObsolete(node, annotation);
				} else {
					if (OWLutil.inst().isAnnotatedToTerm(node.getAnnotations(), go_id) == null) {
						LogEntry.LOG_ENTRY_TYPE invalid = PaintAction.inst().isValidTerm(go_id, node);
						if (invalid == null) {
							PaintAction.inst().propagateAssociation(node, go_id, annotation.getLastUpdateDate());
						} else {
							LogAlert.logInvalid(node, annotation, invalid);
						}
					}
				}
			}
		}
	}

	public static boolean importAnnotations(String family_name) {
		String family_dir = Preferences.inst().getGafdir() + family_name + '/';
		boolean ok = FileUtil.validPath(family_dir);
		if (ok) {
			String prefix = Preferences.panther_files[0].startsWith(".") ? family_name : "";
			String gaf_file = family_dir + File.separator + prefix + ".gaf";
			GafObjectsBuilder builder = new GafObjectsBuilder();
			GafDocument gafdoc;
			try {
				gafdoc = builder.buildDocument(gaf_file);
				propagate(gafdoc);
			} catch (IOException | URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				ok = false;
			}
		}
		return ok;
	}

	private static void applyNots(Map<Bioentity, List<GeneAnnotation>> negate_list) {
		Map<Bioentity, List<GeneAnnotation>> toBeSkipped = new HashMap<Bioentity, List<GeneAnnotation>>();
		for (Bioentity node : negate_list.keySet()) {
			List<GeneAnnotation> row_list = negate_list.get(node);
			List<GeneAnnotation> skipList = new ArrayList<GeneAnnotation>();
			toBeSkipped.put(node, skipList);
			for (GeneAnnotation not_annot : row_list) {
				Collection<String> withs = not_annot.getWithInfos();
				for (Iterator<String> with_it = withs.iterator(); with_it.hasNext();) {
					String with = with_it.next();
					Bioentity with_node = findWithNode(with);
					if (with_node != null && negate_list.containsKey(with_node)) {
						List<GeneAnnotation> check_list = negate_list.get(with_node);
						for (GeneAnnotation check_annot : check_list) {
							if (not_annot.getCls().equals(check_annot.getCls())) {
								skipList.add(check_annot);
							}
						}

					}
					else {
						log.debug("Could not parse dbxref from with column \"" + withs + "\"\n");
					}
				}
			}
		}

		for (Bioentity node : negate_list.keySet()) {
			List<GeneAnnotation> row_list = negate_list.get(node);
			List<GeneAnnotation> skipList = toBeSkipped.get(node);
			for (GeneAnnotation notted_annot : row_list) {
				if (skipList != null && skipList.contains(notted_annot)) {
					continue;
				}
				/* 
				 * Need to propagate this change to all descendants
				 */
				List<GeneAnnotation> associations = node.getAnnotations();
				for (GeneAnnotation assoc : associations) {
					if (assoc.getCls().equals(assoc.getCls())) {
						List<String> all_evidence = assoc.getReferenceIds();
						/*
						 * Should just be one piece of evidence
						 */
						if (all_evidence.size() == 1) {
							PaintAction.inst().setNot(assoc, true);
						}
					}
				}
			}
		}
	}

	private static Bioentity findWithNode(String with) {
		Bioentity with_node = null;
		if (with != null && !with.equals("")) {
			with_node = IDmap.inst().getGeneByPTNId(with);
			if (with_node == null) {
				with_node = IDmap.inst().getGeneByDbId(with);
			}
			if (with_node == null) {
				List<Bioentity> seqs = IDmap.inst().getGenesBySeqId(with);
				if (seqs != null && seqs.size() > 0) {
					with_node = seqs.get(0);
					if (seqs.size() > 1) {
						log.error("Should handle double seqs better for " + with);
					}
				}
			}
		}
		return with_node;
	}
}
