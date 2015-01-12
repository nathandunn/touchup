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

	private static synchronized void initialize() {
		if (go_graph == null) {
			try {
				ParserWrapper pw = new ParserWrapper();
				String iriString = "http://purl.obolibrary.org/obo/go.owl";
				go_graph = new OWLGraphWrapper(pw.parse(iriString));
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
		OWLClass term = go_graph.getOWLClassByIdentifier(go_id);
		return go_graph.isObsolete(term);
	}

	public static String getTermLabel(String go_id) {
		initialize();
		OWLClass term = go_graph.getOWLClassByIdentifier(go_id);
		return go_graph.getLabelOrDisplayId(term);
	}

	public static boolean moreSpecific(String check_term, String against_term) {
		initialize();
		OWLClass check = go_graph.getOWLClassByIdentifier(check_term);
		OWLClass against = go_graph.getOWLClassByIdentifier(against_term);
		Set<OWLObject> broader_terms = go_graph.getAncestors(check, Collections.<OWLPropertyExpression>emptySet());
		return broader_terms.contains(against);
	}

	public static boolean isBroader(String check_term, String against_term) {
		initialize();
		return moreSpecific(against_term, check_term);
	}

	public static boolean descendantsAllBroader(Bioentity node, String go_id, boolean all_broader) {
		initialize();
		List<GeneAnnotation> associations = AnnotationUtil.getExpAssociations(node);
		OWLClass annot_term = go_graph.getOWLClassByIdentifier(go_id);
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
						OWLClass check_term = go_graph.getOWLClassByIdentifier(annotation.getCls());
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

	private static void importGAF() throws IOException {
		initialize();
		ParserWrapper pw = new ParserWrapper();

		String iriString = "http://purl.obolibrary.org/obo/go.owl";
		// TODO catalog.xml ?
		OWLGraphWrapper go_graph;
		try {
			go_graph = new OWLGraphWrapper(pw.parse(iriString));
			/*
			 * Be aware that this version of the ontology has is_a, part_of, occurs_in, and more?
			 * Need to consider this for propagation
			 */
			OWLClass term = go_graph.getOWLClassByIdentifier("GOID");
			go_graph.isObsolete(term);
			go_graph.getDescendants(term);


			// how to traverse a graph
			// go_graph.getIncomingEdgesClosure

			// get all (declared) relationship types
			go_graph.getSourceOntology().getObjectPropertiesInSignature(true);

			// How to create a reasoner?
			// Step 1: create a factory, that how you choose the implementation
			OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
			// Step 2: Create the reasoner from the ontology
			OWLReasoner reasoner = reasonerFactory.createReasoner(go_graph.getSourceOntology());
			// Step 3: use it
			reasoner.getSubClasses(term, false).getFlattened();


			// Step 4 (after you are done): clean up
			reasoner.dispose();
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OBOFormatParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
