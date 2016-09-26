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

package org.bbop.phylo.io.writer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.bbop.phylo.io.PhyloConstant;
import org.bbop.phylo.io.PhyloConstant.NH_CONVERSION_SUPPORT_VALUE_STYLE;
import org.bbop.phylo.io.PhylogenyDataUtil;
import org.bbop.phylo.io.nexus.NexusConstants;
import org.bbop.phylo.io.phyloxml.PhyloXmlMapping;
import org.bbop.phylo.io.phyloxml.PhyloXmlUtil;
import org.bbop.phylo.model.Family;
import org.bbop.phylo.model.Protein;
import org.bbop.phylo.model.Tree;
import org.bbop.phylo.util.PhyloUtil;

public final class PhylogenyWriter {

	public final static boolean         INDENT_PHYLOXML_DEAFULT         = true;
	public final static String          PHYLO_XML_INTENDATION_BASE      = "  ";
	public final static String          PHYLO_XML_VERSION_ENCODING_LINE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	public final static String          PHYLO_XML_NAMESPACE_LINE        = "<phyloxml xmlns:xsi=\""
			+ PhyloConstant.XML_SCHEMA_INSTANCE
			+ "\" xsi:schemaLocation=\""
			+ PhyloConstant.PHYLO_XML_LOCATION
			+ " "
			+ PhyloConstant.PHYLO_XML_LOCATION
			+ "/"
			+ PhyloConstant.PHYLO_XML_VERSION
			+ "/" + PhyloConstant.PHYLO_XML_XSD
			+ "\" " + "xmlns=\""
			+ PhyloConstant.PHYLO_XML_LOCATION
			+ "\">";
	public final static String          PHYLO_XML_END                   = "</phyloxml>";
	private boolean                     _saw_comma;
	private StringBuffer                _buffer;
	private Writer                      _writer;
	private Protein               _root;
	private boolean                     _has_next;
	private Stack<PostOrderStackObject> _stack;
	private boolean                     _nh_write_distance_to_parent;
	NH_CONVERSION_SUPPORT_VALUE_STYLE   _nh_conversion_support_style;
	private boolean                     _indent_phyloxml;
	private int                         _node_level;
	private int                         _phyloxml_level;
	private FORMAT                      _format;

	public PhylogenyWriter() {
		setIndentPhyloxml( INDENT_PHYLOXML_DEAFULT );
		setNhConversionSupportStyle( NH_CONVERSION_SUPPORT_VALUE_STYLE.NONE );
	}

	private void appendPhylogenyLevelPhyloXml( final Writer writer, final Family family, final Tree tree ) throws IOException {
		final String indentation = new String();
		if ( !PhyloUtil.isEmpty( family.getFamily_name() ) ) {
			PhylogenyDataUtil.appendElement( writer, PhyloXmlMapping.PHYLOGENY_NAME, family.getFamily_name(), indentation );
		}
		if ( family.getIdentifier() != null ) {
			if ( PhyloUtil.isEmpty( family.getIdentifier()) ) {
				PhylogenyDataUtil.appendElement( writer,
						PhyloXmlMapping.IDENTIFIER,
						family.getIdentifier(),
						indentation );
			}
		}
		if ( !PhyloUtil.isEmpty( family.getDescription() ) ) {
			PhylogenyDataUtil.appendElement( writer,
					PhyloXmlMapping.PHYLOGENY_DESCRIPTION,
					family.getDescription(),
					indentation );
		}
	}

	private StringBuffer createIndentation() {
		if ( !isIndentPhyloxml() ) {
			return null;
		}
		final StringBuffer sb = new StringBuffer( getNodeLevel() * 2 );
		for( int i = 0; i < getNodeLevel(); ++i ) {
			sb.append( PhylogenyWriter.PHYLO_XML_INTENDATION_BASE );
		}
		return sb;
	}

	private void decreaseNodeLevel() {
		--_node_level;
	}

	private StringBuffer getBuffer() {
		return _buffer;
	}

	private int getNodeLevel() {
		return _node_level;
	}

	private StringBuffer getOutput( final Tree tree ) throws IOException {
		if ( getOutputFormt() == FORMAT.PHYLO_XML ) {
			throw new RuntimeException( "method inappropriately called" );
		}
		if ( tree != null ) {
			reset( tree );
			while ( isHasNext() ) {
				next();
			}
			if ( getOutputFormt() == FORMAT.NH ) {
				getBuffer().append( ';' );
			}
			return getBuffer();
		}
		else {
			return new StringBuffer( 0 );
		}
	}

	private FORMAT getOutputFormt() {
		return _format;
	}

	private int getPhyloXmlLevel() {
		return _phyloxml_level;
	}

	private Protein getRoot() {
		return _root;
	}

	private Stack<PostOrderStackObject> getStack() {
		return _stack;
	}

	private Writer getWriter() {
		return _writer;
	}

	private void increaseNodeLevel() {
		++_node_level;
	}

	private boolean isHasNext() {
		return _has_next;
	}

	private boolean isIndentPhyloxml() {
		return _indent_phyloxml;
	}

	private boolean isSawComma() {
		return _saw_comma;
	}

	private boolean isWriteDistanceToParentInNH() {
		return _nh_write_distance_to_parent;
	}

	private void next() throws IOException {
		while ( true ) {
			final PostOrderStackObject si = getStack().pop();
			final Protein node = si.getNode();
			final int phase = si.getPhase();
			final int kidcount = node.getChildren() == null ? 0 : node.getChildren().size();
			if ( phase > kidcount ) {
				setHasNext( node != getRoot() );
				if ( ( getOutputFormt() != FORMAT.PHYLO_XML ) || node.isLeaf() ) {
					if ( !node.isRoot() && node.isFirstChildNode() ) {
						increaseNodeLevel();
					}
					if ( getOutputFormt() == FORMAT.PHYLO_XML ) {
						writeNode( node, createIndentation() );
					}
					else {
						writeNode( node, null );
					}
				}
				if ( !node.isRoot() ) {
					if ( !node.isLastChildNode() ) {
						writeCladeSeparator();
					}
					else {
						writeCloseClade();
					}
				}
				return;
			}
			else {
				getStack().push( new PostOrderStackObject( node, ( phase + 1 ) ) );
				if ( !node.isLeaf() ) {
					getStack().push( new PostOrderStackObject( (Protein) node.getChildren().get( phase - 1 ), 1 ) );
					writeOpenClade( node );
					if ( getOutputFormt() == FORMAT.PHYLO_XML ) {
						if ( phase == 1 ) {
							writeNode( node, createIndentation() );
						}
					}
				}
			}
		}
	}

	private void reset( final Tree tree ) {
		setBuffer( new StringBuffer() );
		setWriter( null );
		setSawComma( false );
		setHasNext( true );
		setRoot( tree.getRoot() );
		setStack( new Stack<PostOrderStackObject>() );
		getStack().push( new PostOrderStackObject( tree.getRoot(), 1 ) );
		setNodeLevel( 1 );
	}

	private void reset( final Writer writer, final Tree tree ) {
		setBuffer( null );
		setWriter( writer );
		setSawComma( false );
		setHasNext( true );
		setRoot( tree.getRoot() );
		setStack( new Stack<PostOrderStackObject>() );
		getStack().push( new PostOrderStackObject( tree.getRoot(), 1 ) );
		setNodeLevel( 1 );
	}

	private void setBuffer( final StringBuffer buffer ) {
		_buffer = buffer;
	}

	private void setHasNext( final boolean has_next ) {
		_has_next = has_next;
	}

	public void setIndentPhyloxml( final boolean indent_phyloxml ) {
		_indent_phyloxml = indent_phyloxml;
	}

	private void setNodeLevel( final int level ) {
		_node_level = level;
	}

	private void setOutputFormt( final FORMAT format ) {
		_format = format;
	}

	private void setPhyloXmlLevel( final int phyloxml_level ) {
		_phyloxml_level = phyloxml_level;
	}

	private void setRoot( final Protein root ) {
		_root = root;
	}

	private void setSawComma( final boolean saw_comma ) {
		_saw_comma = saw_comma;
	}

	private void setStack( final Stack<PostOrderStackObject> stack ) {
		_stack = stack;
	}

	private void setWriteDistanceToParentInNH( final boolean nh_write_distance_to_parent ) {
		_nh_write_distance_to_parent = nh_write_distance_to_parent;
	}

	private void setWriter( final Writer writer ) {
		_writer = writer;
	}

	public void toNewHampshire( final List<Tree> trees,
			final boolean write_distance_to_parent,
			final File out_file,
			final String separator ) throws IOException {
		final Iterator<Tree> it = trees.iterator();
		final StringBuffer sb = new StringBuffer();
		while ( it.hasNext() ) {
			sb.append( toNewHampshire( it.next(), write_distance_to_parent ) );
			sb.append( separator );
		}
		writeToFile( sb, out_file );
	}

	public StringBuffer toNewHampshire( final Tree tree,
			final boolean nh_write_distance_to_parent,
			final NH_CONVERSION_SUPPORT_VALUE_STYLE svs ) throws IOException {
		setOutputFormt( FORMAT.NH );
		setNhConversionSupportStyle( svs );
		setWriteDistanceToParentInNH( nh_write_distance_to_parent );
		return getOutput( tree );
	}

	public StringBuffer toNewHampshire( final Tree tree, final boolean nh_write_distance_to_parent )
			throws IOException {
		setOutputFormt( FORMAT.NH );
		setWriteDistanceToParentInNH( nh_write_distance_to_parent );
		return getOutput( tree );
	}

	public void toNewHampshire( final Tree tree, final boolean write_distance_to_parent, final File out_file )
			throws IOException {
		writeToFile( toNewHampshire( tree, write_distance_to_parent ), out_file );
	}

	public void toNewHampshire( final Tree tree,
			final boolean write_distance_to_parent,
			final NH_CONVERSION_SUPPORT_VALUE_STYLE svs,
			final File out_file ) throws IOException {
		writeToFile( toNewHampshire( tree, write_distance_to_parent, svs ), out_file );
	}

	public void toNewHampshire( final Tree[] trees,
			final boolean write_distance_to_parent,
			final File out_file,
			final String separator ) throws IOException {
		final StringBuffer sb = new StringBuffer();
		for( final Tree element : trees ) {
			sb.append( toNewHampshire( element, write_distance_to_parent ) );
			sb.append( separator );
		}
		writeToFile( sb, out_file );
	}

	public void toNewHampshireX( final List<Tree> trees, final File out_file, final String separator )
			throws IOException {
		final Iterator<Tree> it = trees.iterator();
		final StringBuffer sb = new StringBuffer();
		while ( it.hasNext() ) {
			sb.append( toNewHampshireX( it.next() ) );
			sb.append( separator );
		}
		writeToFile( sb, out_file );
	}

	public StringBuffer toNewHampshireX( final Tree tree ) throws IOException {
		setOutputFormt( FORMAT.NHX );
		return getOutput( tree );
	}

	public void toNewHampshireX( final Tree tree, final File out_file ) throws IOException {
		writeToFile( toNewHampshireX( tree ), out_file );
	}

	public void toNewHampshireX( final Tree[] trees, final File out_file, final String separator )
			throws IOException {
		final StringBuffer sb = new StringBuffer();
		for( final Tree element : trees ) {
			sb.append( toNewHampshireX( element ) );
			sb.append( separator );
		}
		writeToFile( sb, out_file );
	}

	public void toNexus( final File out_file, final Tree tree, final NH_CONVERSION_SUPPORT_VALUE_STYLE svs )
			throws IOException {
		final Writer writer = new BufferedWriter( new PrintWriter( out_file, PhyloConstant.UTF_8 ) );
		final List<Tree> trees = new ArrayList<Tree>( 1 );
		trees.add( tree );
		writeNexusStart( writer );
		writeNexusTaxaBlock( writer, tree );
		writeNexusTreesBlock( writer, trees, svs );
		writer.flush();
		writer.close();
	}

	public StringBuffer toNexus( final Tree tree, final NH_CONVERSION_SUPPORT_VALUE_STYLE svs ) throws IOException {
		final StringWriter string_writer = new StringWriter();
		final Writer writer = new BufferedWriter( string_writer );
		final List<Tree> trees = new ArrayList<Tree>( 1 );
		trees.add( tree );
		writeNexusStart( writer );
		writeNexusTaxaBlock( writer, tree );
		writeNexusTreesBlock( writer, trees, svs );
		writer.flush();
		writer.close();
		return string_writer.getBuffer();
	}

	public void toPhyloXML( final File out_file, final Family family, final Tree tree, final int phyloxml_level ) throws IOException {
		final Writer writer = new BufferedWriter( new PrintWriter( out_file, PhyloConstant.UTF_8 ) );
		writePhyloXmlStart( writer );
		toPhyloXMLNoPhyloXmlSource( writer, family, tree, phyloxml_level );
		writePhyloXmlEnd( writer );
		writer.flush();
		writer.close();
	}

	public StringBuffer toPhyloXML( final Family family, final Tree tree, final int phyloxml_level ) throws IOException {
		final StringWriter string_writer = new StringWriter();
		final Writer writer = new BufferedWriter( string_writer );
		setPhyloXmlLevel( phyloxml_level );
		setOutputFormt( FORMAT.PHYLO_XML );
		writePhyloXmlStart( writer );
		writeOutput( writer, family, tree );
		writePhyloXmlEnd( writer );
		writer.flush();
		writer.close();
		return string_writer.getBuffer();
	}

	public void toPhyloXML( final Family family, final Tree phy, final int phyloxml_level, final File out_file ) throws IOException {
		final Writer writer = new BufferedWriter( new PrintWriter( out_file ) );
		toPhyloXML( writer, family, phy, phyloxml_level );
		writer.flush();
		writer.close();
	}

	public void toPhyloXML( final Writer writer, final Family family, final Tree tree, final int phyloxml_level ) throws IOException {
		setPhyloXmlLevel( phyloxml_level );
		setOutputFormt( FORMAT.PHYLO_XML );
		writePhyloXmlStart( writer );
		writeOutput( writer, family, tree );
		writePhyloXmlEnd( writer );
	}

	private void toPhyloXMLNoPhyloXmlSource( final Writer writer, final Family family, final Tree tree, final int phyloxml_level )
			throws IOException {
		setPhyloXmlLevel( phyloxml_level );
		setOutputFormt( FORMAT.PHYLO_XML );
		writeOutput( writer, family, tree );
	}

	private void writeCladeSeparator() {
		setSawComma( true );
		if ( ( getOutputFormt() == FORMAT.NHX ) || ( getOutputFormt() == FORMAT.NH ) ) {
			getBuffer().append( "," );
		}
	}

	private void writeCloseClade() throws IOException {
		decreaseNodeLevel();
		if ( getOutputFormt() == FORMAT.PHYLO_XML ) {
			getWriter().write( PhyloUtil.LINE_SEPARATOR );
			if ( isIndentPhyloxml() ) {
				getWriter().write( createIndentation().toString() );
			}
			PhylogenyDataUtil.appendClose( getWriter(), PhyloXmlMapping.CLADE );
		}
		else if ( ( getOutputFormt() == FORMAT.NHX ) || ( getOutputFormt() == FORMAT.NH ) ) {
			getBuffer().append( ")" );
		}
	}

	private void writeNode( final Protein node, final StringBuffer indentation ) throws IOException {
		if ( getOutputFormt() == FORMAT.PHYLO_XML ) {
			if ( node.isLeaf() ) {
				getWriter().write( PhyloUtil.LINE_SEPARATOR );
				if ( indentation != null ) {
					getWriter().write( indentation.toString() );
				}
				String distance = String.valueOf( PhyloUtil.round( node.getDistanceFromParent(), 
						PhyloXmlUtil.ROUNDING_DIGITS_FOR_PHYLOXML_DOUBLE_OUTPUT ) );

				PhylogenyDataUtil.appendOpen( getWriter(), PhyloXmlMapping.CLADE,
						PhyloXmlMapping.IDENTIFIER_PROVIDER_ATTR, node.getDb(),
						PhyloXmlMapping.BRANCH_LENGTH, distance);
			}
			PhyloXmlNodeWriter.toPhyloXml( getWriter(),
					node,
					getPhyloXmlLevel(),
					indentation != null ? indentation.toString() : "" );
			if ( node.isLeaf() ) {
				getWriter().write( PhyloUtil.LINE_SEPARATOR );
				if ( indentation != null ) {
					getWriter().write( indentation.toString() );
				}
				PhylogenyDataUtil.appendClose( getWriter(), PhyloXmlMapping.CLADE );
			}
		}
		else if ( getOutputFormt() == FORMAT.NHX ) {
			getBuffer().append( toNewHampshireX(node) );
		}
		else if ( getOutputFormt() == FORMAT.NH ) {
			getBuffer().append( toNewHampshire( node, isWriteDistanceToParentInNH(), getNhConversionSupportStyle() ) );
		}
	}

	private NH_CONVERSION_SUPPORT_VALUE_STYLE getNhConversionSupportStyle() {
		return _nh_conversion_support_style;
	}

	private void setNhConversionSupportStyle( final NH_CONVERSION_SUPPORT_VALUE_STYLE nh_conversion_support_style ) {
		_nh_conversion_support_style = nh_conversion_support_style;
	}

	private void writeOpenClade( final Protein node ) throws IOException {
		if ( !isSawComma() ) {
			if ( !node.isRoot() && node.isFirstChildNode() ) {
				increaseNodeLevel();
			}
			if ( getOutputFormt() == FORMAT.PHYLO_XML ) {
				getWriter().write( PhyloUtil.LINE_SEPARATOR );
				if ( isIndentPhyloxml() ) {
					getWriter().write( createIndentation().toString() );
				}
				String distance = String.valueOf( PhyloUtil.round( node.getDistanceFromParent(), 
						PhyloXmlUtil.ROUNDING_DIGITS_FOR_PHYLOXML_DOUBLE_OUTPUT ) );

				PhylogenyDataUtil.appendOpen( getWriter(), PhyloXmlMapping.CLADE,
						PhyloXmlMapping.IDENTIFIER_PROVIDER_ATTR, node.getDb(),
						PhyloXmlMapping.BRANCH_LENGTH, distance);
				//				}
			}
			else if ( ( getOutputFormt() == FORMAT.NHX ) || ( getOutputFormt() == FORMAT.NH ) ) {
				getBuffer().append( "(" );
			}
		}
		setSawComma( false );
	}

	private void writeOutput( final Writer writer, final Family family, final Tree tree ) throws IOException {
		if ( getOutputFormt() != FORMAT.PHYLO_XML ) {
			throw new RuntimeException( "method inappropriately called" );
		}
		if ( tree != null ) {
			reset( writer, tree );
			String unit = "";
			String type = "protein tree";
			if ( !PhyloUtil.isEmpty( tree.getDistanceUnit() ) ) {
				unit = tree.getDistanceUnit();
			}
			PhylogenyDataUtil.appendOpen( writer,
					PhyloXmlMapping.PHYLOGENY,
					PhyloXmlMapping.PHYLOGENY_IS_ROOTED_ATTR,
					tree.isRooted() + "",
					PhyloXmlMapping.PHYLOGENY_BRANCHLENGTH_UNIT_ATTR,
					unit,
					PhyloXmlMapping.PHYLOGENY_TYPE_ATTR,
					type,
					PhyloXmlMapping.PHYLOGENY_IS_REROOTABLE_ATTR,
					tree.isRerootable() + "" );
			appendPhylogenyLevelPhyloXml( writer, family, tree );
			while ( isHasNext() ) {
				next();
			}
			writer.write( PhyloUtil.LINE_SEPARATOR );
			PhylogenyDataUtil.appendClose( writer, PhyloXmlMapping.PHYLOGENY );
		}
	}

	private void writeToFile( final StringBuffer sb, final File out_file ) throws IOException {
		if ( out_file.exists() ) {
			throw new IOException( "attempt to overwrite existing file \"" + out_file.getAbsolutePath() + "\"" );
		}
		final PrintWriter out = new PrintWriter( out_file, PhyloConstant.UTF_8 );
		out.print( sb );
		out.flush();
		out.close();
	}

	public static PhylogenyWriter createPhylogenyWriter() {
		return new PhylogenyWriter();
	}

	private static void writeNexusStart( final Writer writer ) throws IOException {
		writer.write( NexusConstants.NEXUS );
		writer.write( PhyloUtil.LINE_SEPARATOR );
	}

	public static void writeNexusTaxaBlock( final Writer writer, final Tree tree ) throws IOException {
		writer.write( NexusConstants.BEGIN_TAXA );
		writer.write( PhyloUtil.LINE_SEPARATOR );
		writer.write( " " );
		writer.write( NexusConstants.DIMENSIONS );
		writer.write( " " );
		writer.write( NexusConstants.NTAX );
		writer.write( "=" );
		writer.write( String.valueOf( tree.getLeaves() ) );
		writer.write( ";" );
		writer.write( PhyloUtil.LINE_SEPARATOR );
		writer.write( " " );
		writer.write( NexusConstants.TAXLABELS );
		List<Protein> leaves = tree.getLeaves();
		for( Protein node : leaves ) {
			writer.write( " " );
			String data = "";
			if ( !PhyloUtil.isEmpty( node.getName() ) ) {
				data = node.getName();
			}
			//			else if ( node.getNodeData().isHasTaxonomy() ) {
			//				if ( !PhyloUtil.isEmpty( node.getNodeData().getTaxonomy().getTaxonomyCode() ) ) {
			//					data = node.getNodeData().getTaxonomy().getTaxonomyCode();
			//				}
			//				else if ( !PhyloUtil.isEmpty( node.getNodeData().getTaxonomy().getScientificName() ) ) {
			//					data = node.getNodeData().getTaxonomy().getScientificName();
			//				}
			//				else if ( !PhyloUtil.isEmpty( node.getNodeData().getTaxonomy().getCommonName() ) ) {
			//					data = node.getNodeData().getTaxonomy().getCommonName();
			//				}
			//			}
			else if ( !PhyloUtil.isEmpty(node.getSequence()) ) {
				if ( !PhyloUtil.isEmpty( node.getSeqId() ) ) {
					data = node.getSeqDb()+':'+node.getSeqId();
				}
				else if ( !PhyloUtil.isEmpty( node.getSymbol() ) ) {
					data = node.getSymbol();
				}
				else if ( !PhyloUtil.isEmpty( node.getFullName() ) ) {
					data = node.getFullName();
				}
			}
			writer.write( PhyloUtil.santitizeStringForNH( data ).toString() );
		}
		writer.write( ";" );
		writer.write( PhyloUtil.LINE_SEPARATOR );
		writer.write( NexusConstants.END );
		writer.write( PhyloUtil.LINE_SEPARATOR );
	}

	public static void writeNexusTreesBlock( final Writer writer,
			final List<Tree> trees,
			final NH_CONVERSION_SUPPORT_VALUE_STYLE svs ) throws IOException {
		writer.write( NexusConstants.BEGIN_TREES );
		writer.write( PhyloUtil.LINE_SEPARATOR );
		int i = 1;
		for( final Tree phylogeny : trees ) {
			writer.write( " " );
			writer.write( NexusConstants.TREE );
			writer.write( " " );
			if ( !PhyloUtil.isEmpty( phylogeny.getId() ) ) {
				writer.write( "\'" );
				writer.write( phylogeny.getId() );
				writer.write( "\'" );
			}
			else {
				writer.write( "tree" );
				writer.write( String.valueOf( i ) );
			}
			writer.write( "=" );
			if ( phylogeny.isRooted() ) {
				writer.write( "[&R]" );
			}
			else {
				writer.write( "[&U]" );
			}
			writer.write( toNewHampshireX( phylogeny.getRoot() ) );
			writer.write( PhyloUtil.LINE_SEPARATOR );
			i++;
		}
		writer.write( NexusConstants.END );
		writer.write( PhyloUtil.LINE_SEPARATOR );
	}

	private static void writePhyloXmlEnd( final Writer writer ) throws IOException {
		writer.write( PhyloUtil.LINE_SEPARATOR );
		writer.write( PhylogenyWriter.PHYLO_XML_END );
	}

	private static void writePhyloXmlStart( final Writer writer ) throws IOException {
		writer.write( PhylogenyWriter.PHYLO_XML_VERSION_ENCODING_LINE );
		writer.write( PhyloUtil.LINE_SEPARATOR );
		writer.write( PhylogenyWriter.PHYLO_XML_NAMESPACE_LINE );
		writer.write( PhyloUtil.LINE_SEPARATOR );
	}

	public static enum FORMAT {
		NH, NHX, PHYLO_XML, NEXUS;
	}

	/**
	 * Converts this PhylogenyNode to a New Hampshire X (NHX) String
	 * representation.
	 */
	final public static String toNewHampshireX(Protein node) {
		final StringBuilder sb = new StringBuilder();
		final StringBuffer s_nhx = new StringBuffer();
		if ( !PhyloUtil.isEmpty( node.getName() ) ) {
			sb.append( PhyloUtil.santitizeStringForNH( node.getName() ) );
		}
		if ( node.getDistanceToParent() != PhylogenyDataUtil.BRANCH_LENGTH_DEFAULT ) {
			sb.append( ":" );
			sb.append( node.getDistanceToParent() );
		}
		//        if ( getNodeDataDirectly() != null ) {
		//            s_nhx.append( getNodeDataDirectly().toNHX() );
		//        }
		//        if ( getBranchDataDirectly() != null ) {
		//            s_nhx.append( getBranchDataDirectly().toNHX() );
		//        }
		if ( s_nhx.length() > 0 ) {
			sb.append( "[&&NHX" );
			sb.append( s_nhx );
			sb.append( "]" );
		}
		return sb.toString();
	}

	// ---------------------------------------------------------
	// Writing of Nodes to Strings
	// ---------------------------------------------------------
	final public String toNewHampshire( Protein node,
			final boolean write_distance_to_parent,
			final NH_CONVERSION_SUPPORT_VALUE_STYLE svs ) {
		String data = "";
		if ( ( svs == NH_CONVERSION_SUPPORT_VALUE_STYLE.AS_INTERNAL_NODE_NAMES ) && !node.isLeaf() ) {
		}
		else if ( !PhyloUtil.isEmpty( node.getName() ) ) {
			data = node.getName();
		}
		//        else if ( node.getNodeData().isHasTaxonomy() ) {
		//            if ( !PhyloUtil.isEmpty( node.getNodeData().getTaxonomy().getTaxonomyCode() ) ) {
		//                data = node.getNodeData().getTaxonomy().getTaxonomyCode();
		//            }
		//            else if ( !PhyloUtil.isEmpty( node.getNodeData().getTaxonomy().getScientificName() ) ) {
		//                data = node.getNodeData().getTaxonomy().getScientificName();
		//            }
		//            else if ( !PhyloUtil.isEmpty( getNodeData().getTaxonomy().getCommonName() ) ) {
		//                data = node.getNodeData().getTaxonomy().getCommonName();
		//            }
		//        }
		else if ( !PhyloUtil.isEmpty(node.getSeqId()) ) {
			data = node.getSeqId();
		}
		else if ( !PhyloUtil.isEmpty( node.getSymbol() ) ) {
			data = node.getSymbol();
		}
		else if ( !PhyloUtil.isEmpty( node.getFullName() ) ) {
			data = node.getFullName();
		}
		//	}
		final StringBuilder sb = PhyloUtil.santitizeStringForNH( data );
		if ( write_distance_to_parent && ( node.getDistanceFromParent() != PhylogenyDataUtil.BRANCH_LENGTH_DEFAULT ) ) {
			sb.append( ":" );
			sb.append( node.getDistanceToParent() );
		}
		return sb.toString();
	}

}




