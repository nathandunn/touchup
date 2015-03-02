package org.bbop.phylo.panther;

import org.apache.log4j.Logger;
import org.bbop.phylo.touchup.Constant;
import org.bbop.phylo.util.TaxonFinder;
import owltools.gaf.Bioentity;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

public class ParsingHack {

    private static final Logger log = Logger.getLogger(ParsingHack.class);

    private static String dbNameHack(String name) {
		/* The GO database is not using the suffix */
        String revision = name;
        if (name.equals("UniProtKB/Swiss-Prot") ||
                name.equals("Uniprot") ||
                name.equals("Gene") ||
                name.equals("EnsemblGenome")) {
            revision = "UniProtKB";
        }
        else if (name.equals("ENTREZ")) {
            revision = "RefSeq";
        }
        else if (name.equals("ECOLI")) {
            revision = "EcoCyc";
        }
        else if (name.equals("GeneDB_Spombe")) {
            revision = "PomBase";
        }
        else if (name.equals("FlyBase")) {
            revision = "FB";
        }
        return revision;
    }

    private static String dbIdHack(String db, String id, String acc) {
        String revision = id;
        if (db.equals("UniProtKB")) {
            if (id.endsWith("gn")) {
                revision = id.substring(0, id.length() - "gn".length());
            } else if (acc != null) {
                // Use the UniProt ID
                revision = acc;
            }
        }
        else if (db.equals("TAIR")) {
            revision = id.toUpperCase();
        }
        return revision;
    }

    private static final String DELIMITER = "\t";
    private static final String DELIM_QUOTE = "'";
    private static final String DELIM_NEW_LINE = "\n";

    public static List<List<String>> parsePantherAttr(List<String> tableContents) {
        if (null == tableContents) {
            return null;
        }

        if (0 == tableContents.size()) {
            return null;
        }
        List<List<String>> rows = new ArrayList<List<String>>();
        List<String> columns;
        List<String> modifiedCols;
        int numCols = tokenize(tableContents.get(0), DELIMITER).size();
        int i = 0;
        while (i < tableContents.size()) {
            // Remove new line character from end
            String line = tableContents.get(i);
            if (line.endsWith(DELIM_NEW_LINE)) {
                tableContents.set(i, line.substring(0, line.length() - 1));
            }
            columns = tokenize(tableContents.get(i), DELIMITER);
            modifiedCols = removeQuotes(columns);
            if (numCols != modifiedCols.size()) {
                return null;
            }
            i++;
            rows.add(modifiedCols);
        }
        return rows;
    }

    static Bioentity findThatNode(String row) {
        IDmap mapper = IDmap.inst();
        Bioentity gene = null;
        String paint_id = parseANid(row);

        if (paint_id != null)
            gene = mapper.getGeneByANId(paint_id);
        if (gene == null) {
            String [] db_source = getDBparts(row);
            if (db_source == null || db_source.length == 0) {
                log.error("Unable to parse " + row);
                return null;
            }
            String db = dbNameHack(db_source[0]);
            String [] seq_source = getSeqParts(row);
            String id =dbIdHack(db, db_source[1], seq_source[1]);
            gene = mapper.getGeneByDbId(db + ':' + id);
            if (gene == null) {
                if (seq_source.length >= 2) {
                    List<Bioentity> genes = mapper.getGenesBySeqId(seq_source[0], seq_source[1]);
                    if (genes != null) {
                        for (Bioentity check : genes) {
                            if (check.getDb().equals(db) && check.getLocalId().equals(id)) {
                                gene = check;
                            }
                        }
                    }
                }
            }
        }
        if (gene == null)
            log.warn("Unable to locate node for " + row);
        return gene;
    }

    static void parseIDstr(Bioentity node, String name) {
        String paint_id = parseANid(name);
        if (paint_id != null)
            node.setPaintId(paint_id);
        else {
            String[] parts = getParts(name);
            if (parts != null && parts.length > 0) {
                node.addSpeciesLabel(parts[0]);
                String taxon = TaxonFinder.getTaxonID(parts[0]);
                if (taxon != null) {
                    node.setNcbiTaxonId(taxon);
                } else {
                    log.debug("no taxon found for " + parts[0]);
                }
                String[] db_source = getDBparts(name);
                String[] seq_source = getSeqParts(name);
			/*
			 * Standard order from PANTHER Database is
			 * SPECI|DbGene=gene_id|ProteinSeq=protein_id
			 * There may be multiple genes all with the same Protein ID
			 */
                if (db_source != null && db_source.length >= 2) {
                    node.setDb(dbNameHack(db_source[0]));
                    node.setId(node.getDb() + ':' + dbIdHack(node.getDb(), db_source[1], seq_source[1]));
                    if (!db_source[1].equals(node.getLocalId())) {
                        if (node.getSymbol() == null)
                            node.setSymbol(db_source[1]);
                        else
                            node.addSynonym(db_source[1]);
                    }
                    IDmap.inst().indexByDBID(node);
                } else {
                    log.info("Couldn't get db from " + name);
                }
                node.setSeqId(seq_source[0], seq_source[1]);
                IDmap.inst().indexBySeqID(node);
            }
        }
    }

    static List<String> tokenize(String input, String delim) {
        List<String> v = new ArrayList<String>();
        StringTokenizer tk = new StringTokenizer(input, delim);
        while (tk.hasMoreTokens()) {
            v.add(tk.nextToken());
        }
        return v;
    }

    private static String parseANid(String name) {
        String [] parts = getParts(name);
        String paint_id = null;
        if (parts.length < 1) {
            paint_id = name;
        } else if (parts.length < 2) {
            paint_id = parts[0];
        }
        return paint_id;
    }

    private static String [] getSeqParts(String row) {
        String [] parts = getParts(row);
        String [] seq_source;
        if (parts.length >= 3) {
            if (parts[2].contains("=ENSTRUG") || parts[1].contains("=ENSTRUP")) {
                seq_source = parts[1].split(Constant.EQUAL);
            } else {
                seq_source = parts[2].split(Constant.EQUAL);
            }
            return seq_source;
        }
        else {
            return null;
        }
    }

    private static String [] getParts(String row) {
        if (row.charAt(row.length() - 1) == ';') {
            row = row.substring(0, row.length() - 1);
        }
        return row.split(Constant.PIPE);
    }

    private static String [] getDBparts(String row) {
        String [] parts = getParts(row);
        String [] db_source;
		/*
		 * Standard order from PANTHER Database is
		 * SPECI|DbGene=gene_id|ProteinSeq=protein_id
		 * There may be multiple genes all with the same Protein ID
		 */
        if (parts.length >= 3) {
            if (parts[2].contains("=ENSTRUG") || parts[1].contains("=ENSTRUP")) {
                db_source = parts[2].split(Constant.EQUAL);
                String [] seq_source = parts[1].split(Constant.EQUAL);
                log.info("Gene " + db_source[1] + " and protein " + seq_source[1] + " appear reversed in " + row);
            } else {
                db_source = parts[1].split(Constant.EQUAL);
                if (db_source.length == 3) {
                    if (db_source[0].equals("MGI") || db_source[0].equals("TAIR") || db_source[0].equals("ECOLI")) {
						/*
						 * MOUSE|MGI=MGI=97788|UniProtKB=Q99LS3
						 * ARATH|TAIR=locus=2015008|NCBI=NP_176687
						 */
                        db_source[1] = db_source[1] + ':' + db_source[2];
                    }
                    else {
                        log.info("Too many parts in " + parts[1]);
                    }
                }
            }
            return db_source;
        } else {
            return null;
        }
    }

    private static Vector<String> removeQuotes(List<String> columns) {
        Vector<String> updated = new Vector<String>();
        for (int i = 0; i < columns.size(); i++) {
            String contents = columns.get(i);
            if (contents.startsWith(DELIM_QUOTE)) {
                contents = contents.substring(1);
                if (contents.endsWith(DELIM_QUOTE)) {
                    contents = contents.substring(0, contents.length() - 1);
                }
            }
            updated.addElement(contents);
        }
        return updated;
    }


}
