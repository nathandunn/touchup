package org.bbop.phylo.gaf;

import org.apache.log4j.Logger;
import org.bbop.phylo.tracking.LogAction;
import org.bbop.phylo.tracking.LogEntry;
import org.bbop.phylo.annotate.WithEvidence;
import org.bbop.phylo.model.Family;
import org.bbop.phylo.model.Tree;
import org.bbop.phylo.panther.IDmap;
import org.bbop.phylo.touchup.Constant;
import org.bbop.phylo.touchup.Preferences;
import org.bbop.phylo.util.FileUtil;
import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.io.GafWriter;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class GafRecorder {

	private static final Logger log = Logger.getLogger(GafRecorder.class);

	private GafRecorder() {
	}

	public static void record(Family family) {
		String family_name = family.getFamily_name();
		String family_dir = Preferences.inst().getGafDir() + family_name + File.separator;
		boolean ok = FileUtil.validPath(family_dir);
		if (ok) {
			String gaf_file = family_dir + family_name + Constant.GAF_SUFFIX;
			Tree tree = family.getTree();
			GafDocument gaf_doc = new GafDocument(gaf_file, family_dir);
			addAnnotations(family, tree, tree.getRoot(), gaf_doc);
			GafWriter gaf_writer = new GafWriter();
			gaf_writer.setStream(gaf_file);
			gaf_writer.write(gaf_doc);
			try {
				gaf_writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void addAnnotations(Family family, Tree tree, Bioentity node, GafDocument gaf_doc) {
		if (node.isPruned()) {
				/* Write out one row to record the pruned branch */
			GeneAnnotation stump = createStump(family, node);
			gaf_doc.addGeneAnnotation(stump);
		}
		else {
			List<GeneAnnotation> annotations = node.getAnnotations();
			if (annotations != null) {
				for (GeneAnnotation annotation : annotations) {
					String assigned_by = annotation.getAssignedBy();
					/**
					 * Only save those associations made within the context of PAINT
					 */
					boolean include = assigned_by.equals(Constant.PAINT_AS_SOURCE) || assigned_by.equals(Constant.OLD_SOURCE);
					include &= annotation.isMRC() || node.isLeaf() || annotation.isDirectNot();
					if (include) {
						if (node.isLeaf()) {
							// This is logically not needed, but is a convenience for PanTree
							addExpWith(tree, node, annotation);
						}
						gaf_doc.addGeneAnnotation(annotation);
					}
				}
			}
		/*
		 * And all the children as well, if this branch hasn't been pruned
		 */
			if (!node.isPruned()) {
				List<Bioentity> children = node.getChildren();
				if (children != null) {
					for (Bioentity child : children) {
						addAnnotations(family, tree, child, gaf_doc);
					}
				}
			}
		}
	}

	private static void addExpWith(Tree tree, Bioentity node, GeneAnnotation annotation) {
		Collection<String> withs = annotation.getWithInfos();
		if (withs.size() == 1) {
			String ancestor_id = withs.iterator().next();
			/*
			 * Seems to just be the one ancestral node, so proceed
			 * First indicate the ancestral node
			 */

			/*
			 * Then add the ancestor's withs to feed back to PANTREE
			 */
			Bioentity ancestor = IDmap.inst().getGeneByDbId(ancestor_id);
			if (ancestor != null) {
				WithEvidence evidence = new WithEvidence(tree, ancestor, annotation.getCls());
				List<String> exp_withs = evidence.getExpWiths();
				annotation.setWithInfos(exp_withs);
			} else {
				log.debug("Where is the ancestral node for " + node + " to inherit " + annotation.getCls() + '?');
			}
		} else {
			log.debug("Why not a single piece of evidence for " + node + " to " + annotation.getCls() + '?');
		}
	}

	private static GeneAnnotation createStump(Family family, Bioentity node) {
		GeneAnnotation stump = new GeneAnnotation();
		stump.setBioentity(node.getId());
		stump.setBioentityObject(node);
		stump.setIsCut(true);
		stump.addReferenceId(family.getReference());
		stump.setAssignedBy(Constant.PAINT_AS_SOURCE);
		LogEntry log_entry = LogAction.findEntry(node, null, LogEntry.LOG_ENTRY_TYPE.PRUNE);
		String date = log_entry.getDate();
		stump.setLastUpdateDate(date);
		return stump;
	}
}
