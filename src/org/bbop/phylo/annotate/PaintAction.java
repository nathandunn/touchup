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
package org.bbop.phylo.annotate;


import org.apache.log4j.Logger;
import org.bbop.phylo.model.Family;
import org.bbop.phylo.model.Tree;
import org.bbop.phylo.touchup.Constant;
import org.bbop.phylo.tracking.LogAction;
import org.bbop.phylo.tracking.LogEntry;
import org.bbop.phylo.tracking.LogEntry.LOG_ENTRY_TYPE;
import org.bbop.phylo.util.OWLutil;
import org.bbop.phylo.util.TaxonChecker;
import owltools.gaf.Bioentity;
import owltools.gaf.GeneAnnotation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;


public class PaintAction {

    private static PaintAction INSTANCE;

    private static Logger log = Logger.getLogger(PaintAction.class);

    private PaintAction() {
    }

    public static PaintAction inst() {
        if (INSTANCE == null) {
            INSTANCE = new PaintAction();
        }
        return INSTANCE;
    }

    public LOG_ENTRY_TYPE isValidTerm(String go_id, Bioentity node, Tree tree) {
		/*
		 * Can't drop onto a pruned node
		 */
        if (node.isPruned()) {
            return LogEntry.LOG_ENTRY_TYPE.PRUNE;
        }

        if (OWLutil.isExcluded(go_id)) {
			/*
			Can't use terms that are merely for the logic of the ontology
			and not relevant or informative for the biology
			 */
            return (LOG_ENTRY_TYPE.EXCLUDED);
        }
        // check to make sure that this term is more specific than any inherited terms
        // and the node is not annotated to this term already
        if (OWLutil.isAnnotatedToTerm(node.getAnnotations(), go_id) != null) {
            return (LogEntry.LOG_ENTRY_TYPE.ALREADY_ASSOCIATED);
        }

        // make sure that the term being annotated is related to terms in the descendants
        WithEvidence withs = new WithEvidence(tree, node, go_id);
        if (withs.lacksEvidence()) {
            return (LogEntry.LOG_ENTRY_TYPE.UNSUPPORTED);
        }
		/*
		 *  check to make sure that this term is more generic than directly annotated descendants
		 * if all of them are more general, then disallow the annotation
		 */
        if (OWLutil.descendantsAllBroader(node, go_id, true)) {
            return (LogEntry.LOG_ENTRY_TYPE.TOO_SPECIFIC);
        }
        // applicable term for this taxon?
        if (!TaxonChecker.checkTaxons(node, go_id)) {
            return (LogEntry.LOG_ENTRY_TYPE.WRONG_TAXA);
        }
        return null;
    }

    /*
     * Called after a drop of a term onto a node in the tree or when loading a GAF file
     */
    public GeneAnnotation propagateAssociation(Family family, Bioentity node, String go_id, String date, int qualifiers) {
        WithEvidence withs = new WithEvidence(family.getTree(), node, go_id);
        List<String> exp_withs = withs.getExpWiths();
        boolean negate = withs.isExperimentalNot();

        int go_qualifiers = getGOqualifiers(qualifiers);
        List<String> top_with = new ArrayList<> ();
        top_with.add(node.getId());

        GeneAnnotation assoc = _propagateAssociation(node,
                go_id,
                go_qualifiers,
                family.getReference(),
                date,
                negate,
                top_with,
                exp_withs);

        List<GeneAnnotation> bucket_list = new ArrayList<>();
        removeMoreGeneralTerms(node, go_id, bucket_list);

        LogAction.logAssociation(node, assoc, bucket_list);

        return assoc;
    }

    private GeneAnnotation _propagateAssociation(Bioentity node,
                                                 String go_id,
                                                 int qualifiers,
                                                 String reference,
                                                 String date,
                                                 boolean negate,
                                                 List<String> top_with,
                                                 List<String> exp_withs) {
        GeneAnnotation top_assoc = null;
        /**
         * Only proceed if this is not one of the original sources of information for this association
         * and this node is not yet annotated to either this term
         */

        if (!exp_withs.contains(node.getId()) && OWLutil.isAnnotatedToTerm(node.getAnnotations(), go_id) == null) {
            GeneAnnotation assoc;
            if (top_with.contains(node.getId())) {
                assoc = createAnnotation(node, go_id, qualifiers, reference, date, true, negate, exp_withs);
                // not dirty if this is restoring annotations from a saved file
                //			DirtyIndicator.inst().dirtyGenes(date == null);
                top_assoc = assoc;
            } else {
                assoc = createAnnotation(node, go_id, qualifiers, reference, date, false, negate, top_with);
            }

			/*
			 * Doing this afterwards to avoid examining it in the above operation
			 */
            node.addAnnotation(assoc);

			/*
			 * propagate negation...
			 */
            assoc.setIsNegated(negate);

			/*
			 * Make the top ancestral gene in this branch of the gene family the source of information
			 */
            List<Bioentity> children = node.getChildren();
            if (children != null) {
                for (Bioentity child : children) {
                    if (!child.isPruned()) {
                        _propagateAssociation(child,
                                go_id,
                                qualifiers,
                                reference,
                                date,
                                negate,
                                top_with,
                                exp_withs);
                    }
                }
            }
        }
        return top_assoc;
    }

    private GeneAnnotation createAnnotation(Bioentity node,
                                            String go_id,
                                            int qualifiers,
                                            String reference,
                                            String date,
                                            boolean is_MRC,
                                            boolean is_directNot,
                                            List<String> withs) {
        GeneAnnotation assoc = new GeneAnnotation();
        assoc.setBioentity(node.getId());
        assoc.setBioentityObject(node);
        assoc.setCls(go_id);
        assoc.setQualifiers(qualifiers);
        assoc.addReferenceId(reference);
        assoc.setAspect(OWLutil.getAspect(go_id));
        assoc.setAssignedBy(Constant.PAINT_AS_SOURCE);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Date annot_date = null;
        if (date != null) {
            try {
                annot_date = sdf.parse(date);
            } catch (ParseException e) {
                log.warn("Unable to parse date for " + node);
            }
        }
        if (annot_date == null) {
            long timestamp = System.currentTimeMillis();
			/* Date appears to be fixed?? */
            annot_date = new Date(timestamp);
        }
        assoc.setLastUpdateDate(annot_date);
        assoc.setDirectMRC(is_MRC);
        assoc.setDirectNot(is_directNot);
        String code = is_MRC ? Constant.DESCENDANT_EVIDENCE_CODE : Constant.ANCESTRAL_EVIDENCE_CODE;
        assoc.setEvidence(code, null);
        assoc.setWithInfos(withs);
       return assoc;
    }

    private void removeMoreGeneralTerms(Bioentity node, String go_id, List<GeneAnnotation> removed) {
        removeMoreGeneralTermsFromNode(node, go_id, removed);
        List<Bioentity> children = node.getChildren();
        if (children != null) {
            for (Bioentity child : children) {
                removeMoreGeneralTerms(child, go_id, removed);
            }
        }
    }

    private void removeMoreGeneralTermsFromNode(Bioentity node, String go_id, List<GeneAnnotation> removed) {
		/*
		 * remove any redundant annotations that have previously been done by PAINT curators
		 * that are less specific than the new association that is being added.
		 * That is: a descendant protein was annotated earlier to a more general term
		 * and now the curator is adding a more specific term to a more ancestral branch of the family
		 */
        List<GeneAnnotation> current_set = node.getAnnotations();
        List<GeneAnnotation> removal = new ArrayList<> ();
        if (current_set != null) {
            for (GeneAnnotation assoc : current_set) {
                if (AnnotationUtil.isPAINTAnnotation(assoc)) {
                    String check_term = assoc.getCls();
                    if (!go_id.equals(check_term) && OWLutil.moreSpecific(go_id, check_term)) {
                        removal.add(assoc);
                        if (removed != null)
                            removed.add(assoc);
                    }
                }
            }
            for (GeneAnnotation remove : removal) {
                _removeAssociation(node, remove.getCls());
            }
        }
    }

    public void pruneBranch(Bioentity node, String date, boolean log_it) {
        List<GeneAnnotation> purged = new ArrayList<>();
        List<GeneAnnotation> initial_annotations = node.getAnnotations();
        if (initial_annotations != null) {
            for (int i = initial_annotations.size() - 1; i >= 0; i--) {
                GeneAnnotation annotation = initial_annotations.get(i);
                GeneAnnotation removed = _removeAssociation(node, annotation.getCls());
					/*
					 * Keep track of any associations that would need to be
					 * restored if the user changes their mind
					 */
                if (log_it && (removed != null && (removed.isMRC() || removed.isDirectNot()))) {
                    purged.add(removed);
                }
            }
        }

        if (log_it)
            LogAction.logPruning(node, date, purged);
//			branchNotify(node);
    }

    //	public void graftBranch(Bioentity node, List<LogAssociation> archive, boolean log) {
    //		restoreInheritedAssociations(node);
    //		for (LogAssociation note : archive) {
    //			GeneAnnotation replacement = redoAssociation(note, null);
    //			if (note.isDirectNot()) {
    //				/* Now what? */
    //				setNot(replacement.getEvidence().iterator().next(), node, note.getEvidenceCode(), log);
    //			}
    //		}
    //		branchNotify(node);
    //	}
    //
    //	private void branchNotify(Bioentity node) {
    //		TreePanel tree = PaintManager.inst().getTree();
    //		tree.handlePruning(node);
    //		EventManager.inst().fireAnnotationChangeEvent(new AnnotationChangeEvent(node));
    //	}
    //
    private boolean restoreInheritedAssociations(Family family, Bioentity node) {
			/*
			 * First collect all of the ancestral annotations that should be applied to this node.
			 */
        List<GeneAnnotation> ancestral_assocs = new ArrayList<>();
        if (node.getParent() != null) {
            collectAncestorTerms(node.getParent(), ancestral_assocs);
        }
        boolean restoration = false;
        for (GeneAnnotation ancestral_assoc : ancestral_assocs) {
            String ancestral_term = ancestral_assoc.getCls();
            boolean covered = false;
            List<GeneAnnotation> check_list = node.getAnnotations();
            if (check_list != null) {
                for (int i = 0; i < check_list.size() && !covered; i++) {
                    GeneAnnotation annot = check_list.get(i);
                    // is first term/argument (from ancestral protein)
                    // is a broader term for the second term/argument (from descendant protein)
                    // then there is no need to re-associate the broader term.
                    covered |= annot.getCls().equals(ancestral_term);
                    covered |= OWLutil.moreSpecific(ancestral_term, annot.getCls());
                }
                if (!covered) {
                    Bioentity top = ancestral_assoc.getBioentityObject();
                    if (top.getPaintId() == null) {
                        log.error("Got wrong bioentity " + top);
                    }
                    String term = ancestral_assoc.getCls();
                    WithEvidence withs = new WithEvidence(family.getTree(), top, term);
                    List<String> exp_withs = withs.getExpWiths();
                    boolean negate = withs.isExperimentalNot();
                    List<String> top_with = new ArrayList<> ();
                    top_with.add(top.getId());

                    int qualifiers = getGOqualifiers(ancestral_assoc.getQualifiers());
                    _propagateAssociation(node,
                            term,
                            qualifiers,
                            family.getReference(),
                            ancestral_assoc.getLastUpdateDate(),
                            negate,
                            top_with,
                            exp_withs);
                    restoration = true;
                }
            }
        }
        return restoration;
    }

    int getGOqualifiers(int current) {
        if (current > 0) {
            int qualifiers = GeneAnnotation.COLOCATES_WITH_MASK | GeneAnnotation.CONTRIBUTES_TO_MASK | GeneAnnotation.INTEGRAL_TO_MASK;
            qualifiers &= current;
            return qualifiers;
        } else {
            return current;
        }
    }

    //	public void redoAssociations(List<LogAssociation> archive, List<LogAssociation> removed) {
    //		removed.clear();
    //		for (LogAssociation note : archive) {
    //			redoAssociation(note, removed);
    //		}
    //	}
    //
    //	public void redoDescendantAssociations(List<LogAssociation> archive) {
    //		for (LogAssociation note : archive) {
    //			redoAssociation(note, null);
    //		}
    //	}
    //
    //	public GeneAnnotation redoAssociation(LogAssociation note, List<LogAssociation> removed) {
    //		Bioentity node = note.getNode();
    //		Term term = note.getTerm();
    //		Set<Term> quals = note.getQuals();
    //		Integer date = note.getDate();
    //
    //		/**
    //		 * Only proceed if this is not one of the original sources of information for this association
    //		 * and this node is not yet annotated to either this term
    //		 */
    //		WithEvidence withs = new WithEvidence(term, node);
    //		boolean negate = withs.isExperimentalNot();
    //		Set<Bioentity> exp_withs = withs.getExpWiths();
    //
    //		Set<Bioentity> top_with = new HashSet<Bioentity> ();
    //		top_with.add(node);
    //		GeneAnnotation assoc = _propagateAssociation(node, term, top_with, exp_withs, negate, date, quals);
    //
    //		removeMoreGeneralTerms(node, term, removed);
    //
    //		return assoc;
    //	}
    //
    private void collectAncestorTerms(Bioentity ancestral_node, List<GeneAnnotation> ancestral_collection) {

        List<GeneAnnotation> ancestral_assocs = ancestral_node.getAnnotations();
        if (ancestral_assocs != null) {
				/*
				 * For each term
				 * If it is a direct annotation
				 * and
				 * If there are no current annotations to that term or any of its child terms
				 *
				 * Then an association to this term needs to be restored
				 */
            for (GeneAnnotation ancestral_assoc : ancestral_assocs) {
                // Did a curator annotate this ancestor?
                if (ancestral_assoc.isMRC()) {
                    // Is a child term of this already in the list?
                    // if yes then don't need to add it.
                    String ancestral_term = ancestral_assoc.getCls();
                    boolean covered = false;
                    for (GeneAnnotation check_assoc : ancestral_collection) {
                        String check_term = check_assoc.getCls();
                        // is first term/argument (from ancestral protein)
                        // is a broader term for the second term/argument (from descendant protein)
                        // then there is no need to re-associate the broader term.
                        covered |= OWLutil.moreSpecific(ancestral_term, check_term);
                    }
                    if (!covered) {
                        ancestral_collection.add(ancestral_assoc);
                    }
                }
            }
        }
        if (ancestral_node.getParent() != null) {
            collectAncestorTerms(ancestral_node.getParent(), ancestral_collection);
        }
    }

    //	/**
    //	 * This is called when the remove term button is clicked
    //	 */
    //	public void removeAssociation(Bioentity node, String go_id) {
    //		GeneAnnotation removed = _removeAssociation(node, go_id);
    //		restoreInheritedAssociations(node);
    //		ActionLog.inst().logDisassociation(node, removed);
    //	}
    //

    private synchronized GeneAnnotation _removeAssociation(Bioentity node, String go_id) {
        GeneAnnotation removed = null;
        List<GeneAnnotation> current = node.getAnnotations();
        for (int i = 0; i < current.size() && removed == null; i++) {
            GeneAnnotation a = current.get(i);
            if ((a.getCls().equals(go_id) && AnnotationUtil.isPAINTAnnotation(a))) {
                removed = a;
            }
        }
        current.remove(removed);

        List<Bioentity> children = node.getChildren();
        if (children != null) {
            for (Bioentity child : children) {
                _removeAssociation(child, go_id);
            }
        }
        return removed;
    }

    //
    //	public synchronized void undoAssociation(Bioentity node, Term term) {
    //		_removeAssociation(node, term);
    //		restoreInheritedAssociations(node);
    //	}
    //
    //	public synchronized void undoAssociation(List<LogAssociation> remove_list) {
    //		for (LogAssociation entry : remove_list) {
    //			_removeAssociation(entry.getNode(), entry.getTerm());
    //		}
    //	}
    //
    //	public boolean isPainted(Bioentity node, boolean recurse) {
    //		boolean annotated = false;
    //		GeneProduct gene_product = node.getGeneProduct();
    //		if (gene_product != null) {
    //			Set<GeneAnnotation> associations = gene_product.getAssociations();
    //			for (Iterator<GeneAnnotation> assoc_it = associations.iterator(); assoc_it.hasNext()  && !annotated; ) {
    //				GeneAnnotation assoc = assoc_it.next();
    //				annotated = GO_Util.inst().isPAINTAnnotation(assoc);
    //			}
    //		}
    //		if (recurse && !annotated) {
    //			List<Bioentity> children = node.getChildren();
    //			if (children != null) {
    //				for (Iterator<Bioentity> node_it = children.iterator(); node_it.hasNext() && !annotated; ) {
    //					Bioentity child = node_it.next();
    //					annotated = isPainted(child, recurse);
    //				}
    //			}
    //		}
    //		return annotated;
    //	}
    //

    public void setNot(Family family, Bioentity node, GeneAnnotation assoc, String evi_code, boolean log_op) {
        if (!assoc.isNegated()) {
            assoc.setIsNegated(true);
            assoc.setDirectNot(true);
            assoc.setEvidence(evi_code, null);

            List<String> with_str = new ArrayList<>();
			/*
			If this NOT has been actively added by the user indicate the direct parent
			*/
            //	with_str.add(node.getParent().getId());

			/*
			Keep both the existing evidence, but now also add the negative evidence
			 */
            List<Bioentity> leafList = family.getTree().getLeafDescendants(node);
            for (Bioentity leaf : leafList) {
                List<GeneAnnotation> leafAssocs = AnnotationUtil.getAspectExpAssociations(leaf, assoc.getAspect());
                for (GeneAnnotation leafAssoc : leafAssocs) {
                    if (leafAssoc.getCls().equals(assoc.getCls()) && leafAssoc.isNegated()) {
                        with_str.add(leaf.getId());
                    }
                }

            }
            if (with_str.isEmpty()) {
                with_str.add(node.getParent().getId());
            }
            assoc.setWithInfos(with_str);

			/*
			 * Need to propagate this change to all descendants
			 */
            propagateNegationDown(node, assoc, true);

            restoreInheritedAssociations(family, node);

            if (log_op)
                LogAction.logNot(assoc);
        }
    }

    //	public void unNot (Evidence evidence, Bioentity node, boolean log) {
    //		GeneAnnotation assoc = evidence.getAssociation();
    //		assoc.setNot(false);
    //		assoc.setDirectNot(false);
    //		evidence.setCode(GOConstants.ANCESTRAL_EVIDENCE_CODE);
    //		evidence.getWiths().clear();
    //		GeneAnnotation a = getAncestralNodeWithPositiveEvidenceForTerm(node.getParent(), assoc.getTerm());
    //		if (a != null)
    //			evidence.addWith(a.getGene_product().getDbxref());
    //		LinkDatabase all_terms = PaintManager.inst().getGoRoot().getLinkDatabase();
    //		propagateNegationDown(node, a.getGene_product().getDbxref(), assoc, evidence.getCode(), false, all_terms);
    //		if (log)
    //			ActionLog.inst().logUnNot(node, evidence);
    //		DirtyIndicator.inst().dirtyGenes(true);
    //	}
    //
    //	private GeneAnnotation getAncestralNodeWithPositiveEvidenceForTerm(Bioentity node, Term term) {
    //		Set<GeneAnnotation> node_assocs = GO_Util.inst().getAssociations(node, term.getCv(), false);
    //		if (node_assocs != null) {
    //			for (GeneAnnotation a : node_assocs) {
    //				if (a.getTerm().equals(term) && !a.isNot() && a.isMRC()) {
    //					return a;
    //				}
    //			}
    //		}
    //		if (node.getParent() == null) {
    //			return null;
    //		} else {
    //			return getAncestralNodeWithPositiveEvidenceForTerm(node.getParent(), term);
    //		}
    //	}
    //
    //	public GeneAnnotation getAncestralNegation(GeneAnnotation assoc, Term term) {
    //		Bioentity node = GO_Util.inst().getBioentity(assoc.getGene_product());
    //		return getAncestralNegation(node, term);
    //	}
    //
    //	private GeneAnnotation getAncestralNegation(Bioentity node, Term term) {
    //		Set<GeneAnnotation> node_assocs = GO_Util.inst().getAssociations(node, term.getCv(), false);
    //		if (node_assocs != null) {
    //			for (GeneAnnotation a : node_assocs) {
    //				if (a.getTerm().equals(term) && a.isNot() && a.isDirectNot()) {
    //					return a;
    //				}
    //			}
    //		}
    //		if (node.getParent() == null) {
    //			return null;
    //		} else {
    //			return getAncestralNegation(node.getParent(), term);
    //		}
    //	}
    //
    private void propagateNegationDown(Bioentity node, GeneAnnotation assoc, boolean is_not) {
        List<Bioentity> children = node.getChildren();
        if (children == null)
            return;
        for (Bioentity child : children) {
					/*
					 * Better to see if the child term is_a (or is part_of) the parent term, rather than an exact match
					 */
            GeneAnnotation child_assoc = OWLutil.isAnnotatedToTerm(child.getAnnotations(), assoc.getCls());
            if (child_assoc != null) {
                child_assoc.setIsNegated(is_not);
                child_assoc.setDirectNot(false);
                Collection<String> withs = new ArrayList<>();
                withs.add(assoc.getBioentity());
                child_assoc.setWithInfos(withs);
                child_assoc.setEvidence(Constant.ANCESTRAL_EVIDENCE_CODE, null);
                // all inherited annotations should have evidence code of "IBA", including
                // NOT annotations
            }
						/*
						 * Hope this is safe enough to do. Essentially saying that all descendant proteins must have
						 * exactly the same set of qualifiers as the ancestral protein for the GeneAnnotation to this
						 * particular term
						 */
            propagateNegationDown(child, assoc, is_not);
        }
    }
}

