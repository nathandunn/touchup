package org.bbop.paint.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class FileUtil {
	protected static Logger log = Logger.getLogger(FileUtil.class.getName());

	public static List<String> readFile(String fileName) {
		List<String> contents = new ArrayList<String>();
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
		if (true == error) {
			return null;
		}
		return contents;
	}

	/**
	 * **************** METHOD HAS NOT BEEN TESTED
	 * @throws IOException 
	 */
	public static void writeFile(String fileName, List<String> contents) throws IOException {
		BufferedWriter bufWriter = null;

		bufWriter = new BufferedWriter(new FileWriter(fileName));
		for (String line : contents) {
			if (line.charAt(line.length() - 1) == '\n')
				bufWriter.write(line);
			else
				 bufWriter.write(line + '\n');
		}

		if (null != bufWriter) {
			bufWriter.close();
		}

	}

	public static boolean validPath(String path) {
		if (null == path) {
			return false;
		}
		File f = new File(path);
		return f.canRead();
	}
}