package org.bbop.phylo.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Constant {

	public static final String BP_ID = "GO:0008150";
	public static final String CC_ID = "GO:0005575";
	public static final String MF_ID = "GO:0003674";

	public static final String MF = "F";
	public static final String CC = "C";
	public static final String BP = "P";
	
	public static final String NOT = "NOT";
	public static final String CUT = "CUT";

	public static final String TREE_SUFFIX = ".tree";
	public static final String ATTR_SUFFIX = ".attr";
	public static final String TAB_SUFFIX = ".tab";
	public static final String MIA_SUFFIX = ".mia";
	public static final String MSA_SUFFIX = ".msa";
	public static final String WTS_SUFFIX = ".wts";
	public static final String GAF_SUFFIX = ".gaf";
	public static final String LOG_SUFFIX = ".txt";
	public static final String OLDLOG_SUFFIX = ".txt";
	public static final String EXP_SUFFIX = ".exp";
	public static final String QUESTIONED_SUFFIX = "-questioned.gaf";
	public static final String CHALLENGED_SUFFIX = "-disputed.gaf";


	private final static String NOT_KEY_RESIDUES = "NOT due to change in key residue(s)";
	public final static String KEY_RESIDUES_EC = "IKR";
	private final static String NOT_DIVERGENT = "NOT due to rapid divergence";
	public final static String DIVERGENT_EC = "IRD";
	private final static String NOT_DESCENDANT_SEQUENCES = "NOT due to descendant sequence(s)";
	public final static String DESCENDANT_EVIDENCE_CODE = "IBD"; // was IDS
	public final static String OLD_DESCENDANT_EVIDENCE_CODE = "IDS"; // replaced by IBD
	public static final String ANCESTRAL_EVIDENCE_CODE = "IBA"; // was IAS
	public static final String OLD_ANCESTRAL_EVIDENCE_CODE = "IAS"; // replaced by IBA
	public static final String LOSS_OF_FUNCTION = "LOF";
	public final static String PANTHER_DB = "PANTHER";
	public final static String PAINT_REF = "PAINT_REF";
	public final static String OLD_SOURCE = "RefGenome";
	public final static String PAINT_AS_SOURCE = "GO_Central";
    public final static String REACTOME = "Reactome";
	public static final String CURATOR_EVIDENCE_CODE = "IC";

	private final static String CONTRIBUTES = "contributes_to";
	private final static String COLOCATES = "colocalizes_with";
	private final static String INTEGRAL_TO = "integral_to";
	
	public final static String STR_EMPTY = "";
	public static final String DELIM = ",();";
	public static final String PIPE = "\\|";

	public static final String SEMI_COLON = ";";
	public static final String OPEN_PAREN = "(";
	public static final String CLOSE_PAREN = ")";
	public static final String COMMA = ",";
	public static final String COLON = ":";
	public static final String OPEN_BRACKET = "[";
	public static final String CLOSE_BRACKET = "]";
	public static final String NEWLINE = "\n";
	public static final String TAB = "\t";
	public static final String SPACE = " ";
	public static final String PLUS = "+";
	public static final String EQUAL = "=";

	public static final String NODE_TYPE_ANNOTATION = "ID=";
	public static final int NODE_TYPE_ANNOTATION_LENGTH = NODE_TYPE_ANNOTATION.length();

	private final static ArrayList<String> not_quals = new ArrayList<String> ();
	static {
		not_quals.add(NOT_KEY_RESIDUES);
		not_quals.add(NOT_DIVERGENT);
		not_quals.add(NOT_DESCENDANT_SEQUENCES);
	}

	public static final String[] Not_Strings = { 
		NOT_DIVERGENT, 
		NOT_KEY_RESIDUES
	};

	public static final String[] Not_Strings_Ext = {
		NOT_DIVERGENT,
		NOT_KEY_RESIDUES,
		NOT_DESCENDANT_SEQUENCES
	};

	public static final String[] Qual_Strings = { 
		CONTRIBUTES,
		COLOCATES,
		INTEGRAL_TO
	};

	public static final Map<String, String> NOT_QUALIFIERS_TO_EVIDENCE_CODES = new HashMap<String, String>();
	static {
		NOT_QUALIFIERS_TO_EVIDENCE_CODES.put(NOT_DIVERGENT, DIVERGENT_EC);
		NOT_QUALIFIERS_TO_EVIDENCE_CODES.put(NOT_DESCENDANT_SEQUENCES, DESCENDANT_EVIDENCE_CODE);
		NOT_QUALIFIERS_TO_EVIDENCE_CODES.put(NOT_KEY_RESIDUES, KEY_RESIDUES_EC);
	}

	private static final Map<String, String> NOT_EVIDENCE_CODES_TO_QUALIFIERS = new HashMap<String, String>();
	static {
		NOT_EVIDENCE_CODES_TO_QUALIFIERS.put(DIVERGENT_EC, NOT_DIVERGENT);
		NOT_EVIDENCE_CODES_TO_QUALIFIERS.put(DESCENDANT_EVIDENCE_CODE, NOT_DESCENDANT_SEQUENCES);
		NOT_EVIDENCE_CODES_TO_QUALIFIERS.put(KEY_RESIDUES_EC, NOT_KEY_RESIDUES);
	}

	public static final HashSet<String> EXP_strings = new HashSet<String>();
	static { 
		EXP_strings.add("EXP");
		EXP_strings.add("IDA");
		EXP_strings.add("IEP");
		EXP_strings.add("IGI");
		EXP_strings.add("IMP");
		EXP_strings.add("IPI");
		EXP_strings.add("IKR");
		EXP_strings.add("IRD");
		EXP_strings.add("TAS");
	}

	public final static String GO_REF_TITLE = "Annotation inferences using phylogenetic trees";
	public final static String GO_REF_SW = "PAINT (Phylogenetic Annotation and INference Tool).";
	public final static String GO_PUBLICATION = GO_REF_TITLE + "\n"
		+ "The goal of the GO Reference Genome Project, described in PMID 19578431, "
		+ "is to provide accurate, complete and consistent GO annotations for all genes in twelve model organism genomes. "
		+ "To this end, GO curators are annotating evolutionary trees from the PANTHER database with GO terms "
		+ "describing molecular function, biological process and cellular component. "
		+ "GO terms based on experimental data from the scientific literature are used "
		+ "to annotate ancestral genes in the phylogenetic tree by sequence similarity (ISS), "
		+ "and unannotated descendants of these ancestral genes are inferred to have inherited these same GO annotations by descent. "
		+ "The annotations are done using a tool called " + GO_REF_SW;

	public static final String PROTEIN = "protein";
	
	public static final String DEV_GOLR = "http://golr.berkeleybop.org";
//	public static final String DEV_GOLR = "http://toaster.lbl.gov:9000/solr";
	public static final String PUB_GOLR = "http://golr.geneontology.org/solr";
	public static final String PANTHER_VERSION = "PANTHER 10.0 built on the 2014 GCRP release";
	
}
