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
package org.bbop.paint.panther;

import org.apache.log4j.Logger;
import owltools.gaf.Bioentity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IDmap {
	/**
	 * 
	 */
	private static IDmap INSTANCE = null;

	private static final long serialVersionUID = 1L;

	private HashMap<String, List<Bioentity>> seqIdtoGene;
	private HashMap<String, Bioentity> paintIdtoGene;
	private HashMap<String, Bioentity> DbIdtoGene;
	private HashMap<String, Bioentity> ptnIdtoGene;

	private static final Logger log = Logger.getLogger(IDmap.class);

	private IDmap() {
		// Exists only to defeat instantiation.
	}

	public static synchronized IDmap inst() {
		if (INSTANCE == null) {
			INSTANCE = new IDmap();
		}
		return INSTANCE;
	}

	public void clearGeneIDs() {
		if (paintIdtoGene == null) {
			paintIdtoGene = new HashMap<String, Bioentity>();
		} else {
			paintIdtoGene.clear();
		}
		if (seqIdtoGene == null) {
			seqIdtoGene = new HashMap<String, List<Bioentity>>();
		} else {
			seqIdtoGene.clear();
		}
		if (DbIdtoGene == null) {
			DbIdtoGene = new HashMap<String, Bioentity>();
		} else {
			DbIdtoGene.clear();
		}
		if (ptnIdtoGene == null) {
			ptnIdtoGene = new HashMap<String, Bioentity>();
		} else {
			ptnIdtoGene.clear();
		}
	}

	public void indexByANid(Bioentity node) {
		String an_number = node.getPaintId();
		if (an_number == null || an_number.length() == 0) {
			log.error("Paint ID for node is missing!");
		} else if (paintIdtoGene.get(an_number) != null) {
			log.error("We've already indexed this node: " + an_number);
		} else {
			paintIdtoGene.put(an_number, node);	
		}			
	}

	public void indexBySeqID(Bioentity node) {
		indexBySeqID(node, node.getSeqDb(), node.getSeqId());
	}

	private void indexBySeqID(Bioentity node, String db, String db_id) {
		if (db_id != null && db_id.length() > 0) {
			String key = db + ':' + db_id.toUpperCase();
			List<Bioentity> genes = seqIdtoGene.get(key);
			if (genes == null) {
				genes = new ArrayList<Bioentity>();
				seqIdtoGene.put(key, genes);
			}
			if (!genes.contains(node))
				genes.add(node);
			else {
				log.debug("Already indexed " + key);
			}
		}
	}

	public void indexByDBID(Bioentity node) {
		String key = node.getId();
		if (key.indexOf(':') < 0)
			log.error("Bad key: " + key);
		if (key != null && key.length() > 0) {
			if (DbIdtoGene.get(key) == null)
				DbIdtoGene.put(key, node);
		}
	}

	public void indexNodeByPTN(Bioentity node) {
		String ptn_id = node.getPersistantNodeID();
		ptnIdtoGene.put(ptn_id, node);
		indexByDBID(node);
	}
	
	public Bioentity getGeneByPTNId(String id) {
		Bioentity node = null;
		if (id != null && ptnIdtoGene != null)
			node = ptnIdtoGene.get(id);
		return node;
	}

	public Bioentity getGeneByANId(String id) {
		if (id.length() == 0)
			return null;
		Bioentity node = paintIdtoGene.get(id);
		if (node == null && id.startsWith("PTHR")) {
			id = id.substring(id.indexOf('_') + 1);
			node = paintIdtoGene.get(id);
		}
		return node;
	}

	public List<Bioentity> getGenesBySeqId(String db, String id) {
		String key = db + ':' + id.toUpperCase();
		return getGenesBySeqId(key);
	}

	public List<Bioentity> getGenesBySeqId(String key) {
		List<Bioentity> node = seqIdtoGene.get(key);
		if (node == null && key.contains("PTHR")) {
			key = key.substring(key.indexOf('_') + 1);
			node = seqIdtoGene.get(key.toUpperCase());
		}
		return node;
	}

	public Bioentity getGeneByDbId(String key) {
		return DbIdtoGene.get(key);
	}

}
