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

import java.awt.Graphics;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.bbop.phylo.io.writer.PhylogenyWriter;
import org.bbop.phylo.util.PhyloUtil;

public final class PhylogenyDataUtil {

    /** Value of -1024.0 is used as default value. */
    public final static float BRANCH_LENGTH_DEFAULT = -1024.0f;

    public static void appendClose( final Writer w, final String element_name ) throws IOException {
        w.write( "</" );
        w.write( element_name );
        w.write( ">" );
    }

    public static void appendElement( final Writer w, final String element_name, final String value )
            throws IOException {
        appendOpen( w, element_name );
        w.write( replaceIllegalXmlCharacters( value ) );
        appendClose( w, element_name );
    }

    public static void appendElement( final Writer w,
                                      final String element_name,
                                      final String value,
                                      final String indentation ) throws IOException {
        w.write( PhyloUtil.LINE_SEPARATOR );
        w.write( indentation );
        w.write( PhylogenyWriter.PHYLO_XML_INTENDATION_BASE );
        // Something like this replacement needs to be done in a more systematic manner.
        appendElement( w, element_name, value );
    }

    public static void appendElement( final Writer w,
                                      final String element_name,
                                      final String value,
                                      final String attribute_name,
                                      final String attribute_value ) throws IOException {
        appendOpen( w, element_name, attribute_name, attribute_value );
        w.write( replaceIllegalXmlCharacters( value ) );
        appendClose( w, element_name );
    }

    public static void appendElement( final Writer w,
                                      final String element_name,
                                      final String value,
                                      final String attribute_name,
                                      final String attribute_value,
                                      final String indentation ) throws IOException {
        w.write( PhyloUtil.LINE_SEPARATOR );
        w.write( indentation );
        w.write( PhylogenyWriter.PHYLO_XML_INTENDATION_BASE );
        appendOpen( w, element_name, attribute_name, attribute_value );
        w.write( replaceIllegalXmlCharacters( value ) );
        appendClose( w, element_name );
    }

    public static void appendElement( final Writer w,
                                      final String element_name,
                                      final String value,
                                      final String attribute1_name,
                                      final String attribute1_value,
                                      final String attribute2_name,
                                      final String attribute2_value,
                                      final String indentation ) throws IOException {
        w.write( PhyloUtil.LINE_SEPARATOR );
        w.write( indentation );
        w.write( PhylogenyWriter.PHYLO_XML_INTENDATION_BASE );
        appendOpen( w, element_name, attribute1_name, attribute1_value, attribute2_name, attribute2_value );
        w.write( replaceIllegalXmlCharacters( value ) );
        appendClose( w, element_name );
    }

    public static void appendElement( final Writer w,
                                      final String element_name,
                                      final String value,
                                      final String attribute1_name,
                                      final String attribute1_value,
                                      final String attribute2_name,
                                      final String attribute2_value ) throws IOException {
        appendOpen( w, element_name, attribute1_name, attribute1_value, attribute2_name, attribute2_value );
        w.write( replaceIllegalXmlCharacters( value ) );
        appendClose( w, element_name );
    }

    public static void appendElement( final Writer w,
                                      final String element_name,
                                      final String attribute1_name,
                                      final String attribute1_value,
                                      final String attribute2_name,
                                      final String attribute2_value,
                                      final String attribute3_name,
                                      final String attribute3_value,
                                      final String attribute4_name,
                                      final String attribute4_value,
                                      final String indentation ) throws IOException {
        w.write( PhyloUtil.LINE_SEPARATOR );
        w.write( indentation );
        appendOpen( w,
                    element_name,
                    attribute1_name,
                    attribute1_value,
                    attribute2_name,
                    attribute2_value,
                    attribute3_name,
                    attribute3_value,
                    attribute4_name,
                    attribute4_value );
        appendClose( w, element_name );
    }

    public static void appendElement( final Writer w,
                                      final String element_name,
                                      final String value,
                                      final String attribute1_name,
                                      final String attribute1_value,
                                      final String attribute2_name,
                                      final String attribute2_value,
                                      final String attribute3_name,
                                      final String attribute3_value,
                                      final String attribute4_name,
                                      final String attribute4_value,
                                      final String attribute5_name,
                                      final String attribute5_value,
                                      final String indentation ) throws IOException {
        w.write( PhyloUtil.LINE_SEPARATOR );
        w.write( indentation );
        w.write( PhylogenyWriter.PHYLO_XML_INTENDATION_BASE );
        appendOpen( w,
                    element_name,
                    attribute1_name,
                    attribute1_value,
                    attribute2_name,
                    attribute2_value,
                    attribute3_name,
                    attribute3_value,
                    attribute4_name,
                    attribute4_value,
                    attribute5_name,
                    attribute5_value );
        w.write( replaceIllegalXmlCharacters( value ) );
        appendClose( w, element_name );
    }

    public static void appendOpen( final Writer w, final String element_name ) throws IOException {
        w.write( "<" );
        w.write( element_name );
        w.write( ">" );
    }

    public static void appendOpen( final Writer w,
                                   final String element_name,
                                   final String attribute_name,
                                   final String attribute_value ) throws IOException {
        w.write( "<" );
        w.write( element_name );
        if ( !PhyloUtil.isEmpty( attribute_value ) ) {
            w.write( " " );
            w.write( attribute_name );
            w.write( "=\"" );
            w.write( attribute_value );
            w.write( "\"" );
        }
        w.write( ">" );
    }

    public static void appendOpen( final Writer w,
                                   final String element_name,
                                   final String attribute1_name,
                                   final String attribute1_value,
                                   final String attribute2_name,
                                   final String attribute2_value ) throws IOException {
        w.write( "<" );
        w.write( element_name );
        if ( !PhyloUtil.isEmpty( attribute1_value ) ) {
            w.write( " " );
            w.write( attribute1_name );
            w.write( "=\"" );
            w.write( attribute1_value );
            w.write( "\"" );
        }
        if ( !PhyloUtil.isEmpty( attribute2_value ) ) {
            w.write( " " );
            w.write( attribute2_name );
            w.write( "=\"" );
            w.write( attribute2_value );
            w.write( "\"" );
        }
        w.write( ">" );
    }

    public static void appendOpen( final Writer w,
                                   final String element_name,
                                   final String attribute1_name,
                                   final String attribute1_value,
                                   final String attribute2_name,
                                   final String attribute2_value,
                                   final String attribute3_name,
                                   final String attribute3_value ) throws IOException {
        w.write( "<" );
        w.write( element_name );
        if ( !PhyloUtil.isEmpty( attribute1_value ) ) {
            w.write( " " );
            w.write( attribute1_name );
            w.write( "=\"" );
            w.write( attribute1_value );
            w.write( "\"" );
        }
        if ( !PhyloUtil.isEmpty( attribute2_value ) ) {
            w.write( " " );
            w.write( attribute2_name );
            w.write( "=\"" );
            w.write( attribute2_value );
            w.write( "\"" );
        }
        if ( !PhyloUtil.isEmpty( attribute2_value ) ) {
            w.write( " " );
            w.write( attribute3_name );
            w.write( "=\"" );
            w.write( attribute3_value );
            w.write( "\"" );
        }
        w.write( ">" );
    }

    public static void appendOpen( final Writer w,
                                   final String element_name,
                                   final String attribute1_name,
                                   final String attribute1_value,
                                   final String attribute2_name,
                                   final String attribute2_value,
                                   final String attribute3_name,
                                   final String attribute3_value,
                                   final String attribute4_name,
                                   final String attribute4_value ) throws IOException {
        w.write( "<" );
        w.write( element_name );
        if ( !PhyloUtil.isEmpty( attribute1_value ) ) {
            w.write( " " );
            w.write( attribute1_name );
            w.write( "=\"" );
            w.write( attribute1_value );
            w.write( "\"" );
        }
        if ( !PhyloUtil.isEmpty( attribute2_value ) ) {
            w.write( " " );
            w.write( attribute2_name );
            w.write( "=\"" );
            w.write( attribute2_value );
            w.write( "\"" );
        }
        if ( !PhyloUtil.isEmpty( attribute3_value ) ) {
            w.write( " " );
            w.write( attribute3_name );
            w.write( "=\"" );
            w.write( attribute3_value );
            w.write( "\"" );
        }
        if ( !PhyloUtil.isEmpty( attribute4_value ) ) {
            w.write( " " );
            w.write( attribute4_name );
            w.write( "=\"" );
            w.write( attribute4_value );
            w.write( "\"" );
        }
        w.write( ">" );
    }

    public static void appendOpen( final Writer w,
                                   final String element_name,
                                   final String attribute1_name,
                                   final String attribute1_value,
                                   final String attribute2_name,
                                   final String attribute2_value,
                                   final String attribute3_name,
                                   final String attribute3_value,
                                   final String attribute4_name,
                                   final String attribute4_value,
                                   final String attribute5_name,
                                   final String attribute5_value ) throws IOException {
        w.write( "<" );
        w.write( element_name );
        if ( !PhyloUtil.isEmpty( attribute1_value ) ) {
            w.write( " " );
            w.write( attribute1_name );
            w.write( "=\"" );
            w.write( attribute1_value );
            w.write( "\"" );
        }
        if ( !PhyloUtil.isEmpty( attribute2_value ) ) {
            w.write( " " );
            w.write( attribute2_name );
            w.write( "=\"" );
            w.write( attribute2_value );
            w.write( "\"" );
        }
        if ( !PhyloUtil.isEmpty( attribute3_value ) ) {
            w.write( " " );
            w.write( attribute3_name );
            w.write( "=\"" );
            w.write( attribute3_value );
            w.write( "\"" );
        }
        if ( !PhyloUtil.isEmpty( attribute4_value ) ) {
            w.write( " " );
            w.write( attribute4_name );
            w.write( "=\"" );
            w.write( attribute4_value );
            w.write( "\"" );
        }
        if ( !PhyloUtil.isEmpty( attribute5_value ) ) {
            w.write( " " );
            w.write( attribute5_name );
            w.write( "=\"" );
            w.write( attribute5_value );
            w.write( "\"" );
        }
        w.write( ">" );
    }

    /**
     * Creates a deep copy of ArrayList of PhylogenyData objects.
     *
     * @param list
     *            an ArrayList of PhylogenyData objects
     * @return a deep copy of ArrayList list
     */
    public static ArrayList<PhylogenyData> copy( final ArrayList<PhylogenyData> list ) {
        final ArrayList<PhylogenyData> l = new ArrayList<PhylogenyData>( list.size() );
        for( int i = 0; i < list.size(); ++i ) {
            l.add( ( list.get( i ) ).copy() );
        }
        return l;
    }

    public static void drawLine( final double x1, final double y1, final double x2, final double y2, final Graphics g ) {
        g.drawLine( PhyloUtil.roundToInt( x1 ),
                    PhyloUtil.roundToInt( y1 ),
                    PhyloUtil.roundToInt( x2 ),
                    PhyloUtil.roundToInt( y2 ) );
    }

    public static String replaceIllegalXmlCharacters( final String value ) {
        String v = value.replaceAll( "&", "&amp;" );
        v = v.replaceAll( "<", "&lt;" );
        v = v.replaceAll( ">", "&gt;" );
        v = v.replaceAll( "'", "&apos;" );
        v = v.replaceAll( "\"", "&quot;" );
        return v;
    }
}
