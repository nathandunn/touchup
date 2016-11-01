// $Id:
// FORESTER -- software libraries and applications
// for evolutionary biology research and applications.
//
// Copyright (C) 2008-2009 Christian M. Zmasek
// Copyright (C) 2008-2009 Burnham Institute for Medical Research
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

package org.bbop.phylo.io.phyloxml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bbop.phylo.io.PhyloConstant;
import org.bbop.phylo.io.PhylogenyDataUtil;
import org.bbop.phylo.io.PhylogenyParserException;
import org.bbop.phylo.model.Bioentity;
import org.bbop.phylo.model.Family;
import org.bbop.phylo.model.Tree;
import org.bbop.phylo.util.PhyloUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public final class PhyloXmlHandler extends DefaultHandler {

	private static final String                         PHYLOXML               = "phyloxml";
	private String                                      _current_element_name;
	private Tree                                        _current_phylogeny;
	private List<Tree>                                  _phylogenies;
	private XmlElement                                  _current_xml_element;
	private Bioentity                                     _current_node;
	private static Map<Tree, HashMap<String, String>> phylogenySequencesById = new HashMap<Tree, HashMap<String, String>>();

	PhyloXmlHandler() {
		// Constructor.
	}

	private void addNode() {
		Bioentity new_node = new Bioentity();
		getCurrentNode().addChild( new_node );
		setCurrentNode( new_node );
	}

	@Override
	public void characters( final char[] chars, final int start_index, final int end_index ) {
		if ( ( ( getCurrentXmlElement() != null ) && ( getCurrentElementName() != null ) )
				&& !getCurrentElementName().equals( PhyloXmlMapping.CLADE )
				&& !getCurrentElementName().equals( PhyloXmlMapping.PHYLOGENY ) ) {
			if ( !PhyloUtil.isEmpty( getCurrentXmlElement().getValueAsString() ) ) {
				getCurrentXmlElement().appendValue( new String( chars, start_index, end_index ) );
			}
			else {
				getCurrentXmlElement().setValue( new String( chars, start_index, end_index ) );
			}
		}
	}

	@Override
	public void endElement( final String namespace_uri, final String local_name, final String qualified_name )
			throws SAXException {
		if ( PhyloUtil.isEmpty( namespace_uri ) || namespace_uri.startsWith( PhyloConstant.PHYLO_XML_LOCATION ) ) {
			if ( local_name.equals( PhyloXmlMapping.CLADE ) ) {
				try {
					mapElementToBioentity( getCurrentXmlElement(), getCurrentNode() );
					if ( !getCurrentNode().isRoot() ) {
						setCurrentNode( getCurrentNode().getParent() );
					}
					getCurrentXmlElement().setValue( null );
					setCurrentXmlElement( getCurrentXmlElement().getParent() );
				}
				catch ( final PhylogenyParserException ex ) {
					throw new SAXException( ex.getMessage() );
				}
				catch ( final PhyloXmlDataFormatException e ) {
					throw new SAXException( e.getMessage() );
				}
			}
			else if ( local_name.equals( PhyloXmlMapping.SEQUENCE_RELATION ) ) {
				//				try {
				if ( getCurrentTree() != null ) {
					//                        final SequenceRelation seqRelation = ( SequenceRelation ) SequenceRelationParser
					//                                .getInstance( getCurrentTree() ).parse( getCurrentXmlElement() );
					//                        final Map<String, String> sequencesById = getSequenceMapByIdForTree( getCurrentTree() );
					//                        final String ref0 = sequencesById.get( seqRelation.getRef0().getSourceId() ), ref1 = sequencesById
					//                                .get( seqRelation.getRef1().getSourceId() );
					//                        if ( ref0 != null ) {
					//                            // check for reverse relation
					//                            boolean fFoundReverse = false;
					//                            for( final SequenceRelation sr : ref0.getSequenceRelations() ) {
					//                                if ( sr.getType().equals( seqRelation.getType() )
					//                                        && ( ( sr.getRef0().isEqual( ref1 ) && sr.getRef1().isEqual( ref0 ) ) || ( sr
					//                                                .getRef0().isEqual( ref0 ) && sr.getRef1().isEqual( ref1 ) ) ) ) {
					//                                    // in this case we don't need to re-add it, but we make sure we don't loose the confidence value
					//                                    fFoundReverse = true;
					//                                    if ( ( sr.getConfidence() == null ) && ( seqRelation.getConfidence() != null ) ) {
					//                                        sr.setConfidence( seqRelation.getConfidence() );
					//                                    }
					//                                }
					//                            }
					//                            if ( !fFoundReverse ) {
					//                                ref0.addSequenceRelation( seqRelation );
					//                            }
				}
				//                        if ( ref1 != null ) {
				//                            // check for reverse relation
				//                            boolean fFoundReverse = false;
				//                            for( final SequenceRelation sr : ref1.getSequenceRelations() ) {
				//                                if ( sr.getType().equals( seqRelation.getType() )
				//                                        && ( ( sr.getRef0().isEqual( ref1 ) && sr.getRef1().isEqual( ref0 ) ) || ( sr
				//                                                .getRef0().isEqual( ref0 ) && sr.getRef1().isEqual( ref1 ) ) ) ) {
				//                                    // in this case we don't need to re-add it, but we make sure we don't loose the confidence value
				//                                    fFoundReverse = true;
				//                                    if ( ( sr.getConfidence() == null ) && ( seqRelation.getConfidence() != null ) ) {
				//                                        sr.setConfidence( seqRelation.getConfidence() );
				//                                    }
				//                                }
				//                            }
				//                            if ( !fFoundReverse ) {
				//                                ref1.addSequenceRelation( seqRelation );
				//                            }
				//                        }
				// we add the type to the current phylogeny so we can know it needs to be displayed in the combo
				//                        final Collection<SEQUENCE_RELATION_TYPE> relationTypesForCurrentTree = getCurrentTree()
				//                                .getRelevantSequenceRelationTypes();
				//                        if ( !relationTypesForCurrentTree.contains( seqRelation.getType() ) ) {
				//                            relationTypesForCurrentTree.add( seqRelation.getType() );
				//                        }
			}
			//				catch ( final PhyloXmlDataFormatException ex ) {
			//					throw new SAXException( ex.getMessage() );
			//				}
		}
		else if ( local_name.equals( PhyloXmlMapping.PHYLOGENY ) ) {
			//				try {
			//					//                    PhyloXmlHandler.mapElementToTree( getCurrentXmlElement(), getCurrentTree() );
			//				}
			//				catch ( final PhylogenyParserException e ) {
			//					throw new SAXException( e.getMessage() );
			//				}
			//				catch ( final PhyloXmlDataFormatException e ) {
			//					throw new SAXException( e.getMessage() );
			//				}
			finishTree();
			reset();
		}
		else if ( local_name.equals( PHYLOXML ) ) {
			// Do nothing.
		}
		else if ( ( getCurrentTree() != null ) && ( getCurrentXmlElement().getParent() != null ) ) {
			setCurrentXmlElement( getCurrentXmlElement().getParent() );
		}
		setCurrentElementName( null );
	}

	private void finishTree() throws SAXException {
		//        getCurrentTree().recalculateNumberOfExternalDescendants( false );
		getPhylogenies().add( getCurrentTree() );
		final HashMap<String, String> phyloSequences = phylogenySequencesById.get( getCurrentTree() );
		if ( phyloSequences != null ) {
			//            getCurrentTree().setSequenceRelationQueries( phyloSequences.values() );
			phylogenySequencesById.remove( getCurrentTree() );
		}
	}

	private String getCurrentElementName() {
		return _current_element_name;
	}

	private Bioentity getCurrentNode() {
		return _current_node;
	}

	private Tree getCurrentTree() {
		return _current_phylogeny;
	}

	private XmlElement getCurrentXmlElement() {
		return _current_xml_element;
	}

	List<Tree> getPhylogenies() {
		return _phylogenies;
	}

	private void init() {
		reset();
		setPhylogenies( new ArrayList<Tree>() );
	}

	private void initCurrentNode() {
		if ( getCurrentNode() != null ) {
			throw new FailedConditionCheckException( "attempt to create new current node when current node already exists" );
		}
		if ( getCurrentTree() == null ) {
			throw new FailedConditionCheckException( "attempt to create new current node for non-existing phylogeny" );
		}
		final Bioentity node = new Bioentity();
		getCurrentTree().setRoot( node );
		setCurrentNode( getCurrentTree().getRoot() );
	}

	private void mapElementToBioentity( final XmlElement xml_element, final Bioentity node )
			throws PhylogenyParserException, PhyloXmlDataFormatException {
		if ( xml_element.isHasAttribute( PhyloXmlMapping.BRANCH_LENGTH ) ) {
			float d = 0;
			try {
				d = Float.parseFloat( xml_element.getAttribute( PhyloXmlMapping.BRANCH_LENGTH ) );
			}
			catch ( final NumberFormatException e ) {
				throw new PhylogenyParserException( "ill formatted distance in clade attribute ["
						+ xml_element.getAttribute( PhyloXmlMapping.BRANCH_LENGTH ) + "]: " + e.getMessage() );
			}
			node.setDistanceFromParent( d );
		}
		if ( xml_element.isHasAttribute( PhyloXmlMapping.NODE_COLLAPSE ) ) {
			final String collapse_str = xml_element.getAttribute( PhyloXmlMapping.NODE_COLLAPSE );
			if ( !PhyloUtil.isEmpty( collapse_str ) && collapse_str.trim().equalsIgnoreCase( "true" ) ) {
				//                node.setCollapse( true );
			}
		}
		for( int i = 0; i < xml_element.getNumberOfChildElements(); ++i ) {
			final XmlElement element = xml_element.getChildElement( i );
			final String qualified_name = element.getQualifiedName();
			if ( qualified_name.equals( PhyloXmlMapping.BRANCH_LENGTH ) ) {
				if ( node.getDistanceFromParent() != PhylogenyDataUtil.BRANCH_LENGTH_DEFAULT ) {
					throw new PhylogenyParserException( "ill advised attempt to set distance twice for the same clade (probably via element and via attribute)" );
				}
				node.setDistanceFromParent( element.getValueAsFloat() );
			}
			if ( qualified_name.equals( PhyloXmlMapping.NODE_NAME ) ) {
				node.setId( element.getValueAsString() );
			}
			//  else if ( qualified_name.equals( PhyloXmlMapping.NODE_IDENTIFIER ) ) {
			//      node.getNodeData().setNodeIdentifier( ( Identifier ) IdentifierParser.getInstance().parse( element ) );
			//  }
			else if ( qualified_name.equals( PhyloXmlMapping.TAXONOMY ) ) {
				//                node.getNodeData().addTaxonomy( ( Taxonomy ) TaxonomyParser.getInstance().parse( element ) );
			}
			else if ( qualified_name.equals( PhyloXmlMapping.SEQUENCE ) ) {
				final String sequence = element.getValueAsString();
				node.setSequence( sequence );
				// we temporarily store all sequences that have a source ID so we can access them easily when we need to attach relations to them
				//                final String sourceId = sequence.getSourceId();
				//                if ( ( getCurrentTree() != null ) && !PhyloUtil.isEmpty( sourceId ) ) {
				//                    getSequenceMapByIdForPhylogeny( getCurrentTree() ).put( sourceId, sequence );
			}
		}
		//            else if ( qualified_name.equals( PhyloXmlMapping.DISTRIBUTION ) ) {
		////                node.getNodeData().addDistribution( ( Distribution ) DistributionParser.getInstance().parse( element ) );
		//            }
		//            else if ( qualified_name.equals( PhyloXmlMapping.CLADE_DATE ) ) {
		//                node.getNodeData().setDate( ( Date ) DateParser.getInstance().parse( element ) );
		//            }
		//            else if ( qualified_name.equals( PhyloXmlMapping.REFERENCE ) ) {
		//                node.getNodeData().addReference( ( Reference ) ReferenceParser.getInstance().parse( element ) );
		//            }
		//            else if ( qualified_name.equals( PhyloXmlMapping.BINARY_CHARACTERS ) ) {
		//                node.getNodeData().setBinaryCharacters( ( BinaryCharacters ) BinaryCharactersParser.getInstance()
		//                                                        .parse( element ) );
		//            }
		//            else if ( qualified_name.equals( PhyloXmlMapping.COLOR ) ) {
		//                node.getBranchData().setBranchColor( ( BranchColor ) ColorParser.getInstance().parse( element ) );
		//            }
		//            else if ( qualified_name.equals( PhyloXmlMapping.CONFIDENCE ) ) {
		//                node.getBranchData().addConfidence( ( Confidence ) ConfidenceParser.getInstance().parse( element ) );
		//            }
		//            else if ( qualified_name.equals( PhyloXmlMapping.WIDTH ) ) {
		//                node.getBranchData().setBranchWidth( ( BranchWidth ) BranchWidthParser.getInstance().parse( element ) );
		//            }
		//            else if ( qualified_name.equals( PhyloXmlMapping.EVENTS ) ) {
		//                node.getNodeData().setEvent( ( Event ) EventParser.getInstance().parse( element ) );
		//            }
		//            else if ( qualified_name.equals( PhyloXmlMapping.PROPERTY ) ) {
		//                final Property prop = ( Property ) PropertyParser.getInstance().parse( element );
		//                if ( prop.getRef().startsWith( NodeVisualData.APTX_VISUALIZATION_REF )
		//                        && ( prop.getAppliesTo() == AppliesTo.NODE ) ) {
		//                    if ( node.getNodeData().getNodeVisualData() == null ) {
		//                        node.getNodeData().setNodeVisualData( new NodeVisualData() );
		//                    }
		//                    node.getNodeData().getNodeVisualData().parseProperty( prop );
		//                }
		//                else {
		//                    if ( !node.getNodeData().isHasProperties() ) {
		//                        node.getNodeData().setProperties( new PropertiesMap() );
		//                    }
		//                    node.getNodeData().getProperties().addProperty( prop );
		//                }
		//            }
		//        }
	}

	private void newClade() {
		if ( getCurrentNode() == null ) {
			initCurrentNode();
		}
		else {
			addNode();
		}
	}

	private void newTree() {
		setCurrentTree( new Tree("") );
	}

	private void reset() {
		setCurrentTree( null );
		setCurrentNode( null );
		setCurrentElementName( null );
		setCurrentXmlElement( null );
	}

	private void setCurrentElementName( final String element_name ) {
		_current_element_name = element_name;
	}

	private void setCurrentNode( final Bioentity current_node ) {
		_current_node = current_node;
	}

	private void setCurrentTree( final Tree phylogeny ) {
		_current_phylogeny = phylogeny;
	}

	private void setCurrentXmlElement( final XmlElement element ) {
		_current_xml_element = element;
	}

	private void setPhylogenies( final List<Tree> phylogenies ) {
		_phylogenies = phylogenies;
	}

	@Override
	public void startDocument() throws SAXException {
		init();
	}

	@Override
	public void startElement( final String namespace_uri,
			final String local_name,
			final String qualified_name,
			final Attributes attributes ) throws SAXException {
		if ( PhyloUtil.isEmpty( namespace_uri ) || namespace_uri.startsWith( PhyloConstant.PHYLO_XML_LOCATION ) ) {
			setCurrentElementName( local_name );
			if ( local_name.equals( PhyloXmlMapping.CLADE ) ) {
				final XmlElement element = new XmlElement( namespace_uri, local_name, local_name, attributes );
				getCurrentXmlElement().addChildElement( element );
				setCurrentXmlElement( element );
				newClade();
			}
			else if ( local_name.equals( PhyloXmlMapping.PHYLOGENY ) ) {
				setCurrentXmlElement( new XmlElement( "", "", "", null ) );
				newTree();
				final XmlElement element = new XmlElement( namespace_uri, local_name, local_name, attributes );
				if ( element.isHasAttribute( PhyloXmlMapping.PHYLOGENY_IS_REROOTABLE_ATTR ) ) {
					getCurrentTree().setRerootable( Boolean.parseBoolean( element
							.getAttribute( PhyloXmlMapping.PHYLOGENY_IS_REROOTABLE_ATTR ) ) );
				}
				if ( element.isHasAttribute( PhyloXmlMapping.PHYLOGENY_BRANCHLENGTH_UNIT_ATTR ) ) {
					getCurrentTree()
					.setDistanceUnit( element.getAttribute( PhyloXmlMapping.PHYLOGENY_BRANCHLENGTH_UNIT_ATTR ) );
				}
				if ( element.isHasAttribute( PhyloXmlMapping.PHYLOGENY_IS_ROOTED_ATTR ) ) {
					getCurrentTree().setRooted( Boolean.parseBoolean( element
							.getAttribute( PhyloXmlMapping.PHYLOGENY_IS_ROOTED_ATTR ) ) );
				}
				if ( element.isHasAttribute( PhyloXmlMapping.PHYLOGENY_TYPE_ATTR ) ) {
					getCurrentNode().setType( ( element.getAttribute( PhyloXmlMapping.PHYLOGENY_TYPE_ATTR ) ) );
				}
			}
			else if ( local_name.equals( PHYLOXML ) ) {
			}
			else if ( getCurrentTree() != null ) {
				final XmlElement element = new XmlElement( namespace_uri, local_name, local_name, attributes );
				getCurrentXmlElement().addChildElement( element );
				setCurrentXmlElement( element );
			}
		}
	}

	public static boolean attributeEqualsValue( final XmlElement element,
			final String attributeName,
			final String attributeValue ) {
		final String attr = element.getAttribute( attributeName );
		return ( ( attr != null ) && attr.equals( attributeValue ) );
	}

	public static String getAtttributeValue( final XmlElement element, final String attributeName ) {
		final String attr = element.getAttribute( attributeName );
		if ( attr != null ) {
			return attr;
		}
		else {
			return "";
		}
	}

	static public Map<String, String> getSequenceMapByIdForPhylogeny( final Tree ph ) {
		HashMap<String, String> seqMap = phylogenySequencesById.get( ph );
		if ( seqMap == null ) {
			seqMap = new HashMap<String, String>();
			phylogenySequencesById.put( ph, seqMap );
		}
		return seqMap;
	}

	private static void mapElementToTree( final XmlElement xml_element, final Tree phylogeny , final Family family)
			throws PhylogenyParserException, PhyloXmlDataFormatException {
		for( int i = 0; i < xml_element.getNumberOfChildElements(); ++i ) {
			final XmlElement element = xml_element.getChildElement( i );
			final String qualified_name = element.getQualifiedName();
			if ( qualified_name.equals( PhyloXmlMapping.PHYLOGENY_NAME ) ) {
				family.setFamily_name( element.getValueAsString() );
			}
			else if ( qualified_name.equals( PhyloXmlMapping.PHYLOGENY_DESCRIPTION ) ) {
				family.setDescription( element.getValueAsString() );
			}
			else if ( qualified_name.equals( PhyloXmlMapping.IDENTIFIER ) ) {
				family.setIdentifier( element.getValueAsString() );
			}
			else if ( qualified_name.equals( PhyloXmlMapping.CONFIDENCE ) ) {
				family.setConfidence( element.getValueAsString() );
			}
		}
	}
}
