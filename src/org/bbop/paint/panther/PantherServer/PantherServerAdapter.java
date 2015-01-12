package org.bbop.paint.panther.PantherServer;

import com.sri.panther.paintCommon.RawComponentContainer;
import org.bbop.paint.model.Tree;
import org.bbop.paint.panther.PantherAdapter;
import org.bbop.paint.panther.ParsingHack;
import org.bbop.paint.touchup.Preferences;
import owltools.gaf.Bioentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * Created by suzi on 12/15/14.
 */
public class PantherServerAdapter extends PantherAdapter {

    public Tree fetchTree(String family_name) {
        Tree tree = null;
        if (family_name != null) {
            RawComponentContainer rcc = PantherIO.getRawPantherFamily(PantherLogin.getUserInfo(), family_name);
            if (rcc != null) {
                Vector<String[]> tree_info = (Vector<String[]>) rcc.getTree();
                tree_content = new ArrayList<>(Arrays.asList(tree_info.elementAt(RawComponentContainer.INDEX_TREE_STR)));

                Bioentity root = parsePantherTree(tree_content);
                if (root != null) {
                    tree = new Tree(family_name, root);

                    // Load the attr file to obtain the PTN #s
                    String[] attr_info = rcc.getAttributeTable();
                    attr_content = new ArrayList<>(Arrays.asList(attr_info));
                    List<List<String>>  rows = ParsingHack.parsePantherAttr(attr_content);
                    decorateNodes(rows, tree);
                }
                Vector<String []> msa_info = (Vector<String []>) rcc.getMSA();
                msa_content = msa_info != null && msa_info.size() > 0 ? new ArrayList<>(Arrays.asList(msa_info.elementAt(0))) : null;
                if (msa_content != null) {
                    // Check for wts file
                    wts_content = msa_info.size() > 1 ? new ArrayList<>(Arrays.asList(msa_info.elementAt(1))) : null;
                }

                if (tree != null && tree.getRoot().getNcbiTaxonId() == null) {
                    String taxon = Preferences.inst().getTaxonID("LUCA");
                    tree.getRoot().setNcbiTaxonId(taxon);
                }

            }
        }
        return tree;
    }

}
