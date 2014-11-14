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
package org.bbop.paint;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bbop.paint.LogEntry.LOG_ENTRY_TYPE;
import org.bbop.paint.touchup.Brush;
import org.bbop.paint.touchup.Constant;
import org.bbop.paint.util.AnnotationUtil;
import org.bbop.paint.util.OWLutil;
import org.bbop.paint.util.TaxonChecker;
import org.semanticweb.HermiT.model.Term;

import owltools.gaf.Bioentity;
import owltools.gaf.GeneAnnotation;


public class PaintAction {

	private static PaintAction stroke;

	private static Logger log = Logger.getLogger(PaintAction.class);

	private PaintAction() {
	}

	public static PaintAction inst() {
		if (stroke == null) {
			stroke = new PaintAction();
		}
		return stroke;
	}

	public LOG_ENTRY_TYPE isValidTerm(String go_id, Bioentity node) {
		/*
		 * Can't drop onto a pruned node
		 */
		if (node.isPruned()) {
			return LogEntry.LOG_ENTRY_TYPE.PRUNE;
		}
		// check to make sure that this term is more specific than any inherited terms
		// and the node is not annotated to this term already
		if (OWLutil.inst().isAnnotatedToTerm(node.getAnnotations(), go_id) != null) {
			return (LogEntry.LOG_ENTRY_TYPE.ALREADY_ASSOCIATED);
		}

		// make sure that the term being annotated is related to terms in the descendants
		WithEvidence withs = new WithEvidence(go_id, node);
		if (withs.lacksEvidence()) {
			return (LogEntry.LOG_ENTRY_TYPE.UNSUPPORTED);
		}
		/*
		 *  check to make sure that this term is more generic than directly annotated descendants
		 * if all of them are more general, then disallow the annotation
		 */
		if (OWLutil.inst().descendantsAllBroader(node, go_id, true)) {
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
	public GeneAnnotation propagateAssociation(Bioentity node, String go_id, String date) {
		WithEvidence withs = new WithEvidence(go_id, node);
		Set<Bioentity> exp_withs = withs.getExpWiths();
		boolean negate = withs.isExperimentalNot();
		Set<Term> quals = withs.getWithQualifiers();

		Set<Bioentity> top_with = new HashSet<Bioentity> ();
		top_with.add(node);
		GeneAnnotation assoc = _propagateAssociation(node, go_id, top_with, exp_withs, negate, date);

		List<GeneAnnotation> removed = new ArrayList<GeneAnnotation>();
		removeMoreGeneralTerms(node, go_id, removed);

		LogAction.inst().logAssociation(node, assoc, removed);

		return assoc;
	}

	private GeneAnnotation _propagateAssociation(Bioentity node, 
			String go_id, 
			Set<Bioentity> top_with, 
			Set<Bioentity> exp_withs, 
			boolean negate,
			String date) { 
		//			Set<Term> quals) {

		GeneAnnotation top_assoc = null;
		/**
		 * Only proceed if this is not one of the original sources of information for this association
		 * and this node is not yet annotated to either this term
		 */
		if (!exp_withs.contains(node) && OWLutil.inst().isAnnotatedToTerm(node.getAnnotations(), go_id) == null) {
			GeneAnnotation assoc;
			if (top_with.contains(node)) {
				assoc = createAnnotation(node, go_id, date, true, negate, exp_withs);
				// not dirty if this is restoring annotations from a saved file
				//			DirtyIndicator.inst().dirtyGenes(date == null);
				top_assoc = assoc;
			} else {
				assoc = createAnnotation(node, go_id, date, false, negate, top_with);				
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
						_propagateAssociation(child, go_id, top_with, exp_withs, negate, date);
					}
				}
			}
		}
		return top_assoc;
	}

	private GeneAnnotation createAnnotation(Bioentity node, String go_id,
			String date, boolean is_MRC, boolean is_directNot, Set<Bioentity> exp_withs) {
		GeneAnnotation assoc = new GeneAnnotation();
		assoc.setBioentity(node.getId());
		assoc.setBioentityObject(node);
		assoc.setCls(go_id);
		assoc.addReferenceId(Constant.PAINT_REF + ':' + Brush.inst().getFamily().getFamilyID());
		assoc.setAspect(OWLutil.inst().getAspect(go_id));
		assoc.setAssignedBy(Constant.PAINT_AS_SOURCE);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date annot_date = null;
		if (date != null) {
			try {			
				annot_date = sdf.parse(date);
			} catch (ParseException e) {
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
		Set<String> with_strings = new HashSet<String>();
		for (Bioentity with : exp_withs) {
			with_strings.add(with.getId());
		}
		assoc.setWithInfos(with_strings);
		assoc.setGeneProductForm(node.getDb() + ':' + node.getSeqId());
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
		List<GeneAnnotation> removal = new ArrayList<GeneAnnotation> ();
		if (current_set != null) {
			for (GeneAnnotation assoc : current_set) {
				if (AnnotationUtil.isPAINTAnnotation(assoc)) {
					String check_term = assoc.getCls();
					if (!go_id.equals(check_term) && OWLutil.inst().moreSpecific(go_id, check_term)) {
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
	
	//	public void pruneBranch(Bioentity node, boolean log_it) {
	//		List<LogAssociation> purged = new ArrayList<LogAssociation>();
	//		if (node.getGeneProduct() != null) {
	//			Object[] initial_assocs = node.getAssociations().toArray();
	//			for (Object o : initial_assocs) {
	//				try {
	//					if (o.getClass() != Class.forName("org.geneontology.db.model.Association")) {
	//						log.debug(("Class is not Association but is " + o.getClass().getName()));
	//					}
	//				} catch (ClassNotFoundException e) {
	//					// TODO Auto-generated catch block
	//					e.printStackTrace();
	//				}
	//				GeneAnnotation assoc = (GeneAnnotation) o;
	//				GeneAnnotation removed = _removeAssociation(node, assoc.getCls());
	//				/*
	//				 * Keep track of any associations that would need to be
	//				 * restored if the user changes their mind
	//				 */
	//				if (log_it && (removed != null && (removed.isMRC() || removed.isDirectNot()))) {
	//					LogAssociation note = new LogAssociation(node, assoc);
	//					purged.add(note);
	//				}
	//			}
	//		}
	//		if (log_it)
	//			ActionLog.inst().logPruning(node, purged);
	//		branchNotify(node);
	//	}
	//
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
	//	private boolean restoreInheritedAssociations(Bioentity node) {
	//		/*
	//		 * First collect all of the ancestral annotations that should be applied to this node.
	//		 */
	//		Set<GeneAnnotation> ancestral_assocs = new HashSet<GeneAnnotation>();
	//		LinkDatabase go_root = PaintManager.inst().getGoRoot().getLinkDatabase();
	//		if (node.getParent() != null) {
	//			collectAncestorTerms(node.getParent(), ancestral_assocs, go_root);
	//		}
	//		GeneProduct gene_product = node.getGeneProduct();
	//		boolean restoration = false;
	//		for (GeneAnnotation ancestral_assoc : ancestral_assocs) {
	//			Term ancestral_term = ancestral_assoc.getTerm();
	//			LinkedObject obo_ancestral_term = (LinkedObject) GO_Util.inst().getObject(go_root, ancestral_term.getAcc());
	//			boolean covered = false;
	//			Set<GeneAnnotation> check_set = gene_product != null ? gene_product.getAssociations() : null;		
	//			if (check_set != null) {
	//				for (Iterator<GeneAnnotation> it = check_set.iterator(); it.hasNext() && !covered;) {
	//					GeneAnnotation assoc = it.next();
	//					// is first term/argument (from ancestral protein) 
	//					// is a broader term for the second term/argument (from descendant protein)
	//					// then there is no need to re-associate the broader term.
	//					LinkedObject obo_check_term = (LinkedObject) GO_Util.inst().getObject(go_root, assoc.getTerm().getAcc());
	//					covered |= (obo_check_term == obo_ancestral_term) || TermUtil.isDescendant(obo_ancestral_term, obo_check_term);
	//				}
	//				if (!covered) {
	//					Bioentity top = GO_Util.inst().getBioentity(ancestral_assoc.getGene_product());
	//					Term term = ancestral_assoc.getTerm();
	//					WithEvidence withs = new WithEvidence(term, top);
	//					Set<Bioentity> exp_withs = withs.getExpWiths();
	//					boolean negate = withs.isExperimentalNot();
	//					Set<Bioentity> top_with = new HashSet<Bioentity> ();
	//					top_with.add(top);
	//					Set<Term> old_quals = ancestral_assoc.getQualifiers();
	//					Set<Term> quals = new HashSet<Term>();
	//					for (Term qual : old_quals) {
	//						String name = qual.getName().toUpperCase();
	//						if (!name.equals(GOConstants.NOT) && !name.equals(GOConstants.CUT)) {
	//							quals.add(qual);
	//						}
	//					}
	//					_propagateAssociation(node, term, top_with, exp_withs, negate, ancestral_assoc.getDate(), quals);
	//					restoration = true;
	//				}
	//			}
	//		}
	//		return restoration;
	//	}
	//
	//	public void redoAssociations(List<LogAssociation> archive, List<LogAssociation> removed) {
	//		removed.clear();
	//		for (LogAssociation note : archive) {
	//			redoAssociation(note, removed);
	//		}
	//	}
	//
	//	public void redoDescendentAssociations(List<LogAssociation> archive) {
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
	//	private void collectAncestorTerms(Bioentity ancestral_node, Set<GeneAnnotation> ancestral_collection, LinkDatabase go_root) {
	//		GeneProduct gene_product = ancestral_node.getGeneProduct();
	//		Set<GeneAnnotation> ancestral_assocs = gene_product != null ? gene_product.getAssociations() : null;		
	//		if (ancestral_assocs != null) {
	//			/*
	//			 * For each term
	//			 * If it is a direct annotation
	//			 * and
	//			 * If there are no current annotations to that term or any of its child terms
	//			 * 
	//			 * Then an association to this term needs to be restored
	//			 */
	//			for (GeneAnnotation ancestral_assoc : ancestral_assocs) {
	//				// Did a curator annotate this ancestor?
	//				if (ancestral_assoc.isMRC()) {
	//					// Is a child term of this already in the list?
	//					// if yes then don't need to add it.
	//					LinkedObject ancestral_term = (LinkedObject) GO_Util.inst().getObject(go_root, ancestral_assoc.getTerm().getAcc());
	//					boolean covered = false;
	//					for (GeneAnnotation check_assoc : ancestral_collection) {
	//						Term check_term = check_assoc.getTerm();
	//						// is first term/argument (from ancestral protein) 
	//						// is a broader term for the second term/argument (from descendant protein)
	//						// then there is no need to re-associate the broader term.
	//						LinkedObject dup_check = (LinkedObject) GO_Util.inst().getObject(go_root, check_term.getAcc());
	//						covered |= TermUtil.isDescendant(ancestral_term, dup_check);
	//					}
	//					if (!covered) {
	//						ancestral_collection.add(ancestral_assoc);
	//					}
	//				}
	//			}
	//		}	
	//		if (ancestral_node.getParent() != null) {
	//			collectAncestorTerms(ancestral_node.getParent(), ancestral_collection, go_root);
	//		}
	//	}
	//
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
		List<GeneAnnotation> revised = new ArrayList<GeneAnnotation>();
		for (GeneAnnotation a : current) {
			if (!(a.getCls().equals(go_id) && AnnotationUtil.isPAINTAnnotation(a))) {
				revised.add(a);
			} else
				removed = a;
		}
		node.setAnnotations(revised);

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
	//	public void setNot(GeneAnnotation assoc, boolean log) {
	//		if (!assoc.isNegated()) {
	//			assoc.setIsNegated(true);
	//			assoc.setDirectNot(true);
	//			if (evi_code.equals(GOConstants.DIVERGENT_EC) || evi_code.equals(GOConstants.KEY_RESIDUES_EC)) {
	//				evidence.addWith(node.getParent().getGeneProduct().getDbxref());
	//			}
	//			else if (evi_code.equals(GOConstants.DESCENDANT_SEQUENCES_EC)) {
	//				org.paint.gui.familytree.TreePanel tree = PaintManager.inst().getTree();
	//				Vector<Bioentity> leafList = new Vector<Bioentity>();
	//				tree.getLeafDescendants(node, leafList);
	//				for (Bioentity leaf : leafList) {
	//					Set<GeneAnnotation> leafAssocs = GO_Util.inst().getAssociations(leaf, AspectSelector.inst().getAspect().toString(), true);
	//					if (leafAssocs != null) {
	//						for (GeneAnnotation leafAssoc : leafAssocs) {
	//							if (leafAssoc.getTerm().equals(assoc.getTerm()) && leafAssoc.isNot()) {
	//								evidence.addWith(leaf.getGeneProduct().getDbxref());
	//							}
	//						}
	//					}
	//				}
	//			}
	//
	//			/* 
	//			 * Need to propagate this change to all descendants
	//			 */
	//			LinkDatabase all_terms = PaintManager.inst().getGoRoot().getLinkDatabase();
	//			propagateNegationDown(node, assoc.getGene_product().getDbxref(), assoc, evi_code, true, all_terms);
	//			if (log)
	//				ActionLog.inst().logNot(node, evidence, evi_code);
	//		}
	//	}
	//
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
	//	private void propagateNegationDown(Bioentity node, DBXref with, GeneAnnotation assoc, String code, boolean is_not, LinkDatabase all_terms) {
	//		List<Bioentity> children = node.getChildren();
	//		if (children == null)
	//			return;
	//		for (Iterator<Bioentity> node_it = children.iterator(); node_it.hasNext();) {
	//			Bioentity child = node_it.next();
	//			Set<GeneAnnotation> assoc_list = child.getGeneProduct().getGeneAnnotations();
	//			for (Iterator<GeneAnnotation> assoc_it = assoc_list.iterator(); assoc_it.hasNext();) {
	//				GeneAnnotation child_assoc = assoc_it.next();
	//				// Should not modify any experimental evidence
	//				if (GO_Util.inst().isExperimental(child_assoc)) {
	//					continue;
	//				}
	//				/*
	//				 * Better to see if the child term is_a (or is part_of) the parent term, rather than an exact match
	//				 */
	//				LinkedObject ancestor_obo = (LinkedObject) GO_Util.inst().getObject(all_terms, assoc.getTerm().getAcc());
	//				LinkedObject child_obo = (LinkedObject) GO_Util.inst().getObject(all_terms, child_assoc.getTerm().getAcc());
	//				if (TermUtil.isAncestor(child_obo, ancestor_obo, all_terms, null)) {
	//					Set<Evidence> child_evidence = child_assoc.getEvidence();
	//					child_assoc.setNot(is_not);
	//					child_assoc.setDirectNot(false);
	//					for (Evidence child_evi : child_evidence) {
	//						// all inherited annotations should have evidence code of "IBA", including
	//						// NOT annotations
	//						//						child_evi.setCode(code);
	//						child_evi.setCode(GOConstants.ANCESTRAL_EVIDENCE_CODE);
	//						child_evi.getWiths().clear();
	//						child_evi.addWith(with);
	//					}
	//					/*
	//					 * Hope this is safe enough to do. Essentially saying that all descendant proteins must have 
	//					 * exactly the same set of qualifiers as the ancestral protein for the GeneAnnotation to this
	//					 * particular term
	//					 */
	//					propagateNegationDown(child, with, assoc, code, is_not, all_terms);
	//				}
	//			}
	//		}
	//	}

}
