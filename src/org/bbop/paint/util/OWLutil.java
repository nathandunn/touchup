package org.bbop.paint.util;
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
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import owltools.gaf.Bioentity;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.parser.DefaultAspectProvider;
import owltools.gaf.parser.GpadGpiObjectsBuilder.AspectProvider;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

import java.io.IOException;
import java.util.*;

public class OWLutil {

	protected static Logger log = Logger.getLogger(OWLutil.class);

	private static OWLGraphWrapper go_graph;
	private static AspectProvider aspect_provider;
	private static Map<String, OWLClass> OWLclasses;

	private static synchronized void initialize() {
		if (go_graph == null) {
			try {
				ParserWrapper pw = new ParserWrapper();
				String iriString = "http://purl.obolibrary.org/obo/go.owl";
				go_graph = new OWLGraphWrapper(pw.parse(iriString));
				OWLclasses = new HashMap<>();
			} catch (OWLOntologyCreationException e) {
				e.printStackTrace();
			} catch (OBOFormatParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
		to avoid overuse of memory reset the term hash
		after a new family is loaded
	 */
	public static void reset() {
		if (OWLclasses != null) {
			OWLclasses.clear();
			System.gc();
		}
	}

	/*
	 * Check each association to the gene product passed as a parameter
	 * If that association is either directly to that term
	 * or the existing association is to a more specific term
	 * then return that association
	 */
	public static GeneAnnotation isAnnotatedToTerm(List<GeneAnnotation> all_annotations, String term_id) {
		initialize();
		GeneAnnotation annotated_with_term = null;
		if (all_annotations != null && term_id != null) {
			for (int i = 0; i < all_annotations.size() && annotated_with_term == null; i++) {
				GeneAnnotation check = all_annotations.get(i);
				String go_id = check.getCls();
				if (term_id.equals(go_id)) {
					annotated_with_term = check;
				}
				else if (moreSpecific(go_id, term_id)) {
					annotated_with_term = check;
				}
			}
		}
		return annotated_with_term;
	}

	public static boolean isObsolete(String go_id) {
		initialize();
		OWLClass term = getTerm(go_id);
		return term == null || (term != null && go_graph.isObsolete(term));
	}

	public static List<String> replacedBy(String go_id) {
		initialize();
		OWLClass term = getTerm(go_id);
		List<String> go_ids;
		if (term != null) {
			go_ids = go_graph.getReplacedBy(term);
			if (go_ids != null) {
				if (go_ids.size() > 1) {
					log.info("More than one replacement term for " + term);
				}
			} else {
				log.info("No replacement for obsolete term " + term);
			}
		} else {
			go_ids = new ArrayList<>();
				/* try looking it up as an alt-id */
			term = go_graph.getOWLClassByIdentifier(go_id, true);
			if (term != null) {
				String term_id = go_graph.getIdentifier(term);
				OWLclasses.put(term_id, term);
				go_ids.add(term_id);
			}

		}
		return go_ids;
	}

	public static boolean isExcluded(String go_id) {
		initialize();
		OWLClass term = getTerm(go_id);
		List<String> subsets = go_graph.getSubsets(term);
		boolean isExcluded = false;
		for(String subset : subsets) {
			if ("gocheck_do_not_manually_annotate".equals(subset) || "gocheck_do_not_annotate".equals(subset)) {
				isExcluded = true;
				break;
			}
		}
		return isExcluded;
	}

	public static String getTermLabel(String go_id) {
		initialize();
		OWLClass term = getTerm(go_id);
		return go_graph.getLabelOrDisplayId(term);
	}

	public static boolean moreSpecific(String check_term, String against_term) {
		initialize();
		OWLClass check = getTerm(check_term);
		OWLClass against = getTerm(against_term);
		Set<OWLObject> broader_terms = go_graph.getAncestors(check, Collections.<OWLPropertyExpression>emptySet());
		return broader_terms.contains(against);
	}

	public static boolean descendantsAllBroader(Bioentity node, String go_id, boolean all_broader) {
		initialize();
		List<GeneAnnotation> associations = AnnotationUtil.getExpAssociations(node);
		OWLClass annot_term = getTerm(go_id);
		Set<OWLObject> broader_terms = go_graph.getAncestors(annot_term);
		if (associations != null) {
			for (GeneAnnotation annotation : associations) {
				// since we've decided to always do positive annotations with NOTs being added afterwards, should make sure that
				// the association is positive
				if (!annotation.isNegated()) {
					if (annotation.isMRC() || node.isLeaf()) {
						/*
						 * First argument is the parent term, second term is the descendant
						 * returns true if 2nd argument is a descendant of the 1st argument 
						 */
						OWLClass check_term = getTerm(annotation.getCls());
						all_broader &= broader_terms.contains(check_term);
					}
				}
			}
		}
		List<Bioentity> children = node.getChildren();
		if (all_broader && children != null) {
			for (Bioentity child : children) {
				all_broader &= descendantsAllBroader(child, go_id, all_broader);
			}
		}
		return all_broader;
	}

	public static String getAspect(String term_id) {
		initialize();
		if (aspect_provider == null) {
			Map<String, String> mappings = new HashMap<String, String>();
			mappings.put("GO:0008150", "P");
			mappings.put("GO:0003674", "F");
			mappings.put("GO:0005575", "C");

			// Step 1: create a factory, that how you choose the implementation
			OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
			// Step 2: Create the reasoner from the ontology
			OWLReasoner reasoner = reasonerFactory.createReasoner(go_graph.getSourceOntology());

			aspect_provider = DefaultAspectProvider.createAspectProvider(go_graph, mappings , reasoner);
		}
		return aspect_provider.getAspect(term_id);
	}

	private static OWLClass getTerm(String go_id) {
		OWLClass term = OWLclasses.get(go_id);
		if (term == null) {
			term = go_graph.getOWLClassByIdentifier(go_id);
			OWLclasses.put(go_id, term);
		}
		return term;
	}

}
