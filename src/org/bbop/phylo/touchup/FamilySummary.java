package org.bbop.phylo.touchup;

import org.bbop.phylo.tracking.LogAlert;

import java.util.List;

/**
 * Created by suzi on 1/30/15.
 */
public class FamilySummary {

    private int cut;
    private int lost_annotations;
    private int obsolete_terms;

    /*
        This only works if called immediately after the family is loaded
     */
    public FamilySummary() {
        cut = LogAlert.getInvalidCount();
        lost_annotations = LogAlert.getMissingCount();
        obsolete_terms = LogAlert.getObsoleteCount();
    }

    public int summarize(String family_name, List<String> content) {
        int review_cnt = 0;
        if (cut != 0 || lost_annotations != 0 || obsolete_terms != 0) {
            content.add(family_name + " needs review ---");
            review_cnt++;
            if (cut != 0) {
                if (cut > 1) {
                    content.add(cut + " annotations have been removed");
                } else {
                    content.add(cut + " annotation has been removed");
                }
            }
            if (lost_annotations != 0) {
                if (lost_annotations > 1) {
                    content.add(lost_annotations + " ancestral nodes are no longer members of this family");
                } else {
                    content.add(lost_annotations + " ancestral node is no longer a member of this family");
                }
            }
            if (obsolete_terms != 0) {
                if (obsolete_terms > 1) {
                    content.add(obsolete_terms + " terms that were used for annotation have been made obsolete");
                } else {
                    content.add(obsolete_terms + " term that was used for annotation has been made obsolete");
                }
            }
            content.add("\n");
        }
        return review_cnt;
    }
}
