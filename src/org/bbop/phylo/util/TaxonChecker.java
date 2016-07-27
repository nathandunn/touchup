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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.bbop.phylo.model.Tree;

import owltools.gaf.Bioentity;

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

	private static boolean io_error = false;

	private static final Logger log = Logger.getLogger(TaxonChecker.class);

	private static final int MAX_TAXA_TO_CHECK = 60;

	private static String error_message;
	
	public static boolean checkTaxons(Tree tree, Bioentity node, String go_id, boolean ancestral) {
		int attempts = 0;
		boolean valid_taxon = false;
		io_error = true;
		while (io_error && attempts++ < 3) {
			valid_taxon = queryTaxons(tree, node, go_id, ancestral);
		}
		if (io_error) {
			log.info("Taxon server is down");
		}
		return valid_taxon;
	}
	
	private static boolean queryTaxons(Tree tree, Bioentity node, String go_id, boolean ancestral) {
		List<String> taxa_to_check = getTaxIDs(tree, node, ancestral);
		boolean descendents_okay = true;
		error_message = "";
		int checked_off = 0;
		String taxa_reply = "";
		io_error = false;
		while (descendents_okay && checked_off < taxa_to_check.size()) {
			StringBuffer taxon_query = new StringBuffer(TAXON_SERVER_URL + "&id=" + go_id );
			int max = Math.min(MAX_TAXA_TO_CHECK + checked_off, taxa_to_check.size());
			for (; checked_off < max; checked_off++) {
				String taxon = taxa_to_check.get(checked_off);
				taxon_query.append("&taxid=NCBITaxon:" + taxon);
			}
			taxa_reply = askTaxonServer(taxon_query);
			descendents_okay &= !io_error && !(taxa_reply.contains("false"));
		}
		if (!descendents_okay) {
			if (!io_error) {
				formatErrorMessage(go_id, taxa_reply);
			} else {
				randomWait(5000, 10000);
			}
			return false;
		} else {
			return true;
		}
	}

	private static void randomWait(int min, int max) {
		Random random = new Random(System.currentTimeMillis());
		long wait = min + random.nextInt((max - min));
		try {
			Thread.sleep(wait);
		} catch (InterruptedException exception) {
			// ignore
		}
	}

	private static void formatErrorMessage(String go_id, String taxon_reply) {
		String [] results = taxon_reply.split("\\s+");
		error_message = "illegal taxa for " + go_id + " - ";
		String prefix = "";
		for (int i = 0; i < results.length; i += 3) {
			if (results[i+2].contains("false")) {
				error_message += prefix + results[i+1].trim();
				prefix = ", ";
			}
		}
	}

	public static String getTaxonError() {
		return error_message;
	}

	private static String askTaxonServer(StringBuffer taxon_query) {
		URL servlet;
		StringBuffer taxon_reply = new StringBuffer();
		try {
			servlet = new URL(taxon_query.toString());
		} catch (MalformedURLException muex) {
			log.error("Attempted to create URL: " + muex.getLocalizedMessage() + " " + taxon_query);
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
			if (!io_error) {
				log.error("Attempted to open URL: " + e1.getLocalizedMessage() + " " + taxon_query);
				io_error = true;
			}
		}
		return taxon_reply.toString();
	}

	public static boolean isLive() {
		if (!io_error) {
			// check it
			StringBuffer test_query = new StringBuffer(TAXON_SERVER_URL + TAXON_SERVER_TEST);
			askTaxonServer(test_query);
		}
		return !io_error;
	}

	private static List<String> getTaxIDs(Tree tree, Bioentity node, boolean ancestral) {
		List<String> taxon_to_check = new ArrayList<>();
		String taxon_id = parseTaxonID(node);
		if (ancestral) {
			if (taxon_id != null) { // && !taxon_id.equals("1") && !taxon_id.equals("2")) {
				taxon_to_check.add(taxon_id);
			}
		} else {
			// too vague, look for children
			List<Bioentity> leaves = tree.getLeafDescendants(node);
			for (Bioentity leaf : leaves) {
				String leaf_taxon = parseTaxonID(leaf);
				if (leaf_taxon != null && !leaf_taxon.equals("1") && !taxon_to_check.contains(leaf_taxon)) {
					taxon_to_check.add(leaf_taxon);
				}
			}
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
