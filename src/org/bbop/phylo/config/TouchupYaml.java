package org.bbop.phylo.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class TouchupYaml {

	private static Logger LOG = Logger.getLogger(TouchupYaml.class);

	/**
	 * Constructor.
	 */
	public TouchupYaml() {
		// Nobody here.
	}
	/**
	 * Work with a flexible document definition from a configuration file.
	 *
	 * @param location
	 * @throws FileNotFoundException 
	 */
	public void loadConfig(String yaml_file) {

		LOG.info("Trying config found at: " + yaml_file);
		// Find the file in question on the filesystem.
		try {
			InputStream input = new FileInputStream(new File(yaml_file));

			LOG.info("Found config: " + yaml_file);
			
			Constructor constructor = new Constructor(TouchupConfig.class);
			Yaml yaml = new Yaml(constructor);
			
			TouchupConfig config = (TouchupConfig) yaml.load(input);
			
		} catch (FileNotFoundException e) {
			LOG.info("Failure with config file at: " + yaml_file);
		}
	}
	
	public Yaml getYaml() {
		DumperOptions options=new DumperOptions();
		options.setIndent(4);
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		Yaml yaml = new Yaml(options);
		return yaml;
	}
}
