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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
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

    public static void collectExpAnnotations(Family family) throws Exception {
        Tree tree = family.getTree();
        List<Bioentity> leaves = tree.getLeaves();
        //RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("http://golr.geneontology.org/solr", 3, true) {
        RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("http://golr.berkeleybop.org") {
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
                randomWait(2000, 5000);
                log.info("retrying " + System.currentTimeMillis());
            }
        };
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
                    processGolrAnnotations(leaf, annots.getGeneAnnotations());
                }
            } catch (Exception e) {
                String message = "Problem collecting experimental annotations for " + leaf + "\n" + e.getMessage();
                log.info(message);
                throw new Exception(message, e.getCause());
            }
        }
    }

    public static void collectExpAnnotationsBatched(Family family) throws Exception {
        Tree tree = family.getTree();
        List<Bioentity> leaves = tree.getLeaves();
        RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("http://golr.geneontology.org/solr", 3, true) {
            //       RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("http://golr.berkeleybop.org") {
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
                randomWait(2000, 5000);
                log.info("retrying " + System.currentTimeMillis());
            }
        };

        List<String> gene_names = new ArrayList<>();
        Map<String, Bioentity> id2gene = new HashMap<>();

        for (Bioentity leaf : leaves) {
            String key = leaf.getId();
            gene_names.add(key);
            id2gene.put(key, leaf);
            // while we're at it, include the sequence ID
            // as a synonym
            addSynonym(leaf, leaf.getSeqDb() + ':' + leaf.getSeqId());
        }
        askGolr(retriever, gene_names, id2gene);

        for (String gene_name : id2gene.keySet()) {
            Bioentity leaf = id2gene.get(gene_name);


            String key = leaf.getSeqDb() + ':' + leaf.getSeqId();
            List<GolrAnnotationDocument> golrDocuments;

            golrDocuments = retriever.getGolrAnnotationsForGene(key);

            if (golrDocuments.size() == 0) {
                golrDocuments = retriever.getGolrAnnotationsForSynonym(leaf.getDb(), leaf.getDBID());
            } else {
                log.info("Found gene using seq ID for " + leaf);
            }

            if (golrDocuments.size() > 0) {
                GafDocument annots = retriever.convert(golrDocuments);
                Collection<Bioentity> bioentities = annots.getBioentities();
                if (bioentities.size() != 1) {
                    log.info(bioentities.size() + " annotations returned for " + leaf.getId());
                } else {
                    processGolrAnnotations(leaf, annots.getGeneAnnotations());
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
                        id2gene.remove(golr_gene.getId());
                        List<GeneAnnotation> golr_annotations = (List<GeneAnnotation>) annots.getGeneAnnotations(leaf.getId());
                        processGolrAnnotations(leaf, golr_annotations);
                    } else {
                        log.info("Whoa! Could not find matching PANTHER node for " + golr_gene);
                    }
                }
            }
        } catch (Exception e) {
            String message = "Problem collecting experimental annotations for \n" + e.getMessage();
            log.info(message);
        }
    }

    private static void processGolrAnnotations(Bioentity leaf, List<GeneAnnotation> golr_annotations) {
        List<GeneAnnotation> exp_annotations = paintAnnotationsFilter(golr_annotations);
        leaf.setAnnotations(exp_annotations);
        // lets compare and fill in any missing fields
        if (golr_annotations != null && golr_annotations.size() > 0) {
            Bioentity go_node = golr_annotations.get(0).getBioentityObject();
            if (leaf.getNcbiTaxonId() == null ||
                    leaf.getNcbiTaxonId().length() == 0 ||
                    leaf.getNcbiTaxonId().endsWith(":1")) {
                leaf.setNcbiTaxonId(go_node.getNcbiTaxonId());
            }
            if (!go_node.getId().equals(leaf.getId()) && !go_node.getDb().equals("UniProtKB")) {
                // default to the identifier used by the GO database
                addSynonym(leaf, leaf.getDBID());
                leaf.setId(go_node.getId());
            }
            if (leaf.getFullName() == null || leaf.getFullName().length() == 0) {
                leaf.setFullName(go_node.getFullName());
            }
            if (go_node.getSynonyms() != null) {
                for (String synonym : go_node.getSynonyms()) {
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
        if (add_it) {
            leaf.addSynonym(synonym);
        }
    }

    private static List<GeneAnnotation> paintAnnotationsFilter(List<GeneAnnotation> all_annotations) {
        List<GeneAnnotation> exp_annotations = new ArrayList<>();
        if (all_annotations != null) {
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
        }
        return exp_annotations;
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

    public static boolean isPAINTAnnotation(GeneAnnotation assoc) {
        String source = assoc.getAssignedBy();
        return (source.equals(Constant.PAINT_AS_SOURCE) || source.equals(Constant.OLD_SOURCE));
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
