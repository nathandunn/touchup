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

package org.bbop.phylo.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.bbop.phylo.model.Confidence;
import org.bbop.phylo.model.Tree;
import org.bbop.phylo.util.PhyloUtil;

import owltools.gaf.Bioentity;

public class PhylogenyMethods {

    private static boolean _order_changed;

    private PhylogenyMethods() {
        // Hidden constructor.
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public final static Tree[] readPhylogenies( final PhylogenyParser parser, final File file )
            throws IOException {
        final PhylogenyFactory factory = ParserBasedPhylogenyFactory.getInstance();
        final Tree[] trees = factory.create( file, parser );
        if ( ( trees == null ) || ( trees.length == 0 ) ) {
            throw new PhylogenyParserException( "Unable to parse phylogeny from file: " + file );
        }
        return trees;
    }

    public final static Tree[] readPhylogenies( final PhylogenyParser parser, final List<File> files )
            throws IOException {
        final List<Tree> tree_list = new ArrayList<Tree>();
        for( final File file : files ) {
            final PhylogenyFactory factory = ParserBasedPhylogenyFactory.getInstance();
            final Tree[] trees = factory.create( file, parser );
            if ( ( trees == null ) || ( trees.length == 0 ) ) {
                throw new PhylogenyParserException( "Unable to parse phylogeny from file: " + file );
            }
            tree_list.addAll( Arrays.asList( trees ) );
        }
        return tree_list.toArray( new Tree[ tree_list.size() ] );
    }

    private static enum NDF {
                             NodeName( "NN" ),
                             TaxonomyCode( "TC" ),
                             TaxonomyCommonName( "CN" ),
                             TaxonomyScientificName( "TS" ),
                             TaxonomyIdentifier( "TI" ),
                             TaxonomySynonym( "SY" ),
                             SequenceName( "SN" ),
                             GeneName( "GN" ),
                             SequenceSymbol( "SS" ),
                             SequenceAccession( "SA" ),
                             Domain( "DO" ),
                             Annotation( "AN" ),
                             CrossRef( "XR" ),
                             BinaryCharacter( "BC" ),
                             MolecularSequence( "MS" );

        private final String _text;

        NDF( final String text ) {
            _text = text;
        }

        public static NDF fromString( final String text ) {
            for( final NDF n : NDF.values() ) {
                if ( text.startsWith( n._text ) ) {
                    return n;
                }
            }
            return null;
        }
    }

    static double addPhylogenyDistances( final float a, final float b ) {
        if ( ( a >= 0.0 ) && ( b >= 0.0 ) ) {
            return a + b;
        }
        else if ( a >= 0.0 ) {
            return a;
        }
        else if ( b >= 0.0 ) {
            return b;
        }
        return PhylogenyDataUtil.BRANCH_LENGTH_DEFAULT;
    }

    static float calculateDistanceToAncestor( final Bioentity anc, Bioentity desc ) {
        float d = 0;
        boolean all_default = true;
        while ( anc != desc ) {
            if ( desc.getDistanceFromParent() != PhylogenyDataUtil.BRANCH_LENGTH_DEFAULT ) {
                d += desc.getDistanceFromParent();
                if ( all_default ) {
                    all_default = false;
                }
            }
            desc = desc.getParent();
        }
        if ( all_default ) {
            return PhylogenyDataUtil.BRANCH_LENGTH_DEFAULT;
        }
        return d;
    }

    /**
     * Calculates the distance between Bioentitys n1 and n2.
     * PRECONDITION: n1 is a descendant of n2.
     *
     * @param n1
     *            a descendant of n2
     * @param n2
     * @return distance between n1 and n2
     */
    private static float getDistance( Bioentity n1, final Bioentity n2 ) {
        float d = 0.0f;
        while ( n1 != n2 ) {
            if ( n1.getDistanceFromParent() > 0.0 ) {
                d += n1.getDistanceFromParent();
            }
            n1 = n1.getParent();
        }
        return d;
    }

    private static boolean match( final String s,
                                  final String query,
                                  final boolean case_sensitive,
                                  final boolean partial,
                                  final boolean regex ) {
        if ( PhyloUtil.isEmpty( s ) || PhyloUtil.isEmpty( query ) ) {
            return false;
        }
        String my_s = s.trim();
        String my_query = query.trim();
        if ( !case_sensitive && !regex ) {
            my_s = my_s.toLowerCase();
            my_query = my_query.toLowerCase();
        }
        if ( regex ) {
            Pattern p = null;
            try {
                if ( case_sensitive ) {
                    p = Pattern.compile( my_query );
                }
                else {
                    p = Pattern.compile( my_query, Pattern.CASE_INSENSITIVE );
                }
            }
            catch ( final PatternSyntaxException e ) {
                return false;
            }
            if ( p != null ) {
                return p.matcher( my_s ).find();
            }
            else {
                return false;
            }
        }
        else if ( partial ) {
            return my_s.indexOf( my_query ) >= 0;
        }
        else {
            Pattern p = null;
            try {
                p = Pattern.compile( "(\\b|_)" + Pattern.quote( my_query ) + "(\\b|_)" );
            }
            catch ( final PatternSyntaxException e ) {
                return false;
            }
            if ( p != null ) {
                return p.matcher( my_s ).find();
            }
            else {
                return false;
            }
        }
    }

    public static enum DESCENDANT_SORT_PRIORITY {
                                                 NODE_NAME,
                                                 SEQUENCE,
                                                 TAXONOMY;
    }

    public static enum PhylogenyNodeField {
                                           CLADE_NAME,
                                           SEQUENCE_NAME,
                                           SEQUENCE_SYMBOL,
                                           TAXONOMY_CODE,
                                           TAXONOMY_COMMON_NAME,
                                           TAXONOMY_ID,
                                           TAXONOMY_ID_UNIPROT_1,
                                           TAXONOMY_ID_UNIPROT_2,
                                           TAXONOMY_SCIENTIFIC_NAME;
    }

    /**
     * Convenience method.
     * Sets value for the first confidence value (created if not present, values overwritten otherwise).
     */
    public static void setBootstrapConfidence( final Bioentity node, final double bootstrap_confidence_value ) {
        setConfidence( node, bootstrap_confidence_value, "bootstrap" );
    }

    /**
     * Convenience method.
     * Sets value for the first confidence value (created if not present, values overwritten otherwise).
     */
    public static void setConfidence( final Bioentity node, final double confidence_value, final String type ) {
        Confidence c = null;
//        if ( node.getBranchData().getNumberOfConfidences() > 0 ) {
//            c = node.getBranchData().getConfidence( 0 );
//        }
//        else {
            c = new Confidence();
//            node.getBranchData().addConfidence( c );
//        }
        c.setType( type );
        c.setValue( confidence_value );
    }


}
