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

package org.bbop.paint.model;

import org.apache.log4j.Logger;
import owltools.gaf.Bioentity;
import owltools.gaf.BioentityDocument;

import java.io.Serializable;
import java.util.*;

public class Tree extends BioentityDocument implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger("Tree.class");

	private Bioentity root = null;
	private List<Bioentity> currentNodes = null; // only the nodes that are visible, as some may be collapsed or pruned away
	private List<Bioentity> terminusNodes = null; // only the terminal nodes (i.e. leaves, collapsed or pruned stubs)

	/*
	 * For ordering operations on the tree
	 */
	private Map<Bioentity, Integer> descendent_count;
	private int species_count;
	private Map<Bioentity, Integer> species_index;

	/**
	 * Constructor declaration
	 *
	 *
	 * @param id, node
	 *
	 * @see
	 */
	public Tree(String id, Bioentity node){
		super(id);
		if (null == node){
			return;
		}
		root = node;

		descendent_count = new HashMap<Bioentity, Integer>();
		species_index = new HashMap<Bioentity, Integer>();
		species_count = 0;
		initSortGuides(root);

		addChildNodesInOrder(root, bioentities);
		currentNodes = new ArrayList<Bioentity>();
		terminusNodes = new ArrayList<Bioentity>();
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
	private void addChildNodesInOrder(Bioentity node, List<Bioentity> node_list) {
		if (node != null) {
			if (node.isTerminus()) {
				node_list.add(node);
			} else {
				/* 
				 * Don't add the parent until the children above it have been added first
				 */
				List<Bioentity> topChildren = getTopChildren(node);
				for (Iterator<Bioentity> it = topChildren.iterator(); it.hasNext();) {
					addChildNodesInOrder(it.next(), node_list);
				}

				// Add the parent
				node_list.add(node);

				List<Bioentity> bottomChildren = getBottomChildren(node);
				for (Iterator<Bioentity> it = bottomChildren.iterator(); it.hasNext();) {
					addChildNodesInOrder(it.next(), node_list);
				}
			}
		}
	}

	/* 
	 * This is only called during initialization of a new family
	 */
	private int initSortGuides(Bioentity node) {
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
					count += initSortGuides(child);
				}
			}
			descendent_count.put(node, new Integer(count));
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
	List<Bioentity> getTopChildren(Bioentity dsn){
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
	List<Bioentity> getBottomChildren(Bioentity dsn){
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
	private List<Bioentity> getChildren(boolean top, Bioentity dsn){
		List<Bioentity>  children = dsn.getChildren();
		if (null == children){
			return null;
		}

		// Add remainder to handle case where there are an odd number of children
		int     half = children.size() / 2 + children.size() % 2;
		List<Bioentity>  returnList = new ArrayList<Bioentity>();

		if (top) {
			for (int i = 0; i < half; i++) {
				Bioentity node = children.get(i);
				if (!returnList.contains(node))
					returnList.add(node);
			}
		}
		else {
			for (int i = half; i < children.size(); i++) {
				Bioentity node = children.get(i);
				if (!returnList.contains(node))
					returnList.add(node);
			}
		}
		return returnList;
	}

	/**
	 * Gets the node which is currently displayed at the top of
	 * the list (i.e. the first row) for a given clade/ancestral node 
	 * @param node, where to start from
	 * @returns Bioentity
	 */
	Bioentity getTopLeafNode(Bioentity node) {
		Bioentity top_leaf = null;
		if (node != null) {
			if (!terminusNodes.contains(node) && node.getChildren() != null) {
				top_leaf = getTopLeafNode(node.getChildren().get(0));
			} else {
				top_leaf = node;
			}
		}
		return top_leaf;
	}

	Bioentity getBottomLeafNode(Bioentity node) {
		Bioentity bottom_leaf = null;
		if (node != null) {
			if (!terminusNodes.contains(node) && node.getChildren() != null) {
				List<Bioentity> children = node.getChildren();
				bottom_leaf = getBottomLeafNode(children.get(children.size() - 1));
			} else {
				bottom_leaf = node;
			}
		}
		return bottom_leaf;
	}


	// Method to set number of leaves in tree
	private void initCurrentNodes() {
		currentNodes.clear();
		terminusNodes.clear();
		addChildNodesInOrder(root, currentNodes);
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
				terminusNodes.add(node);
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

	protected Bioentity getMRCA(Bioentity gene1, Bioentity gene2) {
		Bioentity ancestor = null;
		if (gene1.isLeaf() && gene2.isLeaf()) {
			if (gene1 == gene2) {
				ancestor = gene1;
			} else {
				while (ancestor == null && gene1 != null) {
					Bioentity ancestor1 =  gene1.getParent();
					if (isDescendentOf(ancestor1, gene2)) {
						ancestor = ancestor1;
					} else {
						gene1 = ancestor1;
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

	public Bioentity getRoot() {
		return root;
	}

	protected List<Bioentity> getAllNodes() {
		return getBioentities();
	}

	public List<Bioentity> getTerminusNodes() {
		return terminusNodes;
	}

	protected List<Bioentity> getCurrentNodes() {
		return currentNodes;
	}

	protected Bioentity getCurrentRoot(){
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
	void getDescendentList(Bioentity node, List<Bioentity> v){
		if (!node.isTerminus()) {
			List<Bioentity>  children = node.getChildren();
			for (int i = 0; i < children.size(); i++){
				Bioentity  child = children.get(i);
				v.add(child);
				getDescendentList(child, v);
			}
		}
	}

	public List<Bioentity> getLeaves() {
		List<Bioentity> leaf_nodes = new ArrayList<Bioentity>();
		getLeafDescendants(root, leaf_nodes);
		return leaf_nodes;
	}
	
	public List<Bioentity> getLeafDescendants(Bioentity node) {
		List<Bioentity> leaf_nodes = new ArrayList<Bioentity>();
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
	private void getLeafDescendants(Bioentity node, List<Bioentity> leafList){
		if (null == node){
			return;
		}
		if (node.isLeaf() && !node.isPruned()){
			leafList.add(node);
		}
		else {
			List<Bioentity>  children = node.getChildren();
			for (int i = 0; (children != null && i < children.size()); i++){
				Bioentity  child = children.get(i);
				if (!child.isPruned())
					getLeafDescendants(child, leafList);
			}
		}
	}
}
