package org.bbop.phylo.util;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.List;

public class FileUtil {
	private static final Logger log = Logger.getLogger(FileUtil.class.getName());

	public static List<String> readFile(File file) {
		try {
			List<String> lines = FileUtils.readLines(file);
			return lines;
		}
		catch (IOException ioex) {
			log.error("Exception " + ioex.getMessage() + " returned while attempting to read file " + file);
			return null;
		}
	}

	/**
	 * **************** METHOD HAS NOT BEEN TESTED
	 * @throws IOException 
	 */
	public static void writeFile(File fileName, List<String> contents) throws IOException {
		BufferedWriter bufWriter;

		bufWriter = new BufferedWriter(new FileWriter(fileName));
		for (String line : contents) {
			if (line.length() > 0 && line.charAt(line.length() - 1) == '\n')
				bufWriter.write(line);
			else
				 bufWriter.write(line + '\n');
		}

		bufWriter.close();

	}

	public static boolean validPath(File path) {
		if (null == path) {
			return false;
		}
		return path.isDirectory() && path.canRead();
	}

	public static boolean validFile(File f) {
		boolean ok = false;
		if (f != null) {
			ok = f.canRead();
		}
		return ok;
	}
}