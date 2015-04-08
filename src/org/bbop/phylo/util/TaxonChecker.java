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


package org.bbop.phylo.util;

import org.apache.log4j.Logger;
import org.bbop.phylo.model.Tree;
import owltools.gaf.Bioentity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * @author suzi
 *
 */
public class TaxonChecker {

    //	private static final String TAXON_SERVER_URL = "http://localhost:9999/isClassApplicableForTaxon?format=txt&idstyle=obo";
    //	private static final String TAXON_SERVER_URL = "http://toaster.lbl.gov:9999/isClassApplicableForTaxon?format=txt&idstyle=obo";
    //	id=GO:0007400&id=GO:0048658&id=GO:0090127&taxid=NCBITaxon:3702&taxid=NCBITaxon:9606&
    private static final String TAXON_SERVER_URL = "http://owlservices.berkeleybop.org/isClassApplicableForTaxon?format=txt&idstyle=obo";

    private static final String TAXON_SERVER_TEST = "&id=GO:0007400&taxid=NCBITaxon:3702";

    private static boolean server_is_down = false;

    private static final Logger log = Logger.getLogger(TaxonChecker.class);

    public static boolean checkTaxons(Tree tree, Bioentity node, String go_id) {
        boolean okay = true;
        if (server_is_down) {
            return false;
        }

        List<String> taxa_to_check = getTaxIDs(tree, node);
        for (String taxon : taxa_to_check) {
            StringBuffer taxon_query = new StringBuffer(TAXON_SERVER_URL + "&id=" + go_id + "&taxid=NCBITaxon:" + taxon);
            String taxon_reply = askTaxonServer(taxon_query);
            okay &= !server_is_down && !(taxon_reply.contains("false"));
       }
        if (!okay) {
            if (!server_is_down)
                log.info("Invalid taxon for term: " + go_id + " to " + node);
        }
        return okay;
    }

    private static String askTaxonServer(StringBuffer taxon_query) {
        URL servlet;
        StringBuffer taxon_reply = new StringBuffer();
        try {
            servlet = new URL(taxon_query.toString());
        } catch (MalformedURLException muex) {
            log.error("Attempted to create URL: " + muex.getLocalizedMessage() + " " + taxon_query);
            server_is_down = true;
            return taxon_reply.toString();
        }
        BufferedReader in;
        try {
            URLConnection conn = servlet.openConnection();
            conn.setConnectTimeout(1000); // 1 second timeout
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                taxon_reply.append(inputLine).append(' ');
            }
            in.close();
        } catch (IOException e1) {
            if (!server_is_down) {
                log.error("Attempted to open URL: " + e1.getLocalizedMessage() + " " + taxon_query);
                server_is_down = true;
            }
        }
        return taxon_reply.toString();
    }

    public static boolean isLive() {
        if (!server_is_down) {
            // check it
            StringBuffer test_query = new StringBuffer(TAXON_SERVER_URL + TAXON_SERVER_TEST);
            askTaxonServer(test_query);
        }
        return !server_is_down;
    }

    private static List<String> getTaxIDs(Tree tree, Bioentity node) {
        List<String> taxon_to_check = new ArrayList<>();

        boolean check_leaves = false;
        String taxon_id = parseTaxonID(node);
        if (taxon_id == null || taxon_id.equals("1") || taxon_id.equals("2")) {
            check_leaves = true;
        }
        if (check_leaves) {
            // too vague, look for children
            List<Bioentity> leaves = tree.getLeafDescendants(node);
            for (Bioentity leaf : leaves) {
                String leaf_taxon = parseTaxonID(leaf);
                if (leaf_taxon != null && !leaf_taxon.equals("1")) {
                    taxon_to_check.add(leaf_taxon);
                }
            }
        } else {
            taxon_to_check.add(taxon_id);
        }
        return taxon_to_check;
    }

    private static String parseTaxonID(Bioentity node) {
        String ncbi_taxon_id = node.getNcbiTaxonId();
        String taxon_id = null;
        if (ncbi_taxon_id != null) {
            int separator = ncbi_taxon_id.indexOf(':');
            if (separator > 0) {
                taxon_id = ncbi_taxon_id.substring(separator + 1);
            }
        }
        return taxon_id;
    }
}
