package org.bbop.paint.util;

import org.apache.log4j.Logger;
import org.bbop.golr.java.RetrieveGolrAnnotations;
import org.bbop.golr.java.RetrieveGolrAnnotations.GolrAnnotationDocument;
import org.bbop.paint.model.Family;
import org.bbop.paint.model.Tree;
import org.bbop.paint.touchup.Constant;
import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class AnnotationUtil {

	private static Logger log = Logger.getLogger("AnnotationUtil.class");

	public static void collectExpAnnotations(Family family) {
		Tree tree = family.getTree();
		List<Bioentity> leaves = tree.getLeaves();
		RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("http://golr.berkeleybop.org");
		for (Bioentity leaf : leaves) {
			String key = leaf.getId();
			try {
				List<GolrAnnotationDocument> golrDocuments = retriever.getGolrAnnotationsForGene(key);
				if (golrDocuments.size() == 0) {
					// Try with the seq ID as key
					key = leaf.getSeqDb() + ':' + leaf.getSeqId();
					golrDocuments = retriever.getGolrAnnotationsForGene(key);
				}
				if (golrDocuments.size() == 0) {
					golrDocuments = retriever.getGolrAnnotationsForSynonym(leaf.getDb(), leaf.getDBID());
				}
				if (golrDocuments.size() > 0) {
					GafDocument annots = retriever.convert(golrDocuments);
					Collection<Bioentity> bioentities = annots.getBioentities();
					if (bioentities.size() != 1) {
						log.info(bioentities.size() + " annotations returned for " + key);
						continue;
					}
					List<GeneAnnotation> annotations = annots.getGeneAnnotations();
					experimentalAnnotationsFilter(leaf, annotations);
				}
			} catch (Exception e) {
				log.info(e.getMessage());
			}
		}
	}

	private static void experimentalAnnotationsFilter(Bioentity node, List<GeneAnnotation> all_annotations) {
		if (all_annotations != null) {
			List<GeneAnnotation> exp_annotations = new ArrayList<GeneAnnotation>();
			for (GeneAnnotation annotation : all_annotations) {
				String eco = annotation.getShortEvidence();
				if (Constant.EXP_strings.contains(eco)) {
					exp_annotations.add(annotation);
					Collection<String> withs = annotation.getWithInfos();
					if (withs != null) {
						for (Iterator<String> wit = withs.iterator(); wit.hasNext();) {
							String with = wit.next();
							if (with.startsWith(Constant.PANTHER_DB))
								withs.remove(with);
						}
						annotation.setWithInfos(withs);
					}
				}
			}
			node.setAnnotations(exp_annotations);
			// lets compare
			Bioentity go_node = all_annotations.get(0).getBioentityObject();
			node.setFullName(go_node.getFullName());
			node.setNcbiTaxonId(go_node.getNcbiTaxonId());
			node.setTypeCls(go_node.getTypeCls());
			if (go_node.getSynonyms() != null) {
				for (String synonym : node.getSynonyms()) {
					node.addSynonym(synonym);
				}
			}
		}
	}

	public static List<GeneAnnotation> getExpAssociations(Bioentity leaf) {
		List<GeneAnnotation> annotations = leaf.getAnnotations();
		List<GeneAnnotation> exp_annotations = new ArrayList<GeneAnnotation>();
		if (annotations != null) {
			for (GeneAnnotation annotation : annotations) {
				String eco = annotation.getShortEvidence();
				if (Constant.EXP_strings.contains(eco)) {
					exp_annotations.add(annotation);
				}
			}
		}
		return exp_annotations;
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

	public static boolean isPAINTAnnotation(GeneAnnotation assoc) {
		String source = assoc.getAssignedBy();
		return (source.equals(Constant.PAINT_AS_SOURCE) || source.equals(Constant.OLD_SOURCE));
	}

	public static boolean isExperimental(GeneAnnotation annotation) {
		return Constant.EXP_strings.contains(annotation.getShortEvidence());
	}

}
