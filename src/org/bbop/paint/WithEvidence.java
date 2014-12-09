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

import org.apache.log4j.Logger;
import org.bbop.paint.model.Tree;
import org.bbop.paint.touchup.Brush;
import org.bbop.paint.util.AnnotationUtil;
import org.bbop.paint.util.OWLutil;
import org.semanticweb.HermiT.model.Term;
import owltools.gaf.Bioentity;
import owltools.gaf.GeneAnnotation;

import java.util.*;


public class WithEvidence {

	private static Logger log = Logger.getLogger(WithEvidence.class);

	private Set<Bioentity> exp_withs;
	private Set<Bioentity> notted_withs;
	private Map<Term, Set<Bioentity>> qual2nodes;
	private Set<Term> quals;

	public WithEvidence(String go_id, Bioentity node) {
		initWiths(go_id, node);
	}

	protected Set<Term> getWithQualifiers() {
		quals = new HashSet<Term> ();
		if (qual2nodes != null && !qual2nodes.isEmpty()) {
			//			QualifierDialog qual_dialog = new QualifierDialog(GUIManager.getManager().getFrame(), qual2nodes);
			//			quals = qual_dialog.getQualifiers();
		}
		return quals;
	}

	public Set<Bioentity> getExpWiths() {
		if (exp_withs.size() > 0)
			return exp_withs;
		else
			return notted_withs;
	}

	public boolean isExperimentalNot() {
		return notted_withs.size() > 0 && exp_withs.size() == 0;
	}

	public boolean lacksEvidence() {
		return exp_withs.size() == 0 && notted_withs.size() == 0;
	}

	//	public boolean isContradictory() {
	//		return exp_withs.size() > 0 && notted_withs.size() > 0;
	//	}
	//
	public Map<Term, Set<Bioentity>> getTermQualifiers() {
		return qual2nodes;
	}

	private void initWiths(String go_id, Bioentity node) {
		/*
		 * First gather all of the gene nodes leaves that may have provided this term
		 */
		Tree tree = Brush.inst().getTree();
		List<Bioentity> leaf_list = tree.getLeafDescendants(node);
		exp_withs = new HashSet<Bioentity> ();
		notted_withs = new HashSet<Bioentity> ();
		qual2nodes = new HashMap<Term, Set<Bioentity>>();
		for (Bioentity leaf : leaf_list) {
			// Second argument is true because only the experimental codes can be used for inferencing
			List<GeneAnnotation> exp_annotations = AnnotationUtil.getExpAssociations(leaf);
			if (exp_annotations != null && !exp_annotations.isEmpty()) {
				for (Iterator<GeneAnnotation> it_assoc = exp_annotations.iterator(); it_assoc.hasNext() && !exp_withs.contains(leaf);) {
					GeneAnnotation exp_assoc = it_assoc.next();
					String exp_term = exp_assoc.getCls();
					boolean add = false;
					if (exp_term.equals(go_id)) {
						add = true;
					} 
					else {
						/*
						 * Is the term in question (go_id) a parental/broader term than 
						 * the term associated to the leaf node (exp_term)
						 */
						add = OWLutil.inst().moreSpecific(exp_term, go_id);
					}
					if (add) {
						exp_withs.add(leaf);
						/*
						 * The code below is carrying out an unrelated function
						 * Namely to see if any of the experimental nodes that provide the supporting evidence are qualified
						 * Doing this here, rather than as a separate function to avoid recursing down the tree twice
						 */
						//							if (!name.equals(GOConstants.NOT) && !name.equals(GOConstants.CUT) && qual2nodes != null) {
						//								Set<GeneNode> qualified_nodes = qual2nodes.get(qual);
						//								if (qualified_nodes == null) {
						//									qualified_nodes = new HashSet<GeneNode>();
						//									qual2nodes.put(qual, qualified_nodes);
						//								}
						//								if (!qualified_nodes.contains(leaf)) {
						//									qualified_nodes.add(leaf);
						//								}
						//							} else 
						if (exp_assoc.isNegated()) {
							notted_withs.add(leaf);
							if (exp_term.equals(go_id)) {
								exp_withs.remove(leaf);
							}
						}
					}
				}
			}
		}
	}
}

