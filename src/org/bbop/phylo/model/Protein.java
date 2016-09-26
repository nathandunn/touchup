package org.bbop.phylo.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import owltools.gaf.Bioentity;

public class Protein extends Bioentity {

	private String sequence;
	private String description;

	public static final String NODE_TYPE_DUPLICATION="1>0";
	public static final String NODE_TYPE_HORIZONTAL_TRANSFER="0>0";
	public static final String NODE_TYPE_SPECIATION="0>1";

	private static Logger log = Logger.getLogger(Protein.class);

	public Protein(){
		super();
	}

	public Protein(String id, String symbol, String fullName, String typeCls,
			String ncbiTaxonId, String db) {
		super(id, symbol, fullName, typeCls, ncbiTaxonId, db);
	}

	public boolean isRoot() {
		return getParent() == null;
	}

	// Setter/Getter methods
	public void addChild(Protein child) {
		if (child != null) {
			List<Bioentity> current_children = getChildren();
			if (current_children == null) {
				current_children = new ArrayList<>();
			}
			current_children.add(child);
			super.setChildren(current_children);
		}
	}

	public Protein getChildNode(int i) {
		if (children != null && (i < children.size())) {
			return (Protein) children.get(i);
		} else {
			return null;
		}
	}

	public Protein getLastChildNode() {
		if (children != null && children.size() > 0) {
			return getChildNode(children.size() - 1);
		} else {
			return null;
		}
	}
	
	public void setDistanceToParent(double dist) {
		setDistanceFromParent((float) dist);
	}

	public double getDistanceToParent() {
		return (double) getDistanceFromParent();
	}

	public String getName() {
		return this.getId();
	}

	public void setName(String name) {
		this.setId(name);
	}

	public String getSequence() {
		return sequence;
	}

	public void setSequence(String sequence) {
		this.sequence = sequence;
	}

	public String getDescription() {
		if (!isLeaf() && description == null) {
			StringBuffer about_me = new StringBuffer();
			myChildren(this, about_me);
			description = about_me.toString();
		}
		if (description == null)
			description = "";
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	private void myChildren(Bioentity node, StringBuffer about_me) {
		List<Bioentity> children = node.getChildren();
		for (Bioentity child : children) {
			if (child.isLeaf()) {
				about_me.append(child.getDBID() + " ");
			} else {
				myChildren(child, about_me);
			}
		}
	}

    final public boolean isFirstChildNode() {
    	Bioentity parent = getParent();
    	if (parent != null && parent.getChildren().get(0).equals(this)) {
    		return true;
    	} else {
    		return false;
    	}
    }

    final public boolean isLastChildNode() {
    	Bioentity parent = getParent();
    	int i = parent.getChildren().size() - 1;
    	if (i >= 0 && parent.getChildren().get(i).equals(this)) {
    		return true;
    	} else {
    		return false;
    	}
    }

	public boolean isSpeciation() {
		if (null == getType()) {
			return false;
		}
		int index = getType().indexOf(NODE_TYPE_SPECIATION);
		return index >= 0;
	}

}
