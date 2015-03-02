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
package org.bbop.phylo.panther;

import org.apache.log4j.Logger;
import org.bbop.phylo.model.Family;
import org.bbop.phylo.model.Tree;
import org.bbop.phylo.touchup.Constant;
import org.bbop.phylo.touchup.Preferences;
import org.bbop.phylo.util.FileUtil;
import org.bbop.phylo.util.TaxonFinder;
import owltools.gaf.Bioentity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by suzi on 12/15/14.
 */
public abstract class PantherAdapter {

    private static final Logger log = Logger.getLogger(PantherAdapter.class);

    protected List<String> tree_content;
    protected List<String> attr_content;
    protected List<String> msa_content;
    protected List<String> wts_content;

    private static final String MSG_ERROR_WRITING_FILE = "Error writing file ";

    public abstract Tree fetchTree(String family_name);

    public boolean saveFamily(Family family) {
        String id = family.getFamily_name();
        File family_dir = new File(Preferences.inst().getGafDir(), id);

        boolean ok = FileUtil.validPath(family_dir);
        File treeFileName = new File(family_dir, id + Constant.TREE_SUFFIX);
        File attrFileName = new File(family_dir, id + Constant.ATTR_SUFFIX);

        ok &= writeData(treeFileName, tree_content);
        ok &= writeData(attrFileName, attr_content);

        if (msa_content != null && ok) {
            File msaFileName = new File(family_dir, id + Constant.MSA_SUFFIX);
            ok &= writeData(msaFileName, msa_content);
        }
        if (wts_content != null && ok) {
            File wtsFileName = new File(family_dir, id + Constant.WTS_SUFFIX);
            ok &= writeData(wtsFileName, wts_content);
        }
        return ok;
    }

    private boolean writeData(File file_name, List<String> data) {
        try {
            FileUtil.writeFile(file_name, data);
            return true;
        } catch (IOException e) {
            log.info(MSG_ERROR_WRITING_FILE + file_name);
            return false;
        }
    }

    protected Bioentity parsePantherTree(List<String> treeContents) {
        if (null == treeContents) {
            return null;
        }
        if (0 == treeContents.size()) {
            return null;
        }
        // Modify, if there are no line returns
        if (1 == treeContents.size()) {
            treeContents = ParsingHack.tokenize(treeContents.get(0), Constant.SEMI_COLON);
        }

        Bioentity root = null;

        for (String row : treeContents) {
            if (root == null) {
                root = parseTreeString(row);
            } else {
                int index = row.indexOf(Constant.COLON);
                String anId = row.substring(0, index);
                Bioentity node = IDmap.inst().getGeneByANId(anId);
                if (null == node) {
                    log.info("Found data for non-existent annotation node " + anId);
                    continue;
                }
                // minus 1 to trim the semi-colon off?
                ParsingHack.parseIDstr(node, row.substring(index + 1));
            }
        }
        recordOrigChildOrder(root);

        return root;

    }

    private void recordOrigChildOrder(Bioentity node) {
        if (null == node) {
            return;
        }

        node.setOriginalChildrenToCurrentChildren();
        List<Bioentity> children = node.getChildren();
        if (children != null) {
            for (Bioentity child : children) {
                recordOrigChildOrder(child);
            }
        }
    }

    Bioentity parseTreeString(String s) {
        Bioentity node = null;
        Bioentity root = null;
        StringTokenizer st = new StringTokenizer(s, Constant.DELIM, true);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.equals(Constant.OPEN_PAREN)) {
                if (null == node) {
					/*
					 * The real root node, first one set
					 */
                    node = new Bioentity();
                    root = node;
                }
                else {
                    Bioentity newChild = new Bioentity();
                    List<Bioentity> children = node.getChildren();
                    if (null == children) {
                        children = new ArrayList<Bioentity>();
                    }
                    children.add(newChild);
                    newChild.setParent(node);
                    node.setChildren(children);
					/*
					 * Move down
					 */
                    node = newChild;
                }
            }
            else if ((token.equals(Constant.CLOSE_PAREN)) ||
                    (token.equals(Constant.COMMA)) ||
                    (token.equals(Constant.SEMI_COLON))) {
                // Do nothing
            }
            else {
                int squareIndexStart = token.indexOf(Constant.OPEN_BRACKET);
                int squareIndexEnd = token.indexOf(Constant.CLOSE_BRACKET);
                if (0 == squareIndexStart) {
                    String type = token.substring(squareIndexStart, squareIndexEnd + 1);
					/*
					 * This is when the AN number is teased out
					 */
                    setTypeAndId(type, node);
                }
                else {
                    int index = token.indexOf(Constant.COLON);
                    if (0 == index) {
                        if (-1 == squareIndexStart) {
                            node.setDistanceFromParent(Float.valueOf(token.substring(index+1)).floatValue());
                        }
                        else {
                            node.setDistanceFromParent(Float.valueOf(token.substring((index+1), squareIndexStart)).floatValue());
                            String type = token.substring(squareIndexStart, squareIndexEnd + 1);
							/*
							 * This is when the AN number is teased out
							 */
                            setTypeAndId(type, node); // this use to be included in setType itself
                        }
						/*
						 * Move back up
						 */
                        node = node.getParent();
                    } else if (index > 0) {
                        Bioentity newChild = new Bioentity();
                        if (-1 == squareIndexStart) {
                            newChild.setDistanceFromParent(Float.valueOf(token.substring(index+1)).floatValue());
                            setTypeAndId(token.substring(0, index), newChild); // this use to be included in setType itself
                        }
                        else {
                            newChild.setDistanceFromParent(Float.valueOf(token.substring((index+1), squareIndexStart)).floatValue());
                            String type = token.substring(squareIndexStart, squareIndexEnd + 1);
							/*
							 * This is when the AN number is teased out
							 */
                            setTypeAndId(type, newChild); // this use to be included in setType itself
                        }
                        List<Bioentity> children = node.getChildren();
                        if (null == children) {
                            children = new ArrayList<Bioentity>();
                        }
						/*
						 * Add siblings to current node
						 */
                        children.add(newChild);
                        newChild.setParent(node);
                        node.setChildren(children);
                    }
                }
            }
        }
        return root;
    }

    private void setTypeAndId(String nodeType, Bioentity node) {
        if (null == nodeType) {
            return;
        }
        String annot_id;
        node.setTypeCls(Constant.PROTEIN);
        if (!nodeType.startsWith("AN")) {
            node.setType(nodeType);
            // collect the species while we're at it
            int index = nodeType.indexOf("S=");
            if (index >= 0) {
                int endIndex = nodeType.indexOf(Constant.COLON, index);
                if (-1 == endIndex) {
                    endIndex = nodeType.indexOf(Constant.CLOSE_BRACKET);
                }
                String species = nodeType.substring(index + "S=".length(), endIndex);
                node.addSpeciesLabel(species);
                String taxon = TaxonFinder.getTaxonID(species);
                if (taxon != null && taxon.length() > 0)
                    node.setNcbiTaxonId(taxon);
                else {
                    log.info("Could not find taxa ID for " + species + " on node " + node.getDBID());
                }
            }
            // now pick up the node name/id
            index = nodeType.indexOf(Constant.NODE_TYPE_ANNOTATION);
            if (index >= 0) {
                int endIndex = nodeType.indexOf(Constant.COLON, index);
                if (-1 == endIndex) {
                    endIndex = nodeType.indexOf(Constant.CLOSE_BRACKET);
                }
                annot_id = nodeType.substring(index + Constant.NODE_TYPE_ANNOTATION_LENGTH, endIndex);
            } else {
                annot_id = null;
            }
        } else {
            annot_id = nodeType;
        }

        if (node.getSpeciesLabel() == null || node.getSpeciesLabel().length() == 0) {
            String taxon = TaxonFinder.getTaxonID("LUCA");
            node.setNcbiTaxonId(taxon);
        }

        // now pick up the node name/id
        if (annot_id != null) {
            if (!annot_id.startsWith("AN"))
                log.info(annot_id + " isn't an AN number");
            if (node.getANid() != null && node.getANid().length() > 0) {
                log.info(annot_id + "AN number is already set to " + node.getANid());
            }
            node.setANid(annot_id);
            IDmap.inst().indexByANid(node);
        }

    }

    protected void decorateNodes(List<List<String>> rows, Tree tree) {
        if (null == rows || tree == null) {
            return;
        }
        if (rows.get(0) != null) {
            List<String> header = rows.get(0);
			/* skip the table headers from the first row */
			/* now go after the data */
            for (int i = 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                parseAttributeRow (row, header);
            }
        }
    }

    private static final String ACC_TAG = "gi";
    private static final String PROT_TAG = "Protein Id";
    private static final String ORG_TAG = "organism";
    private static final String SYMB_TAG = "gene symbol";

    private void parseAttributeRow(List<String> row, List<String> header) {
        String id = row.get(0);
        String ptn = row.get(row.size() - 1);
        Bioentity node = ParsingHack.findThatNode(id);
        if (node == null || (node.getPersistantNodeID() != null && !node.getPersistantNodeID().equals(ptn))) {
			/*
			 * This should never happen!
			 */
            if (node == null) {
                log.error("This node is not in the family tree: " + id + " - " + ptn);
            } else {
                log.error("Yikes, " + node.getPersistantNodeID() + " does not equal " + ptn);
            }
            return;
        }
        else {
            node.setPersistantNodeID("PANTHER", ptn);
            IDmap.inst().indexNodeByPTN(node);
        }

        for (int j = 0; j < row.size(); j++) {
            String tag = header.get(j);
            String value = row.get(j);
            value = value != null ? value.trim() : Constant.STR_EMPTY;
            if (tag.equals(ACC_TAG) || tag.equals(PROT_TAG)) {
                if (node.getSeqId() == null) {
                    node.setSeqId(node.getSeqDb(), value);
                    IDmap.inst().indexBySeqID(node);
                    log.info("Set accession after the fact for: " + value);
                }
            } else if (tag.equals(ORG_TAG)) {
                node.addSpeciesLabel(value);
                if (node.getNcbiTaxonId() == null) {
                    String taxon = TaxonFinder.getTaxonID(value);
                    node.setNcbiTaxonId(taxon);
                }
            } else if (tag.equals(SYMB_TAG)) {
                node.setSymbol(value);
            }
        }
    }

    private static final String PREFIX_SEQ_START = ">";

    int parseSeqs(List<String> seqInfo, Map<Bioentity, String> sequences) {

        int seq_length = 0;

        if ((null == seqInfo) || (0 == seqInfo.size())){
            return seq_length;
        }

        IDmap mapper = IDmap.inst();

        StringBuffer  sb = new StringBuffer();
        Bioentity node = null;
        for (String line : seqInfo){
            if (line.startsWith(PREFIX_SEQ_START)) {
                String paint_id = line.replaceFirst(PREFIX_SEQ_START, Constant.STR_EMPTY);
                if (node != null) {
                    String seq = sb.toString();
                    sequences.put(node, seq);
                    // Get the maximum sequence length
                    if (seq_length == 0){
                        seq_length = seq.length();
                    }
                }
                node = mapper.getGeneByANId(paint_id);
                if (node == null) {
                    log.error("Unable to get gene " + paint_id + " for MSA data");
                    continue;
                }
                sb.delete(0, sb.length());
            }
            else {
                sb.append(line.trim());
            }
        }
        if (node != null) {
            String seq = sb.toString();
            sequences.put(node, seq);
            // Get the maximum sequence length
            if (seq_length == 0){
                seq_length = seq.length();
            }
        }
        return seq_length;
    }

    void parseWts(List<String> wtsInfo, Map<Bioentity, Double> weights) {
        if (wtsInfo != null && wtsInfo.size() > 0) {
            // Ignore first two lines
            for (int i = 2; i < wtsInfo.size(); i++){
                List<String>  seqWt = ParsingHack.tokenize(wtsInfo.get(i), Constant.SPACE);
                Bioentity gene = ParsingHack.findThatNode(seqWt.get(0));
                if (gene != null)
                    weights.put(gene, new Double(seqWt.get(1)));
                else
                    log.warn ("Unable to parse ID from wts row " + seqWt.get(0));
            }
        }
        else {
            weights.clear();
        }
    }


}

