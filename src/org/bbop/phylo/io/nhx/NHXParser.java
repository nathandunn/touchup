// $Id:
// FORESTER -- software libraries and applications
// for evolutionary biology research and applications.
//
// Copyright (C) 2013 Christian M. Zmasek
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

package org.bbop.phylo.io.nhx;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.bbop.phylo.io.IteratingPhylogenyParser;
import org.bbop.phylo.io.PhyloConstant;
import org.bbop.phylo.io.PhylogenyDataUtil;
import org.bbop.phylo.io.PhylogenyMethods;
import org.bbop.phylo.io.PhylogenyParser;
import org.bbop.phylo.io.PhylogenyParserException;
import org.bbop.phylo.io.phyloxml.PhyloXmlDataFormatException;
import org.bbop.phylo.model.Protein;
import org.bbop.phylo.model.Tree;
import org.bbop.phylo.util.PhyloUtil;

import owltools.gaf.Bioentity;

public final class NHXParser implements PhylogenyParser, IteratingPhylogenyParser {

    private final static Pattern  MB_BL_PATTERN                              = Pattern.compile( "length.median=([-+eE0-9\\.]+)" );
    private final static Pattern  MB_PROB_PATTERN                            = Pattern.compile( "prob=([-+eE0-9\\.]+)" );
    private final static Pattern  MB_PROB_SD_PATTERN                         = Pattern.compile( "prob.stddev=([-+eE0-9\\.]+)" );
    private final static Pattern  NUMBERS_ONLY_PATTERN                       = Pattern.compile( "^[0-9\\.]+$" );
    
    private final static Pattern  BEAST_STYLE_EXTENDED_BOOTSTRAP_PATTERN              = Pattern.compile( "boot?strap=([\\d\\.]+)" );
    private final static Pattern  ENDS_WITH_NUMBER_PATTERN                   = Pattern.compile( "(:[-+eE0-9\\.]+$)" );
    
    public final static boolean  REPLACE_UNDERSCORES_DEFAULT                = false;
    private final static boolean ALLOW_ERRORS_IN_DISTANCE_TO_PARENT_DEFAULT = false;
    private final static byte    BUFFERED_READER                            = 3;
    private final static byte    CHAR_ARRAY                                 = 2;
    private final static boolean GUESS_IF_SUPPORT_VALUES                    = true;
    private final static boolean GUESS_ROOTEDNESS_DEFAULT                   = true;
    private final static boolean IGNORE_QUOTES_DEFAULT                      = false;
   
    private final static char    BELL                                       = 7;
    private final static String  ENCODING_DEFAULT = PhyloConstant.UTF_8;
    private boolean              _allow_errors_in_distance_to_parent;
    private int                  _clade_level;
    private StringBuilder        _current_anotation;
    private Protein              _current_node;
    private Tree                 _current_phylogeny;
    private boolean              _guess_rootedness;
    private int                  _i;
    private boolean              _ignore_quotes;
    private boolean              _in_comment                                = false;
    private boolean              _in_double_quote                           = false;
    private boolean              _in_open_bracket                           = false;
    private boolean              _in_single_quote                           = false;
    private byte                 _input_type;
    private BufferedReader       _my_source_br                              = null;
    private char[]               _my_source_charary                         = null;
    private Tree                 _next;
    private Object               _nhx_source;
    private boolean              _replace_underscores;
    private boolean              _saw_closing_paren;
    private boolean              _saw_colon                                 = false;
    private boolean              _saw_open_bracket                          = false;
    private Object               _source;
    private int                  _source_length;
    private TAXONOMY_EXTRACTION  _taxonomy_extraction;
    private boolean              _parse_beast_style_extended_tags           = false;
    private final String         _encoding;

	protected static Logger log = Logger.getLogger(NHXParser.class);

    public NHXParser() {
        _encoding = ENCODING_DEFAULT;
        init();
    }
    
    public NHXParser( final String encoding ) {
        _encoding = encoding;
        init();
    }

    @Override
    public String getName() {
        return "NH/NHX Parser";
    }

    public final TAXONOMY_EXTRACTION getTaxonomyExtraction() {
        return _taxonomy_extraction;
    }

    @Override
    public final boolean hasNext() {
        return _next != null;
    }

    @Override
    public final Tree next() throws NHXFormatException, IOException {
        final Tree phy = _next;
        parseNext();
        return phy;
    }

    @Override
    public final Tree[] parse() throws IOException {
        final List<Tree> l = new ArrayList<Tree>();
        while ( hasNext() ) {
            l.add( next() );
        }
        final Tree[] p = new Tree[ l.size() ];
        for( int i = 0; i < l.size(); ++i ) {
            p[ i ] = l.get( i );
        }
        reset();
        return p;
    }

    @Override
    public final void reset() throws NHXFormatException, IOException {
        _i = 0;
        _next = null;
        _in_comment = false;
        _saw_colon = false;
        _saw_open_bracket = false;
        _in_open_bracket = false;
        _in_double_quote = false;
        _in_single_quote = false;
        _clade_level = 0;
        _current_anotation = new StringBuilder();
        _current_phylogeny = null;
        _current_node = null;
        _my_source_charary = null;
        determineAndProcessSourceType( _source );
        switch ( _input_type ) {
            case CHAR_ARRAY:
                _my_source_br = null;
                _my_source_charary = ( char[] ) _nhx_source;
                break;
            case BUFFERED_READER:
                _my_source_br = ( BufferedReader ) _nhx_source;
                break;
            default:
                throw new RuntimeException( "unknown input type" );
        }
        parseNext();
    }

    public final void setGuessRootedness( final boolean guess_rootedness ) {
        _guess_rootedness = guess_rootedness;
    }

    public final void setIgnoreQuotes( final boolean ignore_quotes ) {
        _ignore_quotes = ignore_quotes;
    }

    public final void setReplaceUnderscores( final boolean replace_underscores ) {
        _replace_underscores = replace_underscores;
    }

    @Override
    public final void setSource( final Object nhx_source ) throws NHXFormatException, IOException {
        _source = nhx_source;
        reset();
    }

    public final void setTaxonomyExtraction( final TAXONOMY_EXTRACTION taxonomy_extraction ) {
        _taxonomy_extraction = taxonomy_extraction;
    }

    public final void setAllowErrorsInDistanceToParent( final boolean allow_errors_in_distance_to_parent ) {
        _allow_errors_in_distance_to_parent = allow_errors_in_distance_to_parent;
    }

    private final void determineAndProcessSourceType( final Object nhx_source ) throws IOException {
        if ( nhx_source == null ) {
            throw new PhylogenyParserException( getClass() + ": attempt to parse null object." );
        }
        else if ( nhx_source instanceof String ) {
            _nhx_source = nhx_source;
            _input_type = NHXParser.BUFFERED_READER;
            _source_length = 0;
            InputStream is = new ByteArrayInputStream( (( String ) nhx_source ).getBytes(getEncoding()));
            final InputStreamReader isr = new InputStreamReader( is, getEncoding() );
            _nhx_source = new BufferedReader( isr );
        }
        else if ( nhx_source instanceof char[] ) {
            _input_type = NHXParser.CHAR_ARRAY;
            _source_length = ( ( char[] ) nhx_source ).length;
            _nhx_source = nhx_source;
        }
        else if ( nhx_source instanceof File ) {
            _input_type = NHXParser.BUFFERED_READER;
            _source_length = 0;
            if ( _my_source_br != null ) {
                //I am REALLY not sure if it is a "good" idea NOT to close the stream...
                //                try {
                //                    _my_source_br.close();
                //                }
                //                catch ( final IOException e ) {
                //                }
            }
            final File f = ( File ) nhx_source;
            final String error = PhyloUtil.isReadableFile( f );
            if ( !PhyloUtil.isEmpty( error ) ) {
                throw new PhylogenyParserException( error );
            }
            final InputStream is = new FileInputStream( f );
            final InputStreamReader isr = new InputStreamReader( is, getEncoding() );
            _nhx_source = new BufferedReader( isr );
        }
        else if ( nhx_source instanceof URL ) {
            _input_type = NHXParser.BUFFERED_READER;
            _source_length = 0;
            if ( _my_source_br != null ) {
                //I am REALLY not sure if it is a "good" idea NOT to close the stream...
                //                try {
                //                    _my_source_br.close();
                //                }
                //                catch ( final IOException e ) {
                //                }
            }
            final InputStream is = ( ( URL ) nhx_source ).openStream();
            final InputStreamReader isr = new InputStreamReader( is, getEncoding() );
            _nhx_source = new BufferedReader( isr );
        }
        else if ( nhx_source instanceof InputStream ) {
            _input_type = NHXParser.BUFFERED_READER;
            _source_length = 0;
            if ( _my_source_br != null ) {
                //I am REALLY not sure if it is a "good" idea NOT to close the stream...
                //                try {
                //                    _my_source_br.close();
                //                }
                //                catch ( final IOException e ) {
                //                }
            }
            final InputStream is = ( InputStream ) nhx_source;
            final InputStreamReader isr = new InputStreamReader( is, getEncoding() );
             _nhx_source = new BufferedReader( isr );
        }
        else {
            throw new IllegalArgumentException( getClass() + " can only parse objects of type String,"
                    + " char[], File, InputStream, or URL "
                    + " [attempt to parse object of " + nhx_source.getClass() + "]." );
        }
    }

    private final Tree finishPhylogeny() throws PhylogenyParserException, NHXFormatException,
    PhyloXmlDataFormatException {
        if ( _current_phylogeny != null ) {
            parseNHX( _current_anotation != null ? _current_anotation.toString() : "",
                    _current_phylogeny.getRoot(),
                    getTaxonomyExtraction(),
                    isReplaceUnderscores(),
                    isAllowErrorsInDistanceToParent(),
                    true,
                    isParseBeastStyleExtendedTags());
            if ( GUESS_IF_SUPPORT_VALUES ) {
                if ( isBranchLengthsLikeBootstrapValues( _current_phylogeny ) ) {
                    moveBranchLengthsToConfidenceValues( _current_phylogeny );
                }
            }
            if ( isGuessRootedness() ) {
                final Bioentity root = _current_phylogeny.getRoot();
                if ( ( root.getDistanceFromParent() >= 0.0 ) || !PhyloUtil.isEmpty( root.getId() )
                        || !PhyloUtil.isEmpty( root.getSpeciesLabel() ) || root.getType() == null ) {
                    _current_phylogeny.setRooted( true );
                }
            }
            return _current_phylogeny;
        }
        return null;
    }

    private final Tree finishSingleNodePhylogeny() throws PhylogenyParserException, NHXFormatException,
    PhyloXmlDataFormatException {
        final Protein new_node = new Protein();
        parseNHX( _current_anotation.toString(),
                  new_node,
                  getTaxonomyExtraction(),
                  isReplaceUnderscores(),
                  isAllowErrorsInDistanceToParent(),
                  true,
                  isParseBeastStyleExtendedTags());
        _current_phylogeny = new Tree();
        _current_phylogeny.setRoot( new_node );
        return _current_phylogeny;
    }

    private final void init() {
        setTaxonomyExtraction( TAXONOMY_EXTRACTION.NO );
        setReplaceUnderscores( REPLACE_UNDERSCORES_DEFAULT );
        setGuessRootedness( GUESS_ROOTEDNESS_DEFAULT );
        setIgnoreQuotes( IGNORE_QUOTES_DEFAULT );
        setAllowErrorsInDistanceToParent( ALLOW_ERRORS_IN_DISTANCE_TO_PARENT_DEFAULT );
        setParseBeastStyleExtendedTags( false );
    }

    private final boolean isAllowErrorsInDistanceToParent() {
        return _allow_errors_in_distance_to_parent;
    }

    private final boolean isGuessRootedness() {
        return _guess_rootedness;
    }

    private final boolean isIgnoreQuotes() {
        return _ignore_quotes;
    }

    private final boolean isReplaceUnderscores() {
        return _replace_underscores;
    }

    private final void parseNext() throws IOException, NHXFormatException {
        if ( _source == null ) {
            throw new IOException( "source is not set" );
        }
        while ( true ) {
            char c = '\b';
            if ( _input_type == BUFFERED_READER ) {
                final int ci = _my_source_br.read();
                if ( ci >= 0 ) {
                    c = ( char ) ci;
                }
                else {
                    break;
                }
            }
            else {
                if ( _i >= _source_length ) {
                    break;
                }
                c = _my_source_charary[ _i ];
            }
            if ( !_in_single_quote && !_in_double_quote ) {
                if ( c == ':' ) {
                    _saw_colon = true;
                }
                else if ( !( ( c < 33 ) || ( c == 127 ) ) && _saw_colon
                        && ( ( c != '[' ) && ( c != '.' ) && ( ( c < 48 ) || ( c > 57 ) ) ) ) {
                    _saw_colon = false;
                }
                if ( _in_open_bracket && ( c == ']' ) ) {
                    _in_open_bracket = false;
                }
            }
            // \n\t is always ignored,
            // "=34  '=39 space=32
            if ( ( c < 32 ) || ( c == 127 ) || ( isIgnoreQuotes() && ( ( c == 32 ) || ( c == 34 ) || ( c == 39 ) ) )
                    || ( ( c == 32 ) && ( !_in_single_quote && !_in_double_quote ) )
                    || ( ( _clade_level == 0 ) && ( c == ';' ) && ( !_in_single_quote && !_in_double_quote ) ) ) {
                //do nothing
            }
            else if ( _in_comment ) {
                if ( c == ']' ) {
                    _in_comment = false;
                }
            }
            else if ( _in_double_quote ) {
                if ( c == '"' ) {
                    _in_double_quote = false;
                }
                else {
                    _current_anotation.append( changeCharInParens( c ) );
                }
            }
            else if ( ( c == '"' ) && !_in_single_quote ) {
                _in_double_quote = true;
            }
            else if ( _in_single_quote ) {
                if ( c == 39 ) {
                    _in_single_quote = false;
                }
                else {
                    _current_anotation.append( changeCharInParens( c ) );
                }
            }
            else if ( c == 39 ) {
                _in_single_quote = true;
            }
            else if ( c == '[' ) {
                _saw_open_bracket = true;
                _in_open_bracket = true;
            }
            else if ( _saw_open_bracket ) {
                if ( c != ']' ) {
                    // everything not starting with "[&" is considered a comment
                    // unless ":digits and/or . [bootstrap]":
                    if ( c == '&' ) {
                        _current_anotation.append( "[&" );
                    }
                    else if ( _saw_colon ) {
                        _current_anotation.append( "[" + c );
                    }
                    else {
                        _in_comment = true;
                    }
                }
                // comment consisting just of "[]":
                _saw_open_bracket = false;
            }
            else if ( ( c == '(' ) && !_in_open_bracket ) {
                final Tree phy = processOpenParen();
                if ( phy != null ) {
                    ++_i;
                    _next = phy;
                    return;
                }
            }
            else if ( ( c == ')' ) && !_in_open_bracket ) {
                processCloseParen();
            }
            else if ( ( c == ',' ) && !_in_open_bracket ) {
                processComma();
            }
            else {
                _current_anotation.append( c );
            }
            ++_i;
        } //  while ( true )
        if ( _clade_level != 0 ) {
            throw new PhylogenyParserException( "error in NH (Newick) formatted data: most likely cause: number of open parens does not equal number of close parens" );
        }
        if ( _current_phylogeny != null ) {
            _next = finishPhylogeny();
            _current_phylogeny = null;
            _current_anotation = null;
        }
        else if ( ( _current_anotation != null ) && ( _current_anotation.length() > 0 ) ) {
            _next = finishSingleNodePhylogeny();
            _current_anotation = null;
        }
        else {
            _next = null;
        }
    }

    private final static char changeCharInParens( char c ) {
        if ( c == ':' ) {
            c = BELL;
        }
        else if ( c == '[' ) {
            c = '{';
        }
        else if ( c == ']' ) {
            c = '}';
        }
        return c;
    }

    private final void processCloseParen() throws PhylogenyParserException, NHXFormatException,
    PhyloXmlDataFormatException {
        if ( _clade_level < 0 ) {
            throw new PhylogenyParserException( "error in NH (Newick)/NHX formatted data: most likely cause: number of close parens is larger than number of open parens" );
        }
        --_clade_level;
        if ( !_saw_closing_paren ) {
            final Protein new_node = new Protein();
            parseNHX( _current_anotation.toString(),
                      new_node,
                      getTaxonomyExtraction(),
                      isReplaceUnderscores(),
                      isAllowErrorsInDistanceToParent(),
                      true,
                      isParseBeastStyleExtendedTags());
            _current_anotation = new StringBuilder();
            _current_node.addChild( new_node );
        }
        else {
            parseNHX( _current_anotation.toString(),
                      _current_node.getLastChildNode(),
                      getTaxonomyExtraction(),
                      isReplaceUnderscores(),
                      isAllowErrorsInDistanceToParent(),
                      true,
                      isParseBeastStyleExtendedTags());
            _current_anotation = new StringBuilder();
        }
        if ( !_current_node.isRoot() ) {
            _current_node = (Protein) _current_node.getParent();
        }
        _saw_closing_paren = true;
    }

    private final void processComma() throws PhylogenyParserException, NHXFormatException, PhyloXmlDataFormatException {
        if ( !_saw_closing_paren ) {
            final Protein new_node = new Protein();
            parseNHX( _current_anotation.toString(),
                      new_node,
                      getTaxonomyExtraction(),
                      isReplaceUnderscores(),
                      isAllowErrorsInDistanceToParent(),
                      true,
                      isParseBeastStyleExtendedTags());
            if ( _current_node == null ) {
                throw new NHXFormatException( "format might not be NH or NHX" );
            }
            _current_node.addChild( new_node );
        }
        else {
            parseNHX( _current_anotation.toString(),
                      _current_node.getLastChildNode(),
                      getTaxonomyExtraction(),
                      isReplaceUnderscores(),
                      isAllowErrorsInDistanceToParent(),
                      true,
                      isParseBeastStyleExtendedTags());
        }
        _current_anotation = new StringBuilder();
        _saw_closing_paren = false;
    }

    private final Tree processOpenParen() throws PhylogenyParserException, NHXFormatException,
    PhyloXmlDataFormatException {
        Tree phy = null;
        final Protein new_node = new Protein();
        if ( _clade_level == 0 ) {
            if ( _current_phylogeny != null ) {
                phy = finishPhylogeny();
            }
            _clade_level = 1;
            _current_anotation = new StringBuilder();
            _current_phylogeny = new Tree();
            _current_phylogeny.setRoot( new_node );
        }
        else {
            ++_clade_level;
            _current_node.addChild( new_node );
        }
        _current_node = new_node;
        _saw_closing_paren = false;
        return phy;
    }

    private final static NHXParser createInstance( final Object nhx_source ) throws NHXFormatException, IOException {
        final NHXParser parser = new NHXParser();
        parser.setSource( nhx_source );
        return parser;
    }

    public final static Tree[] parse( final Object nhx_source ) throws NHXFormatException, IOException {
        return NHXParser.createInstance( nhx_source ).parse();
    }

    public final static void parseNHX( String s,
                                       final Protein node_to_annotate,
                                       final TAXONOMY_EXTRACTION taxonomy_extraction,
                                       final boolean replace_underscores,
                                       final boolean allow_errors_in_distance_to_parent,
                                       final boolean replace_bell,
                                       final boolean parse_beast_style_extended_tags ) throws NHXFormatException,
                                       PhyloXmlDataFormatException {
        if ( ( taxonomy_extraction != TAXONOMY_EXTRACTION.NO ) && replace_underscores ) {
            throw new IllegalArgumentException( "cannot extract taxonomies and replace under scores at the same time" );
        }
        if ( ( s != null ) && ( s.length() > 0 ) ) {
            if ( replace_underscores ) {
                s = s.replaceAll( "_+", " " );
            }
            s = s.replaceAll( "\\s+", " " ).trim();
            boolean is_nhx = false;
            final int ob = s.indexOf( "[" );
            if ( ob > -1 ) {
                String b = "";
                is_nhx = true;
                final int cb = s.indexOf( "]" );
                if ( cb < 0 ) {
                    throw new NHXFormatException( "error in NHX formatted data: no closing \"]\" in \"" + s + "\"" );
                }
                if ( s.indexOf( "&&NHX" ) == ( ob + 1 ) ) {
                    b = s.substring( ob + 6, cb );
                }
                else {
                    // No &&NHX and digits only: is likely to be a support value.
                    final String bracketed = s.substring( ob + 1, cb );
                    final Matcher numbers_only = NUMBERS_ONLY_PATTERN.matcher( bracketed );
                    if ( numbers_only.matches() ) {
                        b = ":" + NHXtags.SUPPORT + bracketed;
                    }
                    else if ( s.indexOf( "prob=" ) > -1 ) {
                        processMrBayes3Data( s, node_to_annotate );
                    }
                    if ( parse_beast_style_extended_tags ) {
                        processBeastStyleExtendedData( s, node_to_annotate );
                    }
                    final Matcher ewn_matcher = ENDS_WITH_NUMBER_PATTERN.matcher( s );
                    if ( ewn_matcher.find() ) {
                        b = ewn_matcher.group(1);
                    }
                }
                s = s.substring( 0, ob ) + b;
                if ( ( s.indexOf( "[" ) > -1 ) || ( s.indexOf( "]" ) > -1 ) ) {
                    throw new NHXFormatException( "error in NHX formatted data: more than one \"]\" or \"[\"" );
                }
            }
            final StringTokenizer t = new StringTokenizer( s, ":" );
          
            if ( t.countTokens() > 0 ) {
                if ( !s.startsWith( ":" ) ) {
                    if ( ( s.indexOf( BELL ) <= -1 ) || !replace_bell ) {
                        node_to_annotate.setId( t.nextToken() );
                    }
                    else {
                        node_to_annotate.setId( t.nextToken().replace( BELL, ':' ) );
                    }
                    if ( !replace_underscores && ( !is_nhx && ( taxonomy_extraction != TAXONOMY_EXTRACTION.NO ) ) ) {
//                        ParserUtils.extractTaxonomyDataFromNodeName( node_to_annotate, taxonomy_extraction );
                    	log.info("Not parsing taxonomy");
                    }
                }
                while ( t.hasMoreTokens() ) {
                    s = t.nextToken();
                    if ( ( s.indexOf( BELL ) > -1 ) && replace_bell ) {
                        s = s.replace( BELL, ':' );
                    }
                    if ( s.indexOf( '=' ) < 0 ) {
                        if ( ( node_to_annotate.getDistanceFromParent() != PhylogenyDataUtil.BRANCH_LENGTH_DEFAULT )
                                && !allow_errors_in_distance_to_parent ) {
                            throw new NHXFormatException( "error in NHX formatted data: more than one distance to parent:"
                                    + "\"" + s + "\"" );
                        }
                        node_to_annotate.setDistanceFromParent( floatValue( s, allow_errors_in_distance_to_parent ) );
                    }
                    else if ( s.startsWith( NHXtags.SPECIES_NAME ) ) {
//                        if ( !node_to_annotate.getNodeData().isHasTaxonomy() ) {
//                            node_to_annotate.getNodeData().setTaxonomy( new Taxonomy() );
//                        }
//                        node_to_annotate.getNodeData().getTaxonomy().setScientificName( s.substring( 2 ) );
                    }
                    else if ( s.startsWith( NHXtags.IS_DUPLICATION ) ) {
                        if ( ( s.charAt( 2 ) == 'Y' ) || ( s.charAt( 2 ) == 'T' ) ) {
//                            node_to_annotate.getNodeData().setEvent( Event.createSingleDuplicationEvent() );
                        	node_to_annotate.setType(Protein.NODE_TYPE_DUPLICATION);
                        }
                        else if ( ( s.charAt( 2 ) == 'N' ) || ( s.charAt( 2 ) == 'F' ) ) {
//                            node_to_annotate.getNodeData().setEvent( Event.createSingleSpeciationEvent() );
                        	node_to_annotate.setType(Protein.NODE_TYPE_SPECIATION);
                        }
                        else if ( s.charAt( 2 ) == '?' ) {
//                            node_to_annotate.getNodeData().setEvent( Event.createSingleSpeciationOrDuplicationEvent() );
                        	node_to_annotate.setType("?");
                        }
                        else {
                            throw new NHXFormatException( "error in NHX formatted data: :D=Y or :D=N or :D=?" );
                        }
                    }
                    else if ( s.startsWith( NHXtags.SUPPORT ) ) {
//                        PhylogenyMethods.setConfidence( node_to_annotate, doubleValue( s.substring( 2 ), false ) );
                    }
                    else if ( s.startsWith( NHXtags.TAXONOMY_ID ) ) {
//                        if ( !node_to_annotate.getNodeData().isHasTaxonomy() ) {
//                            node_to_annotate.getNodeData().setTaxonomy( new Taxonomy() );
//                        }
//                        node_to_annotate.getNodeData().getTaxonomy().setIdentifier( new Identifier( s.substring( 2 ) ) );
                    }
                    else if ( s.startsWith( NHXtags.SEQUENCE_ACCESSION ) ) {
//                        if ( !node_to_annotate.getNodeData().isHasSequence() ) {
//                            node_to_annotate.getNodeData().setSequence( new Sequence() );
//                        }
//                        node_to_annotate.getNodeData().getSequence()
//                        .setAccession( new Accession( s.substring( 3 ), "?" ) );
                    }
                    else if ( s.startsWith( NHXtags.GENE_NAME ) ) {
//                        if ( !node_to_annotate.getNodeData().isHasSequence() ) {
//                            node_to_annotate.getNodeData().setSequence( new Sequence() );
//                        }
//                        node_to_annotate.getNodeData().getSequence().setName( s.substring( 3 ) );
                    }
                } // while ( t.hasMoreTokens() )
            }
        }
    }

    private final static float floatValue( final String str, final boolean allow_errors ) throws NHXFormatException {
        try {
            return Float.valueOf( str ).floatValue();
        }
        catch ( final NumberFormatException ex ) {
            if ( !allow_errors ) {
                throw new NHXFormatException( "error in NH/NHX formatted data: failed to parse number from " + "\""
                        + str + "\"" );
            }
        }
        return 0.0f;
    }

    private final static boolean isBranchLengthsLikeBootstrapValues( final Tree p ) {
        final List<Protein> leaves = p.getTerminusNodes();
        final double d0 = leaves.get(0).getDistanceFromParent();
        if ( ( d0 < 10 ) || leaves.size() == 1 ) {
            return false;
        }
        for (Bioentity node : leaves) { 
            final double d = node.getDistanceFromParent();
            if ( ( d != d0 ) || ( d < 10 ) ) {
                return false;
            }
        }
        return true;
    }

    private final static void moveBranchLengthsToConfidenceValues( final Tree p ) {
        final List<Protein> leaves = p.getTerminusNodes();
        for (Protein node : leaves) { 
            PhylogenyMethods.setBootstrapConfidence( node, node.getDistanceFromParent() );
            node.setDistanceFromParent( PhylogenyDataUtil.BRANCH_LENGTH_DEFAULT );
        }
    }

    private final static void processBeastStyleExtendedData( final String s,
                                                     final Bioentity node_to_annotate )
            throws NHXFormatException {
        final Matcher ft_bs_matcher = BEAST_STYLE_EXTENDED_BOOTSTRAP_PATTERN.matcher( s );
       
        double bs = -1;
        if ( ft_bs_matcher.find() ) {
            try {
                bs = Double.parseDouble( ft_bs_matcher.group( 1 ) );
            }
            catch ( final NumberFormatException e ) {
                throw new NHXFormatException( "failed to parse bootstrap support from \""
                        + s + "\"" );
            }
            if ( bs >= 0.0 ) {
//                node_to_annotate.getBranchData()
//                .addConfidence( new Confidence( bs, "bootstrap" ) );
            }
        }
//        final Matcher ft_color_matcher = BEAST_STYLE_EXTENDED_COLOR_PATTERN.matcher( s );
//        Color c = null;
//        if ( ft_color_matcher.find() ) {
//            try {
//                c = Color.decode(ft_color_matcher.group( 1 ) );
//            }
//            catch ( final NumberFormatException e ) {
//                throw new NHXFormatException( "failed to parse color from \""
//                        + s + "\""  );
//            }
//        }
//        if ( c != null ) {
//            node_to_annotate.getBranchData().setBranchColor( new BranchColor( c ) );
//        }
    }
    
    private final static void processMrBayes3Data( final String s, final Bioentity node_to_annotate )
            throws NHXFormatException {
        double sd = -1;
        final Matcher mb_prob_sd_matcher = MB_PROB_SD_PATTERN.matcher( s );
        if ( mb_prob_sd_matcher.find() ) {
            try {
                sd = Double.parseDouble( mb_prob_sd_matcher.group( 1 ) );
            }
            catch ( final NumberFormatException e ) {
                throw new NHXFormatException( "failed to parse probability standard deviation (Mr Bayes output) from \""
                        + s + "\"" );
            }
        }
        final Matcher mb_prob_matcher = MB_PROB_PATTERN.matcher( s );
        if ( mb_prob_matcher.find() ) {
            double prob = -1;
            try {
                prob = Double.parseDouble( mb_prob_matcher.group( 1 ) );
            }
            catch ( final NumberFormatException e ) {
                throw new NHXFormatException( "failed to parse probability (Mr Bayes output) from \"" + s + "\"" );
            }
            if ( prob >= 0.0 ) {
//                if ( sd >= 0.0 ) {
//                    node_to_annotate.getBranchData()
//                    .addConfidence( new Confidence( prob, "posterior probability", sd ) );
//                }
//                else {
//                    node_to_annotate.getBranchData().addConfidence( new Confidence( prob, "posterior probability" ) );
//                }
            }
        }
        final Matcher mb_bl_matcher = MB_BL_PATTERN.matcher( s );
        if ( mb_bl_matcher.find() ) {
            float bl = -1;
            try {
                bl = Float.parseFloat( mb_bl_matcher.group( 1 ) );
            }
            catch ( final NumberFormatException e ) {
                throw new NHXFormatException( "failed to parse median branch length (Mr Bayes output) from \"" + s
                                              + "\"" );
            }
            if ( bl >= 0.0 ) {
                node_to_annotate.setDistanceFromParent( bl );
            }
        }
    }

    public String getEncoding() {
        return _encoding;
    }

    private final boolean isParseBeastStyleExtendedTags() {
        return _parse_beast_style_extended_tags;
    }

    public final void setParseBeastStyleExtendedTags( final boolean parse_beast_style_extended_tags ) {
        _parse_beast_style_extended_tags = parse_beast_style_extended_tags;
    }

    public static enum TAXONOMY_EXTRACTION {
        AGGRESSIVE, NO, PFAM_STYLE_RELAXED, PFAM_STYLE_STRICT;
    }
}
