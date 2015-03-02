package org.bbop.phylo.annotate;

import org.apache.log4j.Logger;
import org.bbop.golr.java.RetrieveGolrAnnotations;
import org.bbop.golr.java.RetrieveGolrAnnotations.GolrAnnotationDocument;
import org.bbop.phylo.model.Family;
import org.bbop.phylo.model.Tree;
import org.bbop.phylo.touchup.Constant;
import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    };

    private static final Logger log = Logger.getLogger("AnnotationUtil.class");

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
                    paintAnnotationsFilter(leaf, annotations);
                }
            } catch (Exception e) {
                log.info("Problem filtering out previous PAINT annotations for " + leaf);
            }
        }
    }

    private static void paintAnnotationsFilter(Bioentity node, List<GeneAnnotation> all_annotations) {
        if (all_annotations != null) {
            List<GeneAnnotation> exp_annotations = new ArrayList<>();
            for (GeneAnnotation annotation : all_annotations) {
                String eco = annotation.getShortEvidence();
                if (Constant.EXP_strings.contains(eco)) {
                    boolean keep = true;
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
                    if (keep) {
                        exp_annotations.add(annotation);
                    }
                }
            }
            node.setAnnotations(exp_annotations);
            // lets compare
            Bioentity go_node = all_annotations.get(0).getBioentityObject();
            node.setFullName(go_node.getFullName());
            node.setNcbiTaxonId(go_node.getNcbiTaxonId());
            if (go_node.getSynonyms() != null) {
                for (String synonym : go_node.getSynonyms()) {
                    node.addSynonym(synonym);
                }
            }
        }
    }

    public static List<GeneAnnotation> getExpAssociations(Bioentity leaf) {
        List<GeneAnnotation> annotations = leaf.getAnnotations();
        List<GeneAnnotation> exp_annotations = new ArrayList<>();
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

    public static boolean isExcluded(String pub_id) {
        boolean excluded = false;
        for (int i = 0; i < high_throughput.length && !excluded; i++) {
            excluded = pub_id.equals(high_throughput[i]);
        }
        return excluded;
    }
}
