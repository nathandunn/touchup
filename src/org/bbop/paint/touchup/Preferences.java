/* 
 * 
 * Copyright (c) 2010, Regents of the University of California 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Neither the name of the Lawrence Berkeley National Lab nor the names of its contributors may be used to endorse 
 * or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, 
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */

package org.bbop.paint.touchup;

import org.apache.log4j.Logger;

import java.awt.*;
import java.beans.DefaultPersistenceDelegate;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Used for reading previous or default user settings from property file and storing current user settings
 */

public class Preferences {
	/**
	 *
	 */
	private static final long serialVersionUID = -5472475387423113108L;

	private String basedir = "/Users/suzi/projects/go/";
	private String gafdir = "gene-associations/submission/paint/";
	private String treedir = "data/trees/panther/";
/*
private String treedir = "gene-associations/submission/paint/";
 */

	public static final String TREE_SUFFIX = ".tree";
	public static final String ATTR_SUFFIX = ".tab";
	public static final String MSA_SUFFIX = ".mia";
	public static final String WTS_SUFFIX = ".wts";
	public static final String GAF_SUFFIX = ".gaf";
	public static final String LOG_SUFFIX = ".log";
	public static final String OLDLOG_SUFFIX = ".txt";

	/*
	 * Get the NCBI taxon ID from their FTP-ed file dump
	 */
	private Map<String, String> taxa2IDs;
	private Map<String, String> IDs2taxa;

	private static final Logger log = Logger.getLogger(Preferences.class);

	private static Preferences preferences;

	/**
	 * Constructor declaration
	 * @throws Exception
	 *
	 *
	 * @see
	 */
	public Preferences() { //throws Exception {
	}

	public static Preferences inst() {
		if (preferences == null) {
			XMLDecoder d;
			try {
				d = new XMLDecoder(new BufferedInputStream(new FileInputStream(
						Preferences.getPrefsXMLFile())));
				preferences = (Preferences) d.readObject();
				d.close();
			} catch (Exception e) {
				log.info("Could not read preferences file from "
						+ Preferences.getPrefsXMLFile());
			}
			if (preferences == null)
				preferences = new Preferences();
		}
		return preferences;
	}

	void writePreferences(Preferences preferences){
		try {
			XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
					new FileOutputStream(getPrefsXMLFile())));
			log.info("Writing preferences to " + getPrefsXMLFile());
			encoder.setPersistenceDelegate(Font.class,
					new DefaultPersistenceDelegate(
							new String[]{ "name",
									"style",
									"size" }) );
			encoder.setPersistenceDelegate(Color.class,
					new DefaultPersistenceDelegate(
							new String[]{ "red",
									"green",
									"blue" }) );
			encoder.writeObject(preferences);
			encoder.close();
		} catch (IOException ex) {
			log.info("Could not write verification settings!");
			ex.printStackTrace();
		}
	}

	private static File getPrefsXMLFile() {
		return new File(getPaintPrefsDir(), "preferences.xml");
	}

	private static File getPaintPrefsDir() {
		return new File("config");
	}

	private static ClassLoader getExtensionLoader() {
		return Preferences.class.getClassLoader();
	}

	public Object clone() throws CloneNotSupportedException {

		throw new CloneNotSupportedException();

	}

	private String getBasedir() {
		return basedir;
	}

	public void setBasedir(String basedir) {
		this.basedir = basedir;
	}

	public String getGafdir() {
		return getBasedir() + gafdir;
	}

	public String getTreedir() {
		return getBasedir() + treedir;
	}

	public String getTaxonID(String species_name) {
		if (taxa2IDs == null) {
			loadTaxaMapping();
		}
		String taxon_id = null;
		if (species_name != null && species_name.length() > 0) {
			if (!species_name.equals("root"))
				species_name = species_name.substring(0, 1).toUpperCase() + species_name.substring(1);
			taxon_id = taxa2IDs.get(species_name);
			if (taxon_id == null) {
				taxon_id = taxa2IDs.get(species_name.toLowerCase());
			}
			if (taxon_id == null) {
				taxon_id = taxa2IDs.get(speciesNameHack(species_name));
			}
		}
		return taxon_id;
	}

	public String getSpecies(String taxon_id) {
		return IDs2taxa.get(taxon_id);
	}

	private void loadTaxaMapping() {
		taxa2IDs = new HashMap<String, String>();
		IDs2taxa = new HashMap<String, String>();
		taxa2IDs.put("LUCA", Constant.TAXON_PREFIX+"1");
		IDs2taxa.put(Constant.TAXON_PREFIX+"1", "LUCA");
		loadUniProtTaxa();
		loadNCBITaxa();
	}

	private void loadNCBITaxa() {
		try {
			URL url = getExtensionLoader().getResource(
					"ncbi_taxa_ids.txt");
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(url.openStream()));
			String id_pair = reader.readLine();
			while (id_pair != null) {
				if (!id_pair.contains("authority")) {
					id_pair = id_pair.replace('\t', ' ');
					String ids []= id_pair.split("\\|");
					String taxon_id = Constant.TAXON_PREFIX+(ids[0].trim());
					String name = ids[1].trim();
					if (!ids[2].contains(name))
						name = (name + " " +  ids[2].trim()).trim();
					else if (ids[2].trim().length() > name.length())
						name = ids[2].trim();
					if (id_pair.contains("scientific name")) {
						IDs2taxa.put(taxon_id, name);
					}
					taxa2IDs.put(name, taxon_id);
				}
				id_pair = reader.readLine();
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadUniProtTaxa() {
		try {
			URL url = getExtensionLoader().getResource(
					"speclist.txt");
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(url.openStream()));
			String line = reader.readLine();
			while (line != null) {
				if (line.contains("N=") && !line.contains("Official")) {
					int index = line.indexOf(' ');
					String code = line.substring(0, index);
					index += 3;
					String taxon_id = Constant.TAXON_PREFIX+line.substring(index, line.indexOf(':')).trim();
					index = line.indexOf("N=") + 2;
					String name = line.substring(index).trim();
					if (!IDs2taxa.containsKey(taxon_id))
						IDs2taxa.put(taxon_id, name);
					if (!taxa2IDs.containsKey(name))
						taxa2IDs.put(name, taxon_id);
					if (!taxa2IDs.containsKey(code))
						taxa2IDs.put(code, taxon_id);
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String speciesNameHack(String name) {
		String lcName = name.toLowerCase();
		/* The GO database is not using the suffix */
		if (lcName.equals("human")) {
			name = "Homo sapiens";
		} else if (lcName.equals("pantr")) {
			name = "Pan troglodytes";
		} else if (lcName.equals("homo-pan")) {
			name = "Homininae";
		} else if (lcName.equals("mouse")) {
			name = "Mus musculus";
		} else if (lcName.equals("rat")) {
			name = "Rattus norvegicus";
		} else if (lcName.equals("bovin")) {
			name = "Bos taurus";
		} else if (lcName.equals("canis familiaris") || lcName.equals("canfa")) {
			name = "Canis lupus familiaris";
		} else if (lcName.equals("mondo")) {
			name = "Monodelphis domestica";
		} else if (lcName.equals("ornan")) {
			name = "Ornithorhynchus anatinus";
		} else if (lcName.equals("chick")) {
			name = "Gallus gallus";
		} else if (lcName.equals("xentr")) {
			name = "Xenopus (Silurana) tropicalis";
		} else if (lcName.equals("fugu rubripes") || lcName.equals("fugru")) {
			name = "Takifugu rubripes";
		} else if (lcName.equals("brachydanio rerio") || lcName.equals("danre")) {
			name = "Danio rerio";
		} else if (lcName.equals("cioin")) {
			name = "Ciona intestinalis";
		} else if (lcName.equals("strpu")) {
			name = "Strongylocentrotus purpuratus";
		} else if (lcName.equals("caenorhabditis")) {
			name = "Caenorhabditis elegans";
		} else if (lcName.equals("briggsae") || lcName.equals("caebr")) {
			name = "Caenorhabditis briggsae";
		} else if (lcName.equals("drome")) {
			name = "Drosophila melanogaster";
		} else if (lcName.equals("anopheles gambiae str. pest") || lcName.equals("anoga")) {
			name = "Anopheles gambiae";
		} else if (lcName.equals("yeast")) {
			name = "Saccharomyces cerevisiae";
		} else if (lcName.equals("ashbya gossypii") || lcName.equals("ashgo")) {
			name = "Eremothecium gossypii";
		} else if (lcName.equals("neucr")) {
			name = "Neurospora crassa";
		} else if (lcName.equals("schpo")) {
			name = "Schizosaccharomyces pombe";
		} else if (lcName.equals("dicdi")) {
			name = "Dictyostelium discoideum";
		} else if (lcName.equals("aspergillus nidulans")) {
			name = "Emericella nidulans";
		} else if (lcName.equals("chlre")) {
			name = "Chlamydomonas reinhardtii";
		} else if (lcName.equals("orysj")) {
			name = "Oryza sativa";
		} else if (lcName.equals("arath")) {
			name = "Arabidopsis thaliana";
		} else if (lcName.equals("metac")) {
			name = "Methanosarcina acetivorans";
		} else if (lcName.equals("strco")) {
			name = "Streptomyces coelicolor";
		} else if (lcName.equals("glovi")) {
			name = "Gloeobacter violaceus";
		} else if (lcName.equals("lepin")) {
			name = "Leptospira interrogans";
		} else if (lcName.equals("braja")) {
			name = "Bradyrhizobium japonicum";
		} else if (lcName.equals("escherichia coli coli str. K-12 substr. MG1655") || lcName.equals("ecoli")) {
			name = "Escherichia coli";
		} else if (lcName.equals("enthi")) {
			name = "Entamoeba histolytica";
		} else if (lcName.equals("bacsu")) {
			name = "Bacillus subtilis";
		} else if (lcName.equals("deira")) {
			name = "Deinococcus radiodurans";
		} else if (lcName.equals("thema")) {
			name = "Thermotoga maritima";
		} else if (lcName.equals("opisthokonts")) {
			name = "Opisthokonta";
		} else if (lcName.equals("bactn")) {
			name = "Bacteroides thetaiotaomicron";
		} else if (lcName.equals("leima")) {
			name = "Leishmania major";
		} else if (lcName.equals("eubacteria")) {
			name = "Bacteria <prokaryote>";
		} else if (lcName.equals("theria")) {
			name = "Theria <Mammalia>";
		} else if (lcName.equals("geobacter sufurreducens") || lcName.equals("geosl")) {
			name = "Geobacter sulfurreducens";
		} else if (lcName.equals("psea7")) {
			name = "Pseudomonas aeruginosa";
		} else if (lcName.equals("aquae") || lcName.equals("aquifex aeolicus vf5")) {
			name = "Aquifex aeolicus";
		} else if (lcName.equals("metac") || lcName.equals("methanosarcina acetivorans c2a")) {
			name = "Methanosarcina acetivorans";
		} else if (lcName.equals("sulso") || lcName.equals("sulfolobus solfataricus p2")) {
			name = "Sulfolobus solfataricus";
		} else if (lcName.equals("saccharomycetaceae-candida")) {
			name = "mitosporic Nakaseomyces";
		} else if (lcName.equals("sordariomycetes-leotiomycetes")) {
			name = "Leotiomycetes";
		} else if (lcName.equals("excavates")) {
			name = "Excavarus";
		} else if (lcName.equals("metazoa-choanoflagellida")) {
			name = "Opisthokonta";
		} else if (lcName.equals("alveolata-stramenopiles")) {
			name = "Eukaryota";
		} else if (lcName.equals("pezizomycotina-saccharomycotina")) {
			name = "saccharomyceta";
		} else if (lcName.equals("unikonts")) {
			name = "Eukaryota";
		} else if (lcName.equals("archaea-eukaryota")) {
			name = "cellular organisms";
		} else if (lcName.equals("osteichthyes")) {
			name = "Euteleostomi";
		} else if (lcName.equals("luca")) { // last universal common ancestor
			name = "root";
		} else if (lcName.equals("craniata-cephalochordata")) {
			name = "Chordata";
		} else if (lcName.equals("hexapoda-crustacea")) {
			name = "Pancrustacea";
		} else if (lcName.equals("rhabditida-chromadorea")) {
			name = "Chromadorea";
		} else if (lcName.equals("artiodactyla")) {
			name = "unclassified Artiodactyla";
		}
		return name;
	}

}
