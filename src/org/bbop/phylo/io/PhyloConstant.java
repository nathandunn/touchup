package org.bbop.phylo.io;

public class PhyloConstant {
	
    public final static String  FORESTER_VERSION            = "1.043";
    public final static String  FORESTER_DATE               = "160817";
    public final static String  PHYLO_XML_VERSION           = "1.20";
    public final static String  PHYLO_XML_LOCATION          = "http://www.phyloxml.org";
    public final static String  PHYLO_XML_XSD               = "phyloxml.xsd";
    public final static String  XML_SCHEMA_INSTANCE         = "http://www.w3.org/2001/XMLSchema-instance";
    public final static String  LOCAL_PHYLOXML_XSD_RESOURCE = "resources/phyloxml.xsd";
    public final static String  PHYLO_XML_SUFFIX            = ".xml";
    public final static String  UTF_8 = "UTF-8";
    public final static String  ISO_8859_1 = "ISO-8859-1";
    public final static String  PHYLO_XML_REFERENCE         = "Han MV and Zmasek CM (2009): \"phyloXML: XML for evolutionary biology and comparative genomics\", BMC Bioinformatics 10:356";
    public final static boolean RELEASE                     = false;

    public enum PhylogeneticTreeFormats {
        NH, NHX, NEXUS, PHYLOXML
    }
    
    public enum NH_CONVERSION_SUPPORT_VALUE_STYLE {
        AS_INTERNAL_NODE_NAMES, IN_SQUARE_BRACKETS, NONE;
    }

}
