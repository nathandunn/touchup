package org.bbop.phylo.util;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
	private static final Logger log = Logger.getLogger(FileUtil.class.getName());

	public static List<String> readFile(String fileName) {
		List<String> contents = new ArrayList<>();
		BufferedReader bufReader = null;
		String line;
		boolean error = false;

		try {
			bufReader = new BufferedReader(new FileReader(fileName));
			line = bufReader.readLine();
			while (line != null) {
				contents.add(line);
				line = bufReader.readLine();
			}
		}
		catch (IOException ioex) {
			error = true;

			log.error("Exception " + ioex.getMessage() + " returned while attempting to read file " + fileName);

		}
		finally {
			try {
				if (null != bufReader) {
					bufReader.close();
				}
			}
			catch (IOException ioex2) {
				error = true;

				log.error("Exception " + ioex2.getMessage() + " returned while attempting to close file " + fileName);

			}
		}
		if (error) {
			return null;
		}
		return contents;
	}

	/**
	 * **************** METHOD HAS NOT BEEN TESTED
	 * @throws IOException 
	 */
	public static void writeFile(String fileName, List<String> contents) throws IOException {
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

	public static boolean validPath(String path) {
		if (null == path) {
			return false;
		}
		File f = new File(path);
		return f.isDirectory() && f.canRead();
	}

	public static boolean validFile(String filename) {
		boolean ok = false;
		if (filename != null) {
			File f = new File(filename);
			ok = f.canRead();
		}
		return ok;
	}
}