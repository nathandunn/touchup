package org.bbop.phylo.gaf;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bbop.phylo.annotate.AnnotationUtil;
import org.bbop.phylo.annotate.WithEvidence;
import org.bbop.phylo.model.Family;
import org.bbop.phylo.model.Tree;
import org.bbop.phylo.panther.IDmap;
import org.bbop.phylo.panther.ParsingHack;
import org.bbop.phylo.tracking.LogAction;
import org.bbop.phylo.tracking.LogEntry;
import org.bbop.phylo.tracking.LogUtil;
import org.bbop.phylo.util.Constant;
import org.bbop.phylo.util.FileUtil;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.io.GafWriter;

public class GafRecorder {

	private static final Logger log = Logger.getLogger(GafRecorder.class);

	private GafRecorder() {
	}

	public static void record(Family family, File family_dir, String comment) {
		String family_name = family.getFamily_name();
		boolean ok = FileUtil.validPath(family_dir);
		if (ok) {
			File gaf_file = new File(family_dir, family_name + Constant.GAF_SUFFIX);
			Tree tree = family.getTree();
			GafDocument gaf_doc = new GafDocument(gaf_file.getAbsolutePath(), family_dir.getAbsolutePath());
			List<String> comments = family.getGafComments();
			if (comments != null) {
				for (String comment_line : comments) {
					gaf_doc.addComment(comment_line);
				}
			}
			gaf_doc.addComment(comment + " on " + LogUtil.dateNow());
			Map<Bioentity, String> originalIDs = new HashMap<>();
			addAnnotations(family, tree, tree.getRoot(), gaf_doc, originalIDs);
			GafWriter gaf_writer = new GafWriter();
			gaf_writer.setStream(gaf_file);
			gaf_writer.write(gaf_doc);
			IOUtils.closeQuietly(gaf_writer);
			Set<Bioentity> modified = originalIDs.keySet();
			for (Bioentity modified_node : modified) {
				String originalID = originalIDs.get(modified_node);
				// restore it
				modified_node.setId(originalID);
				modified_node.setDb(originalID.substring(0, originalID.indexOf(':')));
				List<GeneAnnotation> annotations = modified_node.getAnnotations();
				if (annotations != null) {
					for (GeneAnnotation annotation : annotations) {
						annotation.setBioentity(modified_node.getId());
					}
				}
			}
			log.info("Wrote updated GAF to " + gaf_file);
		} else {
			log.error("Unable to save GAF file for " + family_name + " in " + family_dir);
		}
	}

	private static void addAnnotations(Family family, Tree tree, Bioentity node, GafDocument gaf_doc, Map<Bioentity, String> originalIDs) {
		if (node.isPruned()) {
			/* Write out one row to record the pruned branch */
			GeneAnnotation stump = createStump(family, node);
			gaf_doc.addGeneAnnotation(stump);
		}
		else {
			List<GeneAnnotation> annotations = node.getAnnotations();
			boolean use_seq = ParsingHack.useUniProtID(node.getDb());
			if (annotations != null) {
				for (GeneAnnotation annotation : annotations) {
					/**
					 * Only save those associations made within the context of PAINT
					 */
					boolean include = AnnotationUtil.isPAINTAnnotation(annotation);
					include &= annotation.isMRC() || node.isLeaf() || annotation.isDirectNot();
					if (include) {
						if (node.isLeaf()) {
							// This is logically not needed, but is a convenience for PanTree
							addExpWith(tree, node, annotation);
							if (use_seq) {
								originalIDs.put(node, node.getId());
								node.setId(node.getSeqDb() + ':' + node.getSeqId());
								node.setDb(node.getSeqDb());
								annotation.setBioentity(node.getSeqDb() + ':' + node.getSeqId());
							}
						}
						if (annotation.getWithInfos().size() == 0) {
							log.error("No with information for\n\t" + annotation);
						} else {
							gaf_doc.addGeneAnnotation(annotation);
						}
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
						addAnnotations(family, tree, child, gaf_doc, originalIDs);
					}
				}
			}
		}
	}

	private static void addExpWith(Tree tree, Bioentity node, GeneAnnotation annotation) {
		List<String> withs = new ArrayList<>();
		String ancestor_id = annotation.getWithInfos().iterator().next();
		withs.add(ancestor_id);

		/*
		 * Seems to just be the one ancestral node, so proceed
		 * First indicate the ancestral node
		 */

		/*
		 * Then add the ancestor's withs to feed back to PANTREE
		 */
		List<String> exp_withs;
		List<Bioentity> ancestor = IDmap.inst().getGeneByDbId(ancestor_id);
		if (ancestor != null) {
			if (ancestor.size() > 1) {
				log.debug("More than one ancestor? " + ancestor_id);
			}
			for (Bioentity a : ancestor) {
				WithEvidence evidence = new WithEvidence(tree, a, annotation.getCls());
				exp_withs = evidence.getExpWiths();
				withs.addAll(exp_withs);
			}
		} else {
			log.error("Where is the ancestral node for " + node + " to inherit " + annotation.getCls() + '?');
		}
		annotation.setWithInfos(withs);
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
