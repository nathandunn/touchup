package org.bbop.phylo.gaf.parser;

interface AnnotationParserMessages {

	void fireParsingError(String message);
	
	void fireParsingWarning(String message);
}
