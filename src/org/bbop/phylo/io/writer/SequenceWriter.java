
package org.bbop.phylo.io.writer;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bbop.phylo.util.PhyloUtil;

public class SequenceWriter {

    public static enum SEQ_FORMAT {
        FASTA;
    }

    public static StringBuilder toFasta( final String name, final String mol_seq, final int width ) {
        final StringBuilder sb = new StringBuilder();
        sb.append( ">" );
        sb.append( name );
        sb.append( PhyloUtil.LINE_SEPARATOR );
        if ( ( width < 1 ) || ( width >= mol_seq.length() ) ) {
            sb.append( mol_seq );
        }
        else {
            final int lines = mol_seq.length() / width;
            final int rest = mol_seq.length() - ( lines * width );
            for( int i = 0; i < lines; ++i ) {
                sb.append( mol_seq, i * width, ( i + 1 ) * width );
                if ( i < ( lines - 1 ) ) {
                    sb.append( PhyloUtil.LINE_SEPARATOR );
                }
            }
            if ( rest > 0 ) {
                sb.append( PhyloUtil.LINE_SEPARATOR );
                sb.append( mol_seq, lines * width, mol_seq.length() );
            }
        }
        return sb;
    }

    public static void toFasta( final String id, final String seq, final Writer w, final int width ) throws IOException {
        w.write( ">" );
        w.write( id );
        w.write( PhyloUtil.LINE_SEPARATOR );
        if ( ( width < 1 ) || ( width >= seq.length() ) ) {
            w.write( seq );
        }
        else {
            final int lines = seq.length() / width;
            final int rest = seq.length() - ( lines * width );
            for( int i = 0; i < lines; ++i ) {
                w.write( seq, i * width, width );
                if ( i < ( lines - 1 ) ) {
                    w.write( PhyloUtil.LINE_SEPARATOR );
                }
            }
            if ( rest > 0 ) {
                w.write( PhyloUtil.LINE_SEPARATOR );
                w.write( seq, lines * width, rest );
            }
        }
    }

    public static void writeSeqs( final Map<String, String> seqs,
                                  final File file,
                                  final SEQ_FORMAT format,
                                  final int width ) throws IOException {
        final Writer w = PhyloUtil.createBufferedWriter( file );
        SequenceWriter.writeSeqs( seqs, w, format, width );
        w.close();
    }

    public static void writeSeqs( final Map<String, String> seqs,
                                  final Writer writer,
                                  final SEQ_FORMAT format,
                                  final int width ) throws IOException {
        switch ( format ) {
            case FASTA:
            	Set<String> ids = seqs.keySet();
                for( final String id : ids ) {
                    toFasta( id, seqs.get(id), writer, width );
                    writer.write( PhyloUtil.LINE_SEPARATOR );
                }
                break;
            default:
                throw new RuntimeException( "unknown format " + format );
        }
    }
}
