package org.bbop.phylo.annotate;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bbop.golr.java.RetrieveGolrAnnotations;
import org.bbop.golr.java.RetrieveGolrAnnotations.GolrAnnotationDocument;
import org.bbop.phylo.model.Family;
import org.bbop.phylo.model.Tree;
import org.bbop.phylo.panther.IDmap;
import org.bbop.phylo.panther.ParsingHack;
<<<<<<< HEAD
import org.bbop.phylo.util.Constant;
=======
import org.bbop.phylo.touchup.Constant;
>>>>>>> ed41ed6b88e8398f6657ca7d99aad58dd10c7736
import org.bbop.phylo.util.OWLutil;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;

public class AnnotationUtil {

	private static final String high_throughput[] = {
		"PMID:10341420",
		"PMID:10662773",
		"PMID:11027285",
		"PMID:11452010",
		"PMID:11914276",
		"PMID:12077337",
		"PMID:12089449",
		"PMID:12134085",
		"PMID:12150911",
		"PMID:12192589",
		"PMID:12482937",
		"PMID:12524434",
		"PMID:12586695",
		"PMID:14562095",
		"PMID:14576278",
		"PMID:14645503",
		"PMID:14690591",
		"PMID:14690608",
		"PMID:15024427",
		"PMID:15343339",
		"PMID:15575969",
		"PMID:15632165",
		"PMID:15738404",
		"PMID:16121259",
		"PMID:16269340",
		"PMID:16319894",
		"PMID:16407407",
		"PMID:16467407",
		"PMID:16622836",
		"PMID:16702403",
		"PMID:16823372",
		"PMID:16823961",
		"PMID:17176761",
		"PMID:17443350",
		"PMID:1848238",
		"PMID:18627600",
		"PMID:19001347",
		"PMID:19040720",
		"PMID:19053807",
		"PMID:19056867",
		"PMID:19061648",
		"PMID:19111667",
		"PMID:19158363",
		"PMID:20424846",
		"PMID:2153142",
		"PMID:22842922",
		"PMID:23212245",
		"PMID:23222640",
		"PMID:23376485",
		"PMID:23533145",
		"PMID:24390141",
		"PMID:2445736",
		"PMID:3031032",
		"PMID:3065625",
		"PMID:8660468",
		"PMID:9020838",
		"PMID:9182565",
		"PMID:16926386",
		"PMID:18433157",
		"PMID:19482547",
		"PMID:19946888",
		"PMID:21423176",
		"PMID:22020285",
		"PMID:22658674",
		"PMID:22681889",
		"PMID:23580065",
		"PMID:24270810",
		"PMID:25416956",
		"GO_REF:0000052",
		"GO_REF:0000054",
	};

	private static final Logger log = Logger.getLogger("AnnotationUtil.class");

	public static void collectExpAnnotationsBatched(Family family) throws Exception {
		Tree tree = family.getTree();
		List<Bioentity> leaves = tree.getLeaves();
		RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("http://golr.geneontology.org/solr", 3, true) {
//		RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("http://golr.berkeleybop.org", 3, true) {
			@Override
			protected void logRequest(URI uri) {
				super.logRequest(uri);
			}

			@Override
			protected void logRequestError(URI uri, IOException exception) {
				log.error("Encountered " + uri, exception);
			}

			@Override
			protected void defaultRandomWait() {
				log.info("waiting " + System.currentTimeMillis());
				randomWait(5000, 10000);
				log.info("retrying " + System.currentTimeMillis());
			}
		};

		List<String> gene_names = new ArrayList<>();
		Map<String, Bioentity> id2gene = new HashMap<>();
		int increment = 100;
		for (int i = 0; i < leaves.size(); i += increment) {
			gene_names.clear();
			id2gene.clear();
			int limit = Math.min(leaves.size(), i + increment);
			for (int position = i; position < limit; position++) {
				Bioentity leaf = leaves.get(position);
				/*
				 * Another nasty hack to use UniprotKB rather than ENSG
				 * Or, for that matter, 
				 * any case where PANTHER has used the name of DB that doesn't actually submit to GO
				 */
				String key = leaf.getId();
				if (key.contains("HGNC") || leaf.getSeqId().contains("P41145"))
					log.debug("See if uniprot is used");
				if (ParsingHack.useUniProtID(leaf.getDb())) {
					key = leaf.getSeqDb() + ':' + leaf.getSeqId();
				}
				gene_names.add(key);
				id2gene.put(key, leaf);
				/* 
				 * while we're at it, include the sequence ID as a synonym, 
				 * nothing to do with the primary goal, merely a convenient time
				 */
				addSynonym(leaf, leaf.getSeqDb() + ':' + leaf.getSeqId());
			}
			askGolr(retriever, gene_names, id2gene);

			/*
			 * Continue searching for those genes for which a corresponding gene 
			 * in GoLR could not be found
			 */
			for (String gene_name : id2gene.keySet()) {
				Bioentity leaf = id2gene.get(gene_name);
				List<GolrAnnotationDocument> golrDocuments;
				golrDocuments = retriever.getGolrAnnotationsForSynonym(leaf.getDb(), leaf.getDBID());

				if (golrDocuments.size() == 0) {
					String key = leaf.getSeqDb() + ':' + leaf.getSeqId();
					golrDocuments = retriever.getGolrAnnotationsForGene(key);
				}

				if (golrDocuments.size() > 0) {
					GafDocument annots = retriever.convert(golrDocuments);
					Collection<Bioentity> bioentities = annots.getBioentities();
					Bioentity golr_gene = null;
					if (bioentities.size() != 1) {
						for (Iterator<Bioentity> iter = bioentities.iterator(); iter.hasNext() && golr_gene == null;) {
							Bioentity gene = iter.next();
							if (gene.getDb().equals("AspGD") && gene.getLocalId().startsWith("ASP")) {
								golr_gene = gene;
							}
						}
						log.info(bioentities.size() + " annotations returned for " + leaf.getId());
					} else {
						golr_gene = bioentities.iterator().next();
					}
					processGolrAnnotations(leaf, golr_gene, annots.getGeneAnnotations());
				}
			}
		}
	}

	private static void askGolr(RetrieveGolrAnnotations retriever, List<String> gene_names, Map<String, Bioentity> id2gene) {
		try {
			List<GolrAnnotationDocument> golrDocuments = retriever.getGolrAnnotationsForGenes(gene_names);
			if (golrDocuments.size() > 0) {
				GafDocument annots = retriever.convert(golrDocuments);
				Collection<Bioentity> bioentities = annots.getBioentities();
				for (Bioentity golr_gene : bioentities) {
					Bioentity leaf = id2gene.get(golr_gene.getId());
					if (leaf != null) {
						/* HACK ALERT!!
						 * Have to use synonym search to get the multiple copies of this from GoLR
						 * All due to bad ID matching issues
						 * Panther uses a form with without an underscore 
						 * so the correct gene can only be found using a synonym match
						 */
						boolean mishap = leaf.getDb().equals("AspGD");
						if (!mishap) {
							id2gene.remove(golr_gene.getId());
							List<GeneAnnotation> golr_annotations = (List<GeneAnnotation>) annots.getGeneAnnotations(golr_gene.getId());
							processGolrAnnotations(leaf, golr_gene, golr_annotations);
						} else {
							log.debug("Bad bioentity " + leaf.getId() + '?');
						}
					} else {
						log.debug("Whoa! Could not find matching PANTHER node for " + golr_gene);
					}
				}
			}
		} catch (Exception e) {
			String message = "Problem collecting experimental annotations for \n" + e.getMessage();
			log.info(message);
			System.exit(-1);
		}
	}

	private static void processGolrAnnotations(Bioentity leaf, Bioentity golr_gene, List<GeneAnnotation> golr_annotations) {
		List<GeneAnnotation> exp_annotations = paintAnnotationsFilter(golr_annotations);
		leaf.setAnnotations(exp_annotations);
		// lets compare and fill in any missing fields
		if (golr_gene != null) {
			if (leaf.getNcbiTaxonId() == null ||
					leaf.getNcbiTaxonId().length() == 0 ||
					leaf.getNcbiTaxonId().endsWith(":1")) {
				leaf.setNcbiTaxonId(golr_gene.getNcbiTaxonId());
			}
			if (!golr_gene.getId().equals(leaf.getId()) && !golr_gene.getDb().equals("UniProtKB")) {
				// default to the identifier used by the GO database
<<<<<<< HEAD
				String previous = leaf.getId();
=======
				String previous = leaf.getLocalId();
>>>>>>> ed41ed6b88e8398f6657ca7d99aad58dd10c7736
				leaf.setId(golr_gene.getId());
				IDmap.inst().replace(leaf, previous);
				addSynonym(leaf, previous);
			}
			if (leaf.getFullName() == null || leaf.getFullName().length() == 0) {
				leaf.setFullName(golr_gene.getFullName());
			}
			if (golr_gene.getSymbol() != null) {
				leaf.setSymbol(golr_gene.getSymbol());
			} else if (leaf.getSymbol() == null) {
				// don't leave it blank
				leaf.setSymbol(leaf.getDBID());
			}
			if (golr_gene.getSynonyms() != null) {
				for (String synonym : golr_gene.getSynonyms()) {
					addSynonym(leaf, synonym);
				}
			}
		}
	}

	private static void addSynonym(Bioentity leaf, String synonym) {
		boolean add_it = synonym.length() > 0;
		List<String> existing_synonyms = leaf.getSynonyms();
		for (int i = 0; i < existing_synonyms.size() && add_it; i++) {
			add_it = !synonym.equalsIgnoreCase(existing_synonyms.get(i));
		}
		add_it &= leaf.getLocalId() == null || !synonym.equalsIgnoreCase(leaf.getLocalId());
		add_it &= leaf.getSymbol() == null || !synonym.equalsIgnoreCase(leaf.getSymbol());

		if (add_it) {
			leaf.addSynonym(synonym);
		}
	}

	private static List<GeneAnnotation> paintAnnotationsFilter(List<GeneAnnotation> all_annotations) {
		List<GeneAnnotation> exp_annotations = new ArrayList<>();
		if (all_annotations != null) {
			for (GeneAnnotation annotation : all_annotations) {
				if (isExpAnnotation(annotation)) {
					exp_annotations.add(annotation);
				}
			}
		}
		return exp_annotations;
	}

	public static boolean isExpAnnotation(GeneAnnotation annotation) {
		String eco = annotation.getShortEvidence();
		boolean keep = false;
		if (Constant.EXP_strings.contains(eco)) {
			keep = true;
			if (annotation.getWithInfos() != null) {
				List<String> withs = (List<String>) annotation.getWithInfos();
				for (int i = withs.size() - 1; i >= 0 && keep; i--) {
					keep &= !withs.get(i).startsWith(Constant.PANTHER_DB);
				}
			}
			if (keep) {
				keep = !annotation.getAssignedBy().contains(Constant.REACTOME);
			}
			if (keep) {
				List<String> pub_ids = annotation.getReferenceIds();
				keep = false;
				for (String pub_id : pub_ids) {
					keep |= !isExcluded(pub_id);
				}
			}
		}
		return keep;
	}

	public static List<GeneAnnotation> getExpAssociations(Bioentity leaf) {
		return paintAnnotationsFilter(leaf.getAnnotations());
	}

	public static List<GeneAnnotation> getAspectExpAssociations(Bioentity leaf, String aspect) {
		List<GeneAnnotation> filtered_associations = getExpAssociations(leaf);
		for (int i = filtered_associations.size(); i > 0; i--) {
			GeneAnnotation annotation = filtered_associations.get(i - 1);
			if (!annotation.getAspect().equals(aspect)) {
				filtered_associations.remove(i - 1);
			}
		}
		return filtered_associations;
	}

	public static List<GeneAnnotation> getAspectPaintAssociations(Bioentity node, String aspect) {
		List<GeneAnnotation> paint_assoc = new ArrayList<>();
		List<GeneAnnotation> associations = node.getAnnotations();
		for (GeneAnnotation annotation : associations) {
			if (isPAINTAnnotation(annotation) && (aspect == null || annotation.getAspect().equals(aspect))) {
				paint_assoc.add(annotation);
			}
		}
		return paint_assoc;
	}


	public static boolean isPAINTAnnotation(GeneAnnotation assoc) {
		String source = assoc.getAssignedBy();
		boolean is_gocentral = (source.equals(Constant.PAINT_AS_SOURCE)
				|| source.equals(Constant.OLD_SOURCE));
		boolean is_paint = is_gocentral;
		if (is_gocentral) {
			List<String> refs = assoc.getReferenceIds();
			for (String ref : refs) {
				is_paint &= ref.startsWith(Constant.PAINT_REF);
			}
		}
		return is_paint;
	}

	/*
	 * Check each association to the gene product passed as a parameter
	 * If that association is either directly to that term
	 * or the existing association is to a more specific term
	 * then return that association
	 */
	public static GeneAnnotation isAnnotatedToTerm(List<GeneAnnotation> all_annotations, String term_id, String term_aspect) {
		GeneAnnotation annotated_with_term = null;
		if (all_annotations != null && term_id != null) {
			for (int i = 0; i < all_annotations.size() && annotated_with_term == null; i++) {
				GeneAnnotation check = all_annotations.get(i);
				if (check.getAspect().equals(term_aspect)) {
					String go_id = check.getCls();
					if (term_id.equals(go_id)) {
						annotated_with_term = check;
					}
					else if (OWLutil.inst().moreSpecific(go_id, term_id)) {
						if (!check.isNegated()) {
							annotated_with_term = check;
						} else {
							log.debug("Ignoring more specific annotation to " + go_id + " because it's negated");
						}
					}
				}
			}
		}
		return annotated_with_term;
	}

	public static GeneAnnotation isAnnotatedToTerm(List<GeneAnnotation> all_annotations, String term_id) {
		String aspect = OWLutil.inst().getAspect(term_id);
		return isAnnotatedToTerm(all_annotations, term_id, aspect);
	}

	private static boolean isExcluded(String pub_id) {
		boolean excluded = false;
		for (int i = 0; i < high_throughput.length && !excluded; i++) {
			excluded = pub_id.equals(high_throughput[i]);
		}
		return excluded;
	}

	public static boolean isAncestralNode(Bioentity node) {
		return node.getDb().equals(Constant.PANTHER_DB);
	}
}
