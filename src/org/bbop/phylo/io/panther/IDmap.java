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
package org.bbop.phylo.io.panther;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bbop.phylo.model.Protein;
import org.bbop.phylo.util.Constant;

public class IDmap {
    /**
     *
     */
    private static IDmap INSTANCE = null;

    private HashMap<String, Protein> ANidToGene;
    private HashMap<String, List<Protein>> seqIdtoGene;
    private HashMap<String, List<Protein>> DbIdtoGene;
    private HashMap<String, Protein> ptnIdtoGene;
    private Set<String> duplicates;

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
        if (ANidToGene == null) {
            ANidToGene = new HashMap<String, Protein>();
        } else {
            ANidToGene.clear();
        }
        if (seqIdtoGene == null) {
            seqIdtoGene = new HashMap<String, List<Protein>>();
        } else {
            seqIdtoGene.clear();
        }
        if (DbIdtoGene == null) {
            DbIdtoGene = new HashMap<String, List<Protein>>();
        } else {
            DbIdtoGene.clear();
        }
        if (ptnIdtoGene == null) {
            ptnIdtoGene = new HashMap<String, Protein>();
        } else {
            ptnIdtoGene.clear();
        }
        if (duplicates == null) {
        	duplicates = new HashSet<>();
        }
    }

    public void indexByANid(Protein node) {
        String an_number = node.getPaintId();
        if (an_number == null || an_number.length() == 0) {
            log.error("PANTHER AN ID for node is missing!");
        } else if (ANidToGene.get(an_number) != null) {
            log.error("We've already indexed this node: " + an_number);
        } else {
            ANidToGene.put(an_number, node);
        }
    }

    public void indexBySeqID(Protein node) {
        indexBySeqID(node, node.getSeqDb(), node.getSeqId());
    }

    private void indexBySeqID(Protein node, String db, String db_id) {
        if (db_id != null && db_id.length() > 0) {
            String key = db + ':' + db_id.toUpperCase();
            List<Protein> genes = seqIdtoGene.get(key);
            if (genes == null) {
                genes = new ArrayList<Protein>();
                seqIdtoGene.put(key, genes);
            } else {
            	String msg = ("Identical Seq ID (" +  node.getSeqDb() + ":" + node.getSeqId() + ") for " + 
            			(genes.size() + 1) + " different genes: ");
            	for (Protein g : genes) {
            		msg = msg + g.getId() + " ";
            	}
            	msg = msg + node.getId();
            	log.info(msg);
            }
            if (!genes.contains(node))
                genes.add(node);
            else {
                log.info("Already indexed " + key);
            }
        }
    }
    
    public void replace(Protein node, String old_id) {
    	List<Protein> priors = DbIdtoGene.get(old_id);
    	if (priors != null && priors.size() >= 1) {
    		priors.remove(node);
    	} else {
    		log.debug("Couldn't find node to replace " + node.getId());
    	}
    	indexByDBID(node);
    }

    public void indexByDBID(Protein node) {
        String key = node.getId();
        indexByDBID(key, node);
    }

    private void indexByDBID(String key, Protein node) {
        if (key.length() > 0) {
        	List<Protein> priors = DbIdtoGene.get(key);
            if (priors == null) {
            	priors = new ArrayList<Protein>();
            	DbIdtoGene.put(key, priors);
            } else {
            	String msg = ("Identical Gene ID (" + node.getId() + ") for " +
            			(priors.size() + 1) + " different sequence IDs: ");
            	for (Protein g : priors) {
            		msg = msg + " " + g.getSeqDb() + ':' + g.getSeqId();
            	}
            	msg = msg + " " + node.getSeqDb() + ':' + node.getSeqId();
            	log.info(msg);
            }
            if (!priors.contains(node)) {
                priors.add(node);
            }
        }
    }

    public void indexNodeByPTN(Protein node) {
        String ptn_id = node.getPersistantNodeID();
        if (ptnIdtoGene.get(ptn_id) == null)
            ptnIdtoGene.put(ptn_id, node);
        else
            log.info("Already indexed " + node + " by " + ptn_id);
        if (DbIdtoGene.get(node.getId()) == null) {
        	node.setSeqId(Constant.PANTHER_DB, ptn_id);
            indexByDBID(node);
			indexBySeqID(node);
        }
    }

    public Protein getGeneByPTNId(String id) {
        Protein node = null;
        if (id != null && ptnIdtoGene != null)
            node = ptnIdtoGene.get(id);
        return node;
    }

    public Protein getGeneByANId(String id) {
        if (id.length() == 0)
            return null;
        Protein node = ANidToGene.get(id);
        if (node == null && id.startsWith("PTHR")) {
            id = id.substring(id.indexOf('_') + 1);
            node = ANidToGene.get(id);
        }
        return node;
    }

    public List<Protein> getGenesBySeqId(String db, String id) {
        String key = db + ':' + id.toUpperCase();
        return getGenesBySeqId(key);
    }

    private List<Protein> getGenesBySeqId(String key) {
        List<Protein> nodes = seqIdtoGene.get(key);
        if (nodes == null && key.contains("PTHR")) {
            key = key.substring(key.indexOf('_') + 1);
            nodes = seqIdtoGene.get(key.toUpperCase());
        }
        return nodes;
    }

    public List<Protein> getGeneByDbId(String key) {
        List<Protein> genes = DbIdtoGene.get(key);
        return genes;
    }

}
