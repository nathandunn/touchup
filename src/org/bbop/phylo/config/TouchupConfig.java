package org.bbop.phylo.config;

import java.io.File;
import java.io.StringWriter;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.bbop.golr.java.RetrieveGolrAnnotations;
import org.yaml.snakeyaml.Yaml;

public class TouchupConfig {

	public String gafdir;
	public String treedir;

//	RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("http://golr.geneontology.org/solr", 3, true) {
	public String GOlrURL;

	private static Logger LOG = Logger.getLogger(TouchupYaml.class);

	private static TouchupConfig preferences;

	/**
	 * Constructor declaration
	 * @throws Exception
	 *
	 *
	 * @see
	 */
	public static TouchupConfig inst() {
		if (preferences == null) {
			preferences = new TouchupConfig();
		}
		return preferences;
	}

	// Define the defaults for optional fields.
	public TouchupConfig() {
		gafdir = "";
		treedir = "";
		GOlrURL = "";
		preferences = this;
	}

	public void save(String config_file) {
		try {
			String yamlString = save();
			FileUtils.write(new File(config_file), yamlString);
		} catch (Exception e) {
			LOG.info(e.getMessage());
		}
	}
	
	public String save() {
		TouchupYaml configManager = new TouchupYaml();
		Yaml yaml = configManager.getYaml();
		StringWriter writer = new StringWriter();
		yaml.dump(this, writer);
		return writer.toString();
	}

}

