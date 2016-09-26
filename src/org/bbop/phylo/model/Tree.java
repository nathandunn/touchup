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

package org.bbop.phylo.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import owltools.gaf.Bioentity;
import owltools.gaf.BioentityDocument;

public class Tree extends BioentityDocument implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger("Tree.class");

	protected Protein root = null;
	private List<Bioentity> currentNodes = null; // only the nodes that are visible, as some may be collapsed or pruned away
	private List<Protein> terminusNodes = null; // only the terminal nodes (i.e. leaves, collapsed or pruned stubs)

	/*
	 * For ordering operations on the tree
	 */
	protected Map<Protein, Integer> descendent_count;
	protected int species_count;
	protected Map<Protein, Integer> species_index;
    private boolean rooted;
    private boolean rerootable;
    private String distance_unit;

	/**
	 * Constructor declaration
	 *
	 *
	 * @param id, which is typically a file name
	 *
	 * @see
	 */
	public Tree(String id) {
		super(id);
		rooted = true;
		setRerootable(false);
	}

	public Tree() {
		super("unnamed tree");
	}
	
	public void growTree(Protein node) {
		if (null == node){
			return;
		}
		root = node;

		descendent_count = new HashMap<Protein, Integer>();
		species_index = new HashMap<Protein, Integer>();
		species_count = 0;
		initSortGuides(root);

		addChildNodesInOrder(root, bioentities);
		currentNodes = new ArrayList<Bioentity>();
		terminusNodes = new ArrayList<Protein>();
		initCurrentNodes();
	}

	// Creation of nodes in vector

	/**
	 * Method declaration
	 *
	 *
	 * @see
	 */
	/**
	 * Method declaration
	 *
	 * Order the list of all nodes
	 * @param node
	 * @param node_list
	 *
	 * @see
	 */
	protected void addChildNodesInOrder(Protein node, List<Bioentity> node_list) {
		if (node != null) {
			if (node.isPruned())
				log.info("Pruned node");
			if (node.isTerminus()) {
				node_list.add(node);
			} else {
				/* 
				 * Don't add the parent until the children above it have been added first
				 */
				List<Protein> topChildren = getTopChildren(node);
				for (Protein top_child : topChildren) {
					addChildNodesInOrder(top_child, node_list);
				}

				// Add the parent
				node_list.add(node);

				List<Protein> bottomChildren = getBottomChildren(node);
				for (Protein bottom_child : bottomChildren) {
					addChildNodesInOrder(bottom_child, node_list);
				}
			}
		}
	}

	/*
	 * This is only called during initialization of a new family
	 */
	private int initSortGuides(Protein node) {
		int count = 0;
		species_index.put(node, new Integer(species_count++));
		if (node != null) {
			if (!node.isTerminus()) {
				count = node.getChildren().size();
				/* 
				 * Don't add the parent until the children above it have been added first
				 */
				List<Bioentity> children = node.getChildren();
				for (int i = 0; i < children.size(); i++) {
					Bioentity child = children.get(i);
					count += initSortGuides((Protein)child);
				}
			}
			descendent_count.put((Protein)node, new Integer(count));
		}
		return count;
	}
	// Methods for getting children of a given node
	// Gets the children displayed above the current node

	/**
	 * Method declaration
	 *
	 *
	 * @param dsn
	 *
	 * @return
	 *
	 * @see
	 */
	public List<Protein> getTopChildren(Protein dsn){
		return getChildren(true, dsn);
	}

	// Gets the children displayed at the same level as the node and below the current node

	/**
	 * Method declaration
	 *
	 *
	 * @param dsn
	 *
	 * @return
	 *
	 * @see
	 */
	public List<Protein> getBottomChildren(Protein dsn){
		return getChildren(false, dsn);
	}

	/**
	 * Method declaration
	 *
	 *
	 * @param top
	 * @param dsn
	 *
	 * @return
	 *
	 * @see
	 */
	private List<Protein> getChildren(boolean top, Protein dsn){
		List<Bioentity>  children = dsn.getChildren();
		if (null == children){
			return null;
		}

		// Add remainder to handle case where there are an odd number of children
		int     half = children.size() / 2 + children.size() % 2;
		List<Protein>  returnList = new ArrayList<Protein>();

		if (top) {
			for (int i = 0; i < half; i++) {
				Bioentity node = children.get(i);
				if (!returnList.contains(node))
					returnList.add((Protein)node);
			}
		}
		else {
			for (int i = half; i < children.size(); i++) {
				Bioentity node = children.get(i);
				if (!returnList.contains(node))
					returnList.add((Protein)node);
			}
		}
		return returnList;
	}

	/**
	 * Gets the node which is currently displayed at the top of
	 * the list (i.e. the first row) for a given clade/ancestral node
	 * @param node, where to start from
	 * @returns Protein
	 */
	public Protein getTopLeafNode(Bioentity node) {
		Protein top_leaf = null;
		if (node != null) {
			if (!terminusNodes.contains(node) && node.getChildren() != null) {
				top_leaf = getTopLeafNode(node.getChildren().get(0));
			} else {
				top_leaf = (Protein) node;
			}
		}
		return top_leaf;
	}

	public Protein getBottomLeafNode(Bioentity node) {
		Protein bottom_leaf = null;
		if (node != null) {
			if (!terminusNodes.contains(node) && node.getChildren() != null) {
				List<Bioentity> children = node.getChildren();
				bottom_leaf = getBottomLeafNode(children.get(children.size() - 1));
			} else {
				bottom_leaf = (Protein) node;
			}
		}
		return bottom_leaf;
	}


	// Method to set number of leaves in tree
	public void initCurrentNodes() {
		currentNodes.clear();
		terminusNodes.clear();
		addChildNodesInOrder(getCurrentRoot(), currentNodes);
		setTerminusNodes();
	}

	/**
	 * Method declaration
	 *
	 *
	 * @param
	 *
	 * @see
	 */
	private void setTerminusNodes() {
		for (Bioentity node : currentNodes) {
			if (node.isTerminus()) {
				terminusNodes.add((Protein) node);
			}
		}
	}

	/**
	 * getMRCA
	 *
	 * @param gene1 - one leaf in the tree
	 * @param gene2 - a second leaf in the tree
	 *
	 * @return Node - the node that is an ancestor to both of these two leaves
	 */

	public Protein getMRCA(Protein gene1, Protein gene2) {
		Protein ancestor = null;
		if (gene1.isLeaf() && gene2.isLeaf()) {
			if (gene1 == gene2) {
				ancestor = gene1;
			} else {
				while (ancestor == null && gene1 != null) {
					Bioentity ancestor1 =  gene1.getParent();
					if (isDescendentOf(ancestor1, gene2)) {
						ancestor = (Protein) ancestor1;
					} else {
						gene1 = (Protein) ancestor1;
					}
				}
			}
		}

		return ancestor;
	}

	private boolean isDescendentOf(Bioentity ancestor, Bioentity gene) {
		List<Bioentity> children = ancestor.getChildren();
		boolean is_descendent = false;
		if (children != null) {
			if (children.contains(gene)) {
				is_descendent = true;
			} else {
				for (Iterator<Bioentity> it = children.iterator(); it.hasNext() && !is_descendent; ) {
					is_descendent = isDescendentOf(it.next(), gene);
				}
			}
		}
		return is_descendent;
	}

	public Protein getRoot() {
		return root;
	}

	public void setRoot(Protein node) {
		this.root = node;
	}
	
//	public List<Protein> getAllNodes() {
//		return getBioentities();
//	}
//
	public List<Protein> getTerminusNodes() {
		return terminusNodes;
	}

	public List<Bioentity> getCurrentNodes() {
		return currentNodes;
	}

	public Protein getCurrentRoot(){
		return root;
	}

	/**
	 * Method declaration
	 *
	 *
	 * @param node
	 * @param v
	 *
	 * @see
	 */
	 public void getDescendentList(Bioentity node, List<Protein> v){
		if (!node.isTerminus()) {
			List<Bioentity>  children = node.getChildren();
			for (int i = 0; i < children.size(); i++){
				Bioentity  child = children.get(i);
				v.add((Protein) child);
				getDescendentList(child, v);
			}
		}
	 }

	 public List<Protein> getLeaves() {
		 List<Protein> leaf_nodes = new ArrayList<Protein>();
		 getLeafDescendants(root, leaf_nodes);
		 return leaf_nodes;
	 }

	 public List<Protein> getLeafDescendants(Bioentity node) {
		 List<Protein> leaf_nodes = new ArrayList<Protein>();
		 getLeafDescendants(node, leaf_nodes);
		 return leaf_nodes;
	 }

	 /**
	  * Method declaration
	  *
	  *
	  * @param node
	  * @param leafList
	  *
	  * @see
	  */
	 public void getLeafDescendants(Bioentity node, List<Protein> leafList){
		 List<Bioentity>  children = node.getChildren();
		 if (children != null) {
			 for (Bioentity child : children) {
				 if (child.isLeaf() || child.isPruned()) {
					 leafList.add((Protein) child);
				 }
				 if (!child.isPruned())
					 getLeafDescendants(child, leafList);
			 }
		 }
	 }

	public boolean isRooted() {
		return rooted;
	}

	public void setRooted(boolean rooted) {
		this.rooted = rooted;
	}

	public boolean isRerootable() {
		return rerootable;
	}

	public void setRerootable(boolean rerootable) {
		this.rerootable = rerootable;
	}

	public String getDistanceUnit() {
		return distance_unit;
	}

	public void setDistanceUnit(String distance_unit) {
		this.distance_unit = distance_unit;
	}
}
