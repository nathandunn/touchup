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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bbop.phylo.io.nhx.NHXtags;
import org.bbop.phylo.io.phyloxml.PhyloXmlDataFormatException;
import org.bbop.phylo.io.phyloxml.PhyloXmlMapping;
import org.bbop.phylo.io.phyloxml.PhyloXmlUtil;
import org.bbop.phylo.util.PhyloUtil;

public class Taxonomy implements PhylogenyData, MultipleUris, Comparable<Taxonomy> {

    private String       _scientific_name;
    private String       _common_name;
    private List<String> _synonyms;
    private String       _authority;
    private String   _identifier;
    private String       _taxonomy_code;
    private String       _rank;
    private List<Uri>    _uris;
    private List<String> _lineage;
    public final static Set<String>              TAXONOMY_RANKS_SET  = new HashSet<String>();

    public Taxonomy() {
        init();
    }

    @Override
    public StringBuffer asSimpleText() {
        return asText();
    }

    @Override
    public Uri getUri( final int index ) {
        return getUris().get( index );
    }

    @Override
    public void addUri( final Uri uri ) {
        if ( getUris() == null ) {
            setUris( new ArrayList<Uri>() );
        }
        getUris().add( uri );
    }

    @Override
    public StringBuffer asText() {
        final StringBuffer sb = new StringBuffer();
        if ( getIdentifier() != null ) {
            sb.append( "[" );
            sb.append( getIdentifier() );
            sb.append( "]" );
        }
        if ( !PhyloUtil.isEmpty( getTaxonomyCode() ) ) {
            if ( sb.length() > 0 ) {
                sb.append( " " );
            }
            sb.append( "[" );
            sb.append( getTaxonomyCode() );
            sb.append( "]" );
        }
        if ( !PhyloUtil.isEmpty( getScientificName() ) ) {
            if ( sb.length() > 0 ) {
                sb.append( " " );
            }
            sb.append( getScientificName() );
            if ( !PhyloUtil.isEmpty( getAuthority() ) ) {
                sb.append( " (" );
                sb.append( getAuthority() );
                sb.append( ")" );
            }
        }
        if ( !PhyloUtil.isEmpty( getCommonName() ) ) {
            if ( sb.length() > 0 ) {
                sb.append( " " );
            }
            sb.append( getCommonName() );
        }
        return sb;
    }

    @Override
    public PhylogenyData copy() {
        final Taxonomy t = new Taxonomy();
        try {
            t.setTaxonomyCode( getTaxonomyCode() );
        }
        catch ( final PhyloXmlDataFormatException e ) {
            e.printStackTrace();
        }
        t.setScientificName( getScientificName() );
        t.setCommonName( getCommonName() );
        t.setAuthority( getAuthority() );
        for( final String syn : getSynonyms() ) {
            t.getSynonyms().add( syn );
        }
        if ( getIdentifier() != null ) {
            t.setIdentifier( getIdentifier() );
        }
        else {
            t.setIdentifier( null );
        }
        try {
            t.setRank( new String( getRank() ) );
        }
        catch ( final PhyloXmlDataFormatException e ) {
            e.printStackTrace();
        }
        if ( getUris() != null ) {
            t.setUris( new ArrayList<Uri>() );
            for( final Uri uri : getUris() ) {
                if ( uri != null ) {
                    t.getUris().add( uri );
                }
            }
        }
        if ( getLineage() != null ) {
            t.setLineage( new ArrayList<String>() );
            for( final String l : getLineage() ) {
                if ( l != null ) {
                    t.getLineage().add( l );
                }
            }
        }
        return t;
    }

    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        else if ( o == null ) {
            return false;
        }
        else if ( o.getClass() != this.getClass() ) {
            throw new IllegalArgumentException( "attempt to check [" + this.getClass() + "] equality to " + o + " ["
                    + o.getClass() + "]" );
        }
        else {
            return isEqual( ( Taxonomy ) o );
        }
    }

    public String getAuthority() {
        return _authority;
    }

    public String getCommonName() {
        return _common_name;
    }

    public String getIdentifier() {
        return _identifier;
    }

    public String getRank() {
        return _rank;
    }

    public String getScientificName() {
        return _scientific_name;
    }

    public List<String> getSynonyms() {
        if ( _synonyms == null ) {
            _synonyms = new ArrayList<String>();
        }
        return _synonyms;
    }

    public String getTaxonomyCode() {
        return _taxonomy_code;
    }

    @Override
    public List<Uri> getUris() {
        return _uris;
    }

    @Override
    public int hashCode() {
        if ( ( getIdentifier() != null ) && !PhyloUtil.isEmpty( getIdentifier() ) ) {
            return getIdentifier().hashCode();
        }
        else if ( !PhyloUtil.isEmpty( getTaxonomyCode() ) ) {
            return getTaxonomyCode().hashCode();
        }
        else if ( !PhyloUtil.isEmpty( getScientificName() ) ) {
            if ( !PhyloUtil.isEmpty( getAuthority() ) ) {
                return ( getScientificName().toLowerCase() + getAuthority().toLowerCase() ).hashCode();
            }
            return getScientificName().toLowerCase().hashCode();
        }
        else {
            return getCommonName().toLowerCase().hashCode();
        }
    }

    public void init() {
        setScientificName( "" );
        setCommonName( "" );
        setIdentifier( null );
        try {
            setRank( "" );
        }
        catch ( final PhyloXmlDataFormatException e ) {
            e.printStackTrace();
        }
        try {
            setTaxonomyCode( "" );
        }
        catch ( final PhyloXmlDataFormatException e ) {
            e.printStackTrace();
        }
        setAuthority( "" );
        setSynonyms( null );
        setUris( null );
        setLineage( null );
    }

    public boolean isEmpty() {
        return ( ( getIdentifier() == null ) && PhyloUtil.isEmpty( getTaxonomyCode() )
                && PhyloUtil.isEmpty( getCommonName() ) && PhyloUtil.isEmpty( getScientificName() )
                && PhyloUtil.isEmpty( _lineage ) );
    }

    /**
     *
     * If this and taxonomy 'data' has an identifier, comparison will be based on that.
     * Otherwise,  if this and taxonomy 'data' has a code, comparison will be based on that.
     * Otherwise,  if Taxonomy 'data' has a scientific name, comparison will be
     * based on that (case insensitive!).
     * Otherwise,  if Taxonomy 'data' has a common  name, comparison will be
     * based on that (case insensitive!).
     * (Note. This is important and should not be change without a very good reason.)
     *
     */
    @Override
    public boolean isEqual( final PhylogenyData data ) {
        if ( this == data ) {
            return true;
        }
        final Taxonomy tax = ( Taxonomy ) data;
        if ( ( getIdentifier() != null ) && ( tax.getIdentifier() != null )
                && !PhyloUtil.isEmpty( getIdentifier() )
                && !PhyloUtil.isEmpty( tax.getIdentifier() ) ) {
            return getIdentifier().equals( tax.getIdentifier() );
        }
        else if ( !PhyloUtil.isEmpty( getTaxonomyCode() ) && !PhyloUtil.isEmpty( tax.getTaxonomyCode() ) ) {
            return getTaxonomyCode().equals( tax.getTaxonomyCode() );
        }
        else if ( !PhyloUtil.isEmpty( getScientificName() ) && !PhyloUtil.isEmpty( tax.getScientificName() ) ) {
            if ( !PhyloUtil.isEmpty( getAuthority() ) && !PhyloUtil.isEmpty( tax.getAuthority() ) ) {
                return ( getScientificName().equalsIgnoreCase( tax.getScientificName() ) )
                        && ( getAuthority().equalsIgnoreCase( tax.getAuthority() ) );
            }
            return getScientificName().equalsIgnoreCase( tax.getScientificName() );
        }
        else if ( !PhyloUtil.isEmpty( getCommonName() ) && !PhyloUtil.isEmpty( tax.getCommonName() ) ) {
            return getCommonName().equalsIgnoreCase( tax.getCommonName() );
        }
        //throw new RuntimeException( "comparison not possible with empty fields" );
        return false;
    }

    public void setAuthority( final String authority ) {
        _authority = authority;
    }

    public void setCommonName( final String common_name ) {
        _common_name = common_name;
    }

    public void setIdentifier( final String identifier ) {
        _identifier = identifier;
    }

    public void setRank( final String rank ) throws PhyloXmlDataFormatException {
        if ( !PhyloUtil.isEmpty( rank ) && !TAXONOMY_RANKS_SET.contains( rank ) ) {
            throw new PhyloXmlDataFormatException( "illegal rank: [" + rank + "]" );
        }
        _rank = rank;
    }

    public void setScientificName( final String scientific_name ) {
        _scientific_name = scientific_name;
    }

    private void setSynonyms( final List<String> synonyms ) {
        _synonyms = synonyms;
    }

    public void setTaxonomyCode( String taxonomy_code ) throws PhyloXmlDataFormatException {
        if ( !PhyloUtil.isEmpty( taxonomy_code )
                && !PhyloXmlUtil.TAXOMONY_CODE_PATTERN.matcher( taxonomy_code ).matches() ) {
            throw new PhyloXmlDataFormatException( "illegal taxonomy code: [" + taxonomy_code + "]" );
        }
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //TODO FIXME (added on 13-11-18) remove me eventually
        if ( taxonomy_code.equals( "ACIBL" ) ) {
            taxonomy_code = "KORVE";
        }
        else if ( taxonomy_code.equals( "PYRKO" ) ) {
            taxonomy_code = "THEKO";
        }
        //TODO FIXME (added on 13-11-18) remove me eventually
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        _taxonomy_code = taxonomy_code;
    }

    @Override
    public void setUris( final List<Uri> uris ) {
        _uris = uris;
    }

    @Override
    public StringBuffer toNHX() {
        final StringBuffer sb = new StringBuffer();
        if ( getIdentifier() != null ) {
            sb.append( ':' + NHXtags.TAXONOMY_ID );
            sb.append( PhyloUtil.replaceIllegalNhxCharacters( getIdentifier() ) );
        }
        final StringBuffer species = new StringBuffer();
        if ( !PhyloUtil.isEmpty( getTaxonomyCode() ) ) {
            species.append( PhyloUtil.replaceIllegalNhxCharacters( getTaxonomyCode() ) );
        }
        if ( !PhyloUtil.isEmpty( getScientificName() ) ) {
            PhyloUtil.appendSeparatorIfNotEmpty( species, '|' );
            species.append( PhyloUtil.replaceIllegalNhxCharacters( getScientificName() ) );
        }
        if ( !PhyloUtil.isEmpty( getCommonName() ) ) {
            PhyloUtil.appendSeparatorIfNotEmpty( species, '|' );
            species.append( PhyloUtil.replaceIllegalNhxCharacters( getCommonName() ) );
        }
        if ( species.length() > 0 ) {
            sb.append( ':' + NHXtags.SPECIES_NAME );
            sb.append( species );
        }
        return sb;
    }

    @Override
    public void toPhyloXML( final Writer writer, final int level, final String indentation ) throws IOException {
        if ( isEmpty() ) {
            return;
        }
        writer.write( PhyloUtil.LINE_SEPARATOR );
        writer.write( indentation );
        PhylogenyDataUtil.appendOpen( writer, PhyloXmlMapping.TAXONOMY );
        if ( ( getIdentifier() != null ) && !PhyloUtil.isEmpty( getIdentifier() ) ) {
            idToPhyloXML( writer, level, indentation );
        }
        if ( !PhyloUtil.isEmpty( getTaxonomyCode() ) ) {
            PhylogenyDataUtil.appendElement( writer, PhyloXmlMapping.TAXONOMY_CODE, getTaxonomyCode(), indentation );
        }
        if ( !PhyloUtil.isEmpty( getScientificName() ) ) {
            PhylogenyDataUtil.appendElement( writer,
                                             PhyloXmlMapping.TAXONOMY_SCIENTIFIC_NAME,
                                             getScientificName(),
                                             indentation );
        }
        if ( !PhyloUtil.isEmpty( getAuthority() ) ) {
            PhylogenyDataUtil.appendElement( writer, PhyloXmlMapping.TAXONOMY_AUTHORITY, getAuthority(), indentation );
        }
        if ( !PhyloUtil.isEmpty( getCommonName() ) ) {
            PhylogenyDataUtil.appendElement( writer,
                                             PhyloXmlMapping.TAXONOMY_COMMON_NAME,
                                             getCommonName(),
                                             indentation );
        }
        if ( _synonyms != null ) {
            for( final String syn : getSynonyms() ) {
                if ( !PhyloUtil.isEmpty( syn ) ) {
                    PhylogenyDataUtil.appendElement( writer, PhyloXmlMapping.TAXONOMY_SYNONYM, syn, indentation );
                }
            }
        }
        if ( !PhyloUtil.isEmpty( getRank() ) ) {
            PhylogenyDataUtil.appendElement( writer, PhyloXmlMapping.TAXONOMY_RANK, getRank(), indentation );
        }
        if ( getUris() != null ) {
            for( final Uri uri : getUris() ) {
                if ( uri != null ) {
                    uri.toPhyloXML( writer, level, indentation );
                }
            }
        }
        if ( getLineage() != null ) {
            final StringBuilder sb = new StringBuilder();
            boolean first = true;
            for( final String lin : getLineage() ) {
                if ( !PhyloUtil.isEmpty( lin ) ) {
                    if ( first ) {
                        first = false;
                    }
                    else {
                        sb.append( "," );
                    }
                    sb.append( lin );
                }
            }
            if ( sb.length() > 0 ) {
                PhylogenyDataUtil.appendElement( writer, PhyloXmlMapping.TAXONOMY_LINEAGE, sb.toString(), indentation );
            }
        }
        writer.write( PhyloUtil.LINE_SEPARATOR );
        writer.write( indentation );
        PhylogenyDataUtil.appendClose( writer, PhyloXmlMapping.TAXONOMY );
    }

    @Override
    public String toString() {
        return asText().toString();
    }

    @Override
    public int compareTo( final Taxonomy o ) {
        if ( equals( o ) ) {
            return 0;
        }
        if ( ( getIdentifier() != null ) && ( o.getIdentifier() != null )
                && !PhyloUtil.isEmpty( getIdentifier() )
                && !PhyloUtil.isEmpty( o.getIdentifier() ) ) {
            final int x = getIdentifier().compareTo( o.getIdentifier() );
            if ( x != 0 ) {
                return x;
            }
        }
        if ( !PhyloUtil.isEmpty( getScientificName() ) && !PhyloUtil.isEmpty( o.getScientificName() ) ) {
            return getScientificName().compareToIgnoreCase( o.getScientificName() );
        }
        if ( !PhyloUtil.isEmpty( getCommonName() ) && !PhyloUtil.isEmpty( o.getCommonName() ) ) {
            return getCommonName().compareToIgnoreCase( o.getCommonName() );
        }
        if ( !PhyloUtil.isEmpty( getTaxonomyCode() ) && !PhyloUtil.isEmpty( o.getTaxonomyCode() ) ) {
            return getTaxonomyCode().compareToIgnoreCase( o.getTaxonomyCode() );
        }
        if ( ( getIdentifier() != null ) && ( o.getIdentifier() != null )
                && !PhyloUtil.isEmpty( getIdentifier() )
                && !PhyloUtil.isEmpty( o.getIdentifier() ) ) {
            return getIdentifier().compareTo( o.getIdentifier() );
        }
        return 1;
    }

    public void setLineage( final List<String> lineage ) {
        _lineage = lineage;
    }

    public List<String> getLineage() {
        return _lineage;
    }
    
    private void idToPhyloXML( Writer writer, int level, String indentation ) throws IOException {
    	String id = getIdentifier();
        if ( id.contains(":") ) {
            PhylogenyDataUtil.appendElement( writer,
                                             PhyloXmlMapping.IDENTIFIER,
                                             id.substring(id.indexOf(':') + 1),
                                             PhyloXmlMapping.IDENTIFIER_PROVIDER_ATTR,
                                             id.substring(0, id.indexOf(':')),
                                             indentation );
        }
        else {
            PhylogenyDataUtil.appendElement( writer, PhyloXmlMapping.IDENTIFIER, id, indentation );
        }
    }


}
