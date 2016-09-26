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

package org.bbop.phylo.io.tol;

import java.util.ArrayList;
import java.util.List;

import org.bbop.phylo.io.PhyloConstant;
import org.bbop.phylo.io.PhylogenyParserException;
import org.bbop.phylo.io.Taxonomy;
import org.bbop.phylo.io.phyloxml.FailedConditionCheckException;
import org.bbop.phylo.io.phyloxml.XmlElement;
import org.bbop.phylo.model.Protein;
import org.bbop.phylo.model.Tree;
import org.bbop.phylo.util.PhyloUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import owltools.gaf.Bioentity;

public final class TolXmlHandler extends DefaultHandler {

    private String              _current_element_name;
    private Tree                _current_phylogeny;
    private List<Tree>          _phylogenies;
    private XmlElement          _current_xml_element;
    private Protein             _current_node;
    private final static StringBuffer _buffer = new StringBuffer();

    TolXmlHandler() {
        // Constructor.
    }

    private void addNode() {
        final Protein new_node = new Protein();
        getCurrentNode().addChild( new_node );
        setCurrentNode( new_node );
    }

    @Override
    public void characters( final char[] chars, final int start_index, final int end_index ) {
        if ( ( ( getCurrentXmlElement() != null ) && ( getCurrentElementName() != null ) )
                && !getCurrentElementName().equals( TolXmlMapping.CLADE )
                && !getCurrentElementName().equals( TolXmlMapping.PHYLOGENY ) ) {
            getCurrentXmlElement().setValue( new String( chars, start_index, end_index ).trim() );
        }
    }

    @Override
    public void endElement( final String namespace_uri, final String local_name, final String qualified_name )
            throws SAXException {
        if ( PhyloUtil.isEmpty( namespace_uri ) || namespace_uri.startsWith( PhyloConstant.PHYLO_XML_LOCATION ) ) {
            if ( local_name.equals( TolXmlMapping.CLADE ) ) {
                try {
                    TolXmlHandler.mapElementToPhylogenyNode( getCurrentXmlElement(), getCurrentNode() );
                    if ( !getCurrentNode().isRoot() ) {
                        setCurrentNode( (Protein) getCurrentNode().getParent() );
                    }
                    setCurrentXmlElement( getCurrentXmlElement().getParent() );
                }
                catch ( final PhylogenyParserException ex ) {
                    throw new SAXException( ex.getMessage() );
                }
            }
            else if ( local_name.equals( TolXmlMapping.PHYLOGENY ) ) {
                try {
                    TolXmlHandler.mapElementToPhylogeny( getCurrentXmlElement(), getCurrentPhylogeny() );
                }
                catch ( final PhylogenyParserException ex ) {
                    throw new SAXException( ex.getMessage() );
                }
                finishPhylogeny();
                reset();
            }
            else if ( ( getCurrentPhylogeny() != null ) && ( getCurrentXmlElement().getParent() != null ) ) {
                setCurrentXmlElement( getCurrentXmlElement().getParent() );
            }
            setCurrentElementName( null );
        }
    }

    private void finishPhylogeny() throws SAXException {
        getCurrentPhylogeny().setRooted( true );
        getCurrentPhylogeny().initCurrentNodes();
        getPhylogenies().add( getCurrentPhylogeny() );
    }

    private String getCurrentElementName() {
        return _current_element_name;
    }

    private Protein getCurrentNode() {
        return _current_node;
    }

    private Tree getCurrentPhylogeny() {
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
        if ( getCurrentPhylogeny() == null ) {
            throw new FailedConditionCheckException( "attempt to create new current node for non-existing phylogeny" );
        }
        final Protein node = new Protein();
        getCurrentPhylogeny().setRoot( node );
        setCurrentNode( getCurrentPhylogeny().getRoot() );
    }

    private void newClade() {
        if ( getCurrentNode() == null ) {
            initCurrentNode();
        }
        else {
            addNode();
        }
    }

    private void newPhylogeny() {
        setCurrentPhylogeny( new Tree() );
    }

    private void reset() {
        setCurrentPhylogeny( null );
        setCurrentNode( null );
        setCurrentElementName( null );
        setCurrentXmlElement( null );
    }

    private void setCurrentElementName( final String element_name ) {
        _current_element_name = element_name;
    }

    private void setCurrentNode( final Protein current_node ) {
        _current_node = current_node;
    }

    private void setCurrentPhylogeny( final Tree phylogeny ) {
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
        setCurrentElementName( local_name );
        if ( local_name.equals( TolXmlMapping.CLADE ) ) {
            final XmlElement element = new XmlElement( namespace_uri, local_name, local_name, attributes );
            getCurrentXmlElement().addChildElement( element );
            setCurrentXmlElement( element );
            newClade();
        }
        else if ( local_name.equals( TolXmlMapping.PHYLOGENY ) ) {
            setCurrentXmlElement( new XmlElement( "", "", "", null ) );
            newPhylogeny();
        }
        else if ( getCurrentPhylogeny() != null ) {
            final XmlElement element = new XmlElement( namespace_uri, local_name, local_name, attributes );
            getCurrentXmlElement().addChildElement( element );
            setCurrentXmlElement( element );
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

    private static void mapElementToPhylogeny( final XmlElement xml_element, final Tree phylogeny )
            throws PhylogenyParserException {
        // Not needed for now.
    }

    private static void mapElementToPhylogenyNode( final XmlElement xml_element, final Protein node )
            throws PhylogenyParserException {
        if ( xml_element.isHasAttribute( TolXmlMapping.NODE_ID_ATTR ) ) {
            final String id = xml_element.getAttribute( TolXmlMapping.NODE_ID_ATTR );
            if ( !PhyloUtil.isEmpty( id ) ) {
//                if ( !node.getNodeData().isHasTaxonomy() ) {
//                    node.getNodeData().setTaxonomy( new Taxonomy() );
//                }
//                node.getNodeData().getTaxonomy()
//                .setIdentifier( new Identifier( id, TolXmlMapping.TOL_TAXONOMY_ID_TYPE ) );
            }
        }
        final boolean put_into_scientific_name = true; // Allways put into scientific name.
        //        if ( xml_element.isHasAttribute( TolXmlMapping.NODE_ITALICIZENAME_ATTR ) ) {
        //            final String ital = xml_element.getAttribute( TolXmlMapping.NODE_ITALICIZENAME_ATTR );
        //            if ( !PhyloUtil.isEmpty( ital ) && ital.equals( "1" ) ) {
        //                put_into_scientific_name = true;
        //            }
        //        }
        for( int i = 0; i < xml_element.getNumberOfChildElements(); ++i ) {
            final XmlElement element = xml_element.getChildElement( i );
            final String qualified_name = element.getQualifiedName();
            if ( qualified_name.equals( TolXmlMapping.TAXONOMY_NAME ) ) {
                final String name = element.getValueAsString();
                if ( !PhyloUtil.isEmpty( name ) ) {
//                    if ( !node.getNodeData().isHasTaxonomy() ) {
//                        node.getNodeData().setTaxonomy( new Taxonomy() );
//                    }
//                    if ( put_into_scientific_name ) {
//                        node.getNodeData().getTaxonomy().setScientificName( name );
//                    }
//                    else {
//                        node.getNodeData().getTaxonomy().setCommonName( name );
//                    }
                }
            }
            else if ( qualified_name.equals( TolXmlMapping.AUTHORITY ) ) {
                String auth = element.getValueAsString();
                if ( !PhyloUtil.isEmpty( auth ) && !auth.equalsIgnoreCase( "null" ) ) {
//                    if ( !node.getNodeData().isHasTaxonomy() ) {
//                        node.getNodeData().setTaxonomy( new Taxonomy() );
//                    }
//                    auth = auth.replaceAll( "&amp;", "&" );
//                    node.getNodeData().getTaxonomy().setAuthority( auth );
                }
            }
            else if ( qualified_name.equals( TolXmlMapping.AUTHDATE ) ) {
                final String authdate = element.getValueAsString();
                if ( !PhyloUtil.isEmpty( authdate ) && !authdate.equalsIgnoreCase( "null" ) ) {
//                    if ( node.getNodeData().isHasTaxonomy()
//                            && !PhyloUtil.isEmpty( node.getNodeData().getTaxonomy().getAuthority() ) ) {
//                        _buffer.setLength( 0 );
//                        _buffer.append( node.getNodeData().getTaxonomy().getAuthority() );
//                        _buffer.append( " " );
//                        _buffer.append( authdate );
//                        node.getNodeData().getTaxonomy().setAuthority( _buffer.toString() );
//                    }
                }
            }
            else if ( qualified_name.equals( TolXmlMapping.OTHERNAMES ) ) {
                for( int j = 0; j < element.getNumberOfChildElements(); ++j ) {
                    final XmlElement element_j = element.getChildElement( j );
                    if ( element_j.getQualifiedName().equals( TolXmlMapping.OTHERNAME ) ) {
                        for( int z = 0; z < element_j.getNumberOfChildElements(); ++z ) {
                            final XmlElement element_z = element_j.getChildElement( z );
                            if ( element_z.getQualifiedName().equals( TolXmlMapping.OTHERNAME_NAME ) ) {
                                final String syn = element_z.getValueAsString();
                                if ( !PhyloUtil.isEmpty( syn ) && !syn.equalsIgnoreCase( "null" ) ) {
//                                    if ( !node.getNodeData().isHasTaxonomy() ) {
//                                        node.getNodeData().setTaxonomy( new Taxonomy() );
//                                    }
//                                    node.getNodeData().getTaxonomy().getSynonyms().add( syn );
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}