package org.bbop.phylo.gaf;
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
import org.bbop.phylo.annotate.PaintAction;
import org.bbop.phylo.model.Family;
import org.bbop.phylo.panther.IDmap;
import org.bbop.phylo.touchup.Constant;
import org.bbop.phylo.touchup.Preferences;
import org.bbop.phylo.tracking.LogAlert;
import org.bbop.phylo.tracking.LogEntry;
import org.bbop.phylo.util.FileUtil;
import org.bbop.phylo.util.OWLutil;
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
     * @see
     */
    private static void propagate(GafDocument gafdoc, Family family) throws IOException {
        List<GeneAnnotation> gaf_annotations = gafdoc.getGeneAnnotations();

        Map<Bioentity, String> pruned_list = new HashMap<>();
        Map<Bioentity, List<GeneAnnotation>> negate_list = new HashMap<>();

        IDmap mapper = IDmap.inst();

        for (GeneAnnotation gaf_annotation : gaf_annotations) {
			/*
			The GAF file has it's own instantiation of the protein nodes
			Need to be careful not to mix these up
			 */
            Bioentity gaf_node = gaf_annotation.getBioentityObject();
			/*
			 * Backwards compatibility
			 */
            if (gaf_annotation.getAssignedBy().equals(Constant.OLD_SOURCE))
                gaf_annotation.setAssignedBy(Constant.PAINT_AS_SOURCE);
			/*
			* Similarly update the evidence codes
			*/
            String evi_code = gaf_annotation.getShortEvidence();
            if (evi_code.equals(Constant.OLD_ANCESTRAL_EVIDENCE_CODE)) {
                evi_code = Constant.ANCESTRAL_EVIDENCE_CODE;
                gaf_annotation.setEvidence(evi_code, null);
            } else if (evi_code.equals(Constant.OLD_DESCENDANT_EVIDENCE_CODE)) {
                evi_code = Constant.DESCENDANT_EVIDENCE_CODE;
                gaf_annotation.setEvidence(evi_code, null);
            }
			/*
			 * Next step is to find the corresponding gene node
			 */
            List<Bioentity> seqs;
            Bioentity node = mapper.getGeneByDbId(gaf_node.getId());
            if (node == null) {
                seqs = mapper.getGenesBySeqId(gaf_node.getSeqDb() + ':' + gaf_node.getSeqId());
                if (seqs == null || seqs.size() == 0) {
					/*
					 * If a node can't be found it is likely not a big deal
					 * The only really important nodes are the ancestors from which everything else is propagated.
					 */
                    seqs = mapper.getGenesBySeqId("UniProtKB", gaf_node.getLocalId());
                }
            } else {
                seqs = new ArrayList<>();
                seqs.add(node);
            }
            if (seqs == null || seqs.size() == 0) {
                if (gaf_node.getDb().equals(Constant.PANTHER_DB)) {
                    LogAlert.logMissing(gaf_node, gaf_annotation);
                }
            } else {
                for (Bioentity seq_node : seqs) {
                    gaf_annotation.setBioentityObject(seq_node);
                    gaf_annotation.setBioentity(seq_node.getId());
                    parseAnnotations(family, seq_node, gaf_annotation, pruned_list, negate_list);
                }
            } // end for loop going through gaf file contents
        }
        Set<Bioentity> pruned = pruned_list.keySet();
        for (Bioentity node : pruned) {
            node.setPrune(true);
            PaintAction.inst().pruneBranch(node, pruned_list.get(node), true);
        }
        if (!negate_list.isEmpty()) {
            applyNots(family, negate_list);
        }
    }

    private static void parseAnnotations(Family family,
                                         Bioentity node,
                                         GeneAnnotation gaf_annotation,
                                         Map<Bioentity, String> pruned_list,
                                         Map<Bioentity, List<GeneAnnotation>> negate_list) {

        if (gaf_annotation.isCut()) {
            pruned_list.put(node, gaf_annotation.getLastUpdateDate());
        }
		/*
		 * Ignore the rows (from older GAFs) that are for descendant nodes 
		 * in the tree. These will be propagated from the ancestral nodes
		 * that were directly annotated
		 */
        else {
            String evi_code = gaf_annotation.getShortEvidence();

            if (!evi_code.equals(Constant.ANCESTRAL_EVIDENCE_CODE)) {
                boolean negation = gaf_annotation.isNegated();

                if (negation) {
                    List<GeneAnnotation> not_annots = negate_list.get(node);
                    if (not_annots == null) {
                        not_annots = new ArrayList<>();
                        negate_list.put(node, not_annots);
                    }
                    not_annots.add(gaf_annotation);
                } else {
                    List<String> go_ids = new ArrayList<>();
                    if (OWLutil.isObsolete(gaf_annotation.getCls())) {
                        go_ids = OWLutil.replacedBy(gaf_annotation.getCls());
                        if (go_ids.size() == 0) {
                            LogAlert.logObsolete(node, gaf_annotation);
                        }
                    } else {
                        go_ids.add(gaf_annotation.getCls());
                    }
                    for (String go_id : go_ids) {
                        if (OWLutil.isAnnotatedToTerm(node.getAnnotations(), go_id) == null) {
                            LogEntry.LOG_ENTRY_TYPE invalid = PaintAction.inst().isValidTerm(go_id, node, family.getTree());
                            if (invalid == null) {
                                PaintAction.inst().propagateAssociation(family, node, go_id, gaf_annotation.getLastUpdateDate(), gaf_annotation.getQualifiers());
                            } else {
                                LogAlert.logInvalid(node, gaf_annotation, invalid);
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean importAnnotations(Family family) {
        File family_dir = new File(Preferences.inst().getGafDir(), family.getFamily_name());
        boolean ok = FileUtil.validPath(family_dir);
        if (ok) {
            File gaf_file = new File(family_dir, family.getFamily_name() + Constant.GAF_SUFFIX);
            GafObjectsBuilder builder = new GafObjectsBuilder();
            GafDocument gafdoc;
            try {
                gafdoc = builder.buildDocument(gaf_file.getAbsolutePath());
                propagate(gafdoc, family);
            } catch (IOException | URISyntaxException e) {
                ok = false;
            }
        }
        return ok;
    }

    private static void applyNots(Family family, Map<Bioentity, List<GeneAnnotation>> negate_list) {
        /*
        Leaf annotation were also added to the list and these will be redundant if the
        direct NOT is to ancestral node.
        These need to be removed before processing the NOT
         */

        Map<Bioentity, List<GeneAnnotation>> toBeSkipped = new HashMap<>();
        for (Bioentity node : negate_list.keySet()) {
            List<GeneAnnotation> row_list = negate_list.get(node);
            List<GeneAnnotation> skipList = new ArrayList<>();
            toBeSkipped.put(node, skipList);
            for (GeneAnnotation not_annot : row_list) {
                Collection<String> withs = not_annot.getWithInfos();
                for (Iterator<String> with_it = withs.iterator(); with_it.hasNext(); ) {
                    String with = with_it.next();
                    Bioentity with_node = IDmap.inst().getGeneByDbId(with);
                    if (with_node != null && negate_list.containsKey(with_node)) {
                        List<GeneAnnotation> check_list = negate_list.get(with_node);
                        for (GeneAnnotation check_annot : check_list) {
                            if (not_annot.getCls().equals(check_annot.getCls())) {
                                skipList.add(not_annot);
                            }
                        }

                    }
                }
            }
        }


        for (Bioentity node : negate_list.keySet()) {
            List<GeneAnnotation> row_list = negate_list.get(node);
            List<GeneAnnotation> skipList = toBeSkipped.get(node);
            for (GeneAnnotation notted_gaf_annot : row_list) {
                if (skipList != null && skipList.contains(notted_gaf_annot)) {
                    continue;
                }
				/*
				 * Need to propagate this change to all descendants
				 */
                List<GeneAnnotation> associations = node.getAnnotations();
                if (associations != null) {
                    for (GeneAnnotation assoc : associations) {
                        if (assoc.getCls().equals(notted_gaf_annot.getCls())) {
                            List<String> all_evidence = assoc.getReferenceIds();
						/*
						 * Should just be one piece of evidence
						 */
                            if (all_evidence.size() == 1) {
                                PaintAction.inst().setNot(family, node, assoc, notted_gaf_annot.getShortEvidence(), true);
                            }
                        }
                    }
                } else {
                    log.debug("No annotations to negate for " + node.getDBID());
                }
            }
        }
    }
}


