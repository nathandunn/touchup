package org.bbop.paint.touchup;

public class Report {

	private StringBuffer mf_annotations;
	private StringBuffer cc_annotations;
	private StringBuffer bp_annotations;
	private StringBuffer obsolete_terms;
	private StringBuffer lost_ancestors;
	private StringBuffer lost_annotations;
	private StringBuffer pruned_ancestors;

	public Report () {
		mf_annotations = new StringBuffer();
		cc_annotations = new StringBuffer();
		bp_annotations = new StringBuffer();
		obsolete_terms = new StringBuffer();
		lost_ancestors = new StringBuffer();
		lost_annotations = new StringBuffer();
		pruned_ancestors = new StringBuffer();
	}
}
