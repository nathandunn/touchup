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
import org.bbop.phylo.model.Tree;
import org.bbop.phylo.util.OWLutil;
import owltools.gaf.Bioentity;
import owltools.gaf.GeneAnnotation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class WithEvidence {

	private static Logger log = Logger.getLogger(WithEvidence.class);

	private List<String> exp_withs;
	private List<String> notted_withs;
	private int qualifiers;

	public WithEvidence(Tree tree, Bioentity node, String go_id) {
		initWiths(tree, node, go_id);
	}

	protected int getWithQualifiers() {
		if (qualifiers > 0) {
			//			QualifierDialog qual_dialog = new QualifierDialog(GUIManager.getManager().getFrame(), qual2nodes);
			//			quals = qual_dialog.getQualifiers();
		}
		return qualifiers;
	}

	public List<String> getExpWiths() {
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

	public List <String> getNottedWiths () {
		return notted_withs;
	}

	private void initWiths(Tree tree, Bioentity node, String go_id) {
		/*
		 * First gather all of the gene nodes leaves that may have provided this term
		 */
		List<Bioentity> leaf_list = tree.getLeafDescendants(node);
		exp_withs = new ArrayList<>();
		notted_withs = new ArrayList<> ();

		qualifiers = 0;
		for (Bioentity leaf : leaf_list) {
			// Second argument is true because only the experimental codes can be used for inferencing
			List<GeneAnnotation> exp_annotations = AnnotationUtil.getExpAssociations(leaf);
			if (exp_annotations != null && !exp_annotations.isEmpty()) {
				for (Iterator<GeneAnnotation> it_assoc = exp_annotations.iterator(); it_assoc.hasNext() && !exp_withs.contains(leaf.getId());) {
					GeneAnnotation exp_assoc = it_assoc.next();
					String exp_term = exp_assoc.getCls();
						/*
						 * Is the term in question (go_id) a parental/broader term than
						 * the term associated to the leaf node (exp_term)
						 */
					boolean add = (exp_term.equals(go_id)) || OWLutil.moreSpecific(exp_term, go_id);

                    /*
                    * Don't add this node if it is already included.
                     */
					if (add) {
                        for (String with_id : exp_withs) {
                            add &= !with_id.equals(leaf.getId());
                        }
                    }
                    if (add) {
						exp_withs.add(leaf.getId());
						/*
						 * The code below is carrying out an unrelated function
						 * Namely to see if any of the experimental nodes that provide the supporting evidence are qualified
						 * Doing this here, rather than as a separate function to avoid recursing down the tree twice
						 */
						if (exp_assoc.hasQualifiers()) {
							if (exp_assoc.isColocatesWith())
								qualifiers |= GeneAnnotation.COLOCATES_WITH_MASK;
							if (exp_assoc.isContributesTo())
								qualifiers |= GeneAnnotation.CONTRIBUTES_TO_MASK;
							if (exp_assoc.isIntegralTo())
								qualifiers |= GeneAnnotation.INTEGRAL_TO_MASK;
						}
						if (exp_assoc.isNegated()) {
							notted_withs.add(leaf.getId());
							if (exp_term.equals(go_id)) {
								exp_withs.remove(leaf.getId());
							}
						}
					}
				}
			}
		}
	}
}

