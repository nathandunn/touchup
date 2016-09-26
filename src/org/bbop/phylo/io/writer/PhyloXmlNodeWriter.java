// $Id:
// FORESTER -- software libraries and applications
// for evolutionary biology research and applications.
//
// Copyright (C) 2000-2009 Christian M. Zmasek
// Copyright (C) 2007-2009 Burnham Institute for Medical Research
// All rights reserved
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
//
// Contact: phylosoft @ gmail . com
// WWW: https://sites.google.com/site/cmzmasek/home/software/forester

package org.bbop.phylo.io.writer;

import java.io.IOException;
import java.io.Writer;

import org.apache.log4j.Logger;
import org.bbop.phylo.io.PhylogenyDataUtil;
import org.bbop.phylo.io.phyloxml.PhyloXmlMapping;
import org.bbop.phylo.model.Protein;
import org.bbop.phylo.util.Constant;
import org.bbop.phylo.util.PhyloUtil;

import owltools.gaf.species.TaxonFinder;

public class PhyloXmlNodeWriter {
	
	private static Logger log = Logger.getLogger(PhyloXmlNodeWriter.class);


	public static void toPhyloXml( final Writer w, final Protein node, final int level, final String indentation )
			throws IOException {
		String ind = "";
		if ( ( indentation != null ) && ( indentation.length() > 0 ) ) {
			ind = indentation + PhylogenyWriter.PHYLO_XML_INTENDATION_BASE;
		}
		if ( !PhyloUtil.isEmpty( node.getId() ) ) {
			PhylogenyDataUtil.appendElement( w, PhyloXmlMapping.IDENTIFIER, node.getId(), indentation );
		}
		if ( !PhyloUtil.isEmpty( node.getPersistantNodeID() ) ) {
			PhylogenyDataUtil.appendElement( w, PhyloXmlMapping.NODE_NAME, node.getPersistantNodeID(), indentation );
		}
//		if ( node.getDistanceFromParent() != PhylogenyDataUtil.BRANCH_LENGTH_DEFAULT ) {
//			PhylogenyDataUtil.appendElement( w, PhyloXmlMapping.BRANCH_LENGTH, 
//					String.valueOf( PhyloUtil.round( node.getDistanceFromParent(), 
//							PhyloXmlUtil.ROUNDING_DIGITS_FOR_PHYLOXML_DOUBLE_OUTPUT ) ), indentation );
//		}
		if ( !PhyloUtil.isEmpty(node.getSeqId() ) && !node.getSeqDb().equals(Constant.PANTHER_DB)) {
			String element_indent = indentation + PhylogenyWriter.PHYLO_XML_INTENDATION_BASE;
			w.write( PhyloUtil.LINE_SEPARATOR );
			w.write( element_indent );
			PhylogenyDataUtil.appendOpen( w, PhyloXmlMapping.SEQUENCE, 
					PhyloXmlMapping.SEQUENCE_TYPE, "protein",
					PhyloXmlMapping.ACCESSION_SOURCE_ATTR, node.getSeqDb());
			if ( !PhyloUtil.isEmpty( node.getSymbol()) && !node.getSymbol().equals(node.getLocalId()) ) {
				PhylogenyDataUtil.appendElement( w, PhyloXmlMapping.SEQUENCE_SYMBOL, node.getSymbol(), element_indent );
			}
			PhylogenyDataUtil.appendElement( w, PhyloXmlMapping.ACCESSION, node.getSeqId(), element_indent );
			w.write( PhyloUtil.LINE_SEPARATOR );
			w.write( element_indent );
			PhylogenyDataUtil.appendClose( w, PhyloXmlMapping.SEQUENCE );
		}
		//            if ( !ForesterUtil.isEmpty( getLocation() ) ) {
		//                PhylogenyDataUtil.appendElement( w, PhyloXmlMapping.SEQUENCE_LOCATION, getLocation(), indentation );
		//            }
		//            if ( !ForesterUtil.isEmpty( getMolecularSequence() ) ) {
		//                PhylogenyDataUtil.appendElement( w,
		//                                                 PhyloXmlMapping.SEQUENCE_MOL_SEQ,
		//                                                 getMolecularSequence(),
		//                                                 PhyloXmlMapping.SEQUENCE_MOL_SEQ_ALIGNED_ATTR,
		//                                                 String.valueOf( isMolecularSequenceAligned() ),
		//                                                 indentation );
		//            }
		//            if ( ( getUris() != null ) && !getUris().isEmpty() ) {
		//                for( final Uri uri : getUris() ) {
		//                    if ( uri != null ) {
		//                        uri.toPhyloXML( writer, level, indentation );
		//                    }
		//                }
		//            }
		//            if ( ( getAnnotations() != null ) && !getAnnotations().isEmpty() ) {
		//                for( final PhylogenyData annotation : getAnnotations() ) {
		//                    annotation.toPhyloXML( writer, level, my_ind );
		//                }
		//            }
		//            if ( ( getCrossReferences() != null ) && !getCrossReferences().isEmpty() ) {
		//                writer.write( ForesterUtil.LINE_SEPARATOR );
		//                writer.write( my_ind );
		//                PhylogenyDataUtil.appendOpen( writer, PhyloXmlMapping.SEQUENCE_X_REFS );
		//                for( final PhylogenyData x : getCrossReferences() ) {
		//                    x.toPhyloXML( writer, level, my_ind );
		//                }
		//                writer.write( ForesterUtil.LINE_SEPARATOR );
		//                writer.write( my_ind );
		//                PhylogenyDataUtil.appendClose( writer, PhyloXmlMapping.SEQUENCE_X_REFS );
		//            }
		//            if ( getDomainArchitecture() != null ) {
		//                getDomainArchitecture().toPhyloXML( writer, level, my_ind );
		String ncbi_code = node.getNcbiTaxonId();
		if ( ncbi_code != null && !ncbi_code.endsWith(":1")  && ncbi_code.startsWith(TaxonFinder.TAXON_PREFIX)) {
			String element_indent = indentation + PhylogenyWriter.PHYLO_XML_INTENDATION_BASE;
			w.write( PhyloUtil.LINE_SEPARATOR );
			w.write( element_indent );
			if (ncbi_code.equals("1")) {
				log.info("Stop right here");
			}
			PhylogenyDataUtil.appendOpen( w, PhyloXmlMapping.TAXONOMY );
			String taxon = ncbi_code.substring(TaxonFinder.TAXON_PREFIX.length());
			PhylogenyDataUtil.appendElement( w, PhyloXmlMapping.IDENTIFIER, taxon, 
					PhyloXmlMapping.IDENTIFIER_PROVIDER_ATTR, "ncbi",
					element_indent );
			String name = TaxonFinder.getSpecies(ncbi_code);
			if ( !PhyloUtil.isEmpty( name ) ) {
				PhylogenyDataUtil.appendElement( w,
						PhyloXmlMapping.TAXONOMY_SCIENTIFIC_NAME,
						name,
						element_indent );
			}
	        w.write( PhyloUtil.LINE_SEPARATOR );
	        w.write( element_indent );
	        PhylogenyDataUtil.appendClose( w, PhyloXmlMapping.TAXONOMY );
		}

		if ( !PhyloUtil.isEmpty(node.getType() )) {
			String element_indent = indentation + PhylogenyWriter.PHYLO_XML_INTENDATION_BASE;
			String node_type = node.getType();
	        w.write( PhyloUtil.LINE_SEPARATOR );
	        w.write( element_indent );
	        PhylogenyDataUtil.appendOpen( w, PhyloXmlMapping.EVENTS );
	        if ( ( node.isDuplication() ) ) {
	            PhylogenyDataUtil.appendElement( w, PhyloXmlMapping.EVENT_TYPE, "D", element_indent );
	        }
	        else if ( ( node.isSpeciation()) ) {
	            PhylogenyDataUtil.appendElement( w, PhyloXmlMapping.EVENT_TYPE, "S", element_indent );
	        }
	        else if ( ( node.isHorizontalTransfer() ) ) {
	            PhylogenyDataUtil.appendElement( w, PhyloXmlMapping.EVENT_TYPE, "H", indentation );
	        }
	        else {
	        	log.info("What is this? " + node_type);
	        }
	        w.write( PhyloUtil.LINE_SEPARATOR );
	        w.write( element_indent );
	        PhylogenyDataUtil.appendClose( w, PhyloXmlMapping.EVENTS );

		}
//		if ( isHasBinaryCharacters() ) {
//			getBinaryCharacters().toPhyloXML( writer, level, indentation );
//		}
//		if ( isHasDistribution() ) {
//			for( final Distribution d : getDistributions() ) {
//				d.toPhyloXML( writer, level, indentation );
//			}
//		}
//		if ( isHasDate() ) {
//			getDate().toPhyloXML( writer, level, indentation );
//		}
//		if ( isHasReference() ) {
//			for( final Reference r : getReferences() ) {
//				r.toPhyloXML( writer, level, indentation );
//			}
//		}
//		if ( isHasProperties() ) {
//			getProperties().toPhyloXML( writer, level, indentation.substring( 0, indentation.length() - 2 ) );
//		}
//		if ( ( level == 0 ) && ( getNodeVisualData() != null ) && !getNodeVisualData().isEmpty() ) {
//			getNodeVisualData().toPhyloXML( writer, level, indentation.substring( 0, indentation.length() - 2 ) );
//		}
//		if ( ( getVector() != null )
//				&& !getVector().isEmpty()
//				&& ( ( getProperties() == null ) || getProperties()
//						.getPropertiesWithGivenReferencePrefix( PhyloXmlUtil.VECTOR_PROPERTY_REF ).isEmpty() ) ) {
//			final List<Property> ps = vectorToProperties( getVector() );
//			final String my_indent = indentation.substring( 0, indentation.length() - 2 );
//			for( final Property p : ps ) {
//				p.toPhyloXML( writer, level, my_indent );
//			}
//		}
//	}

}
}
