package org.bbop.paint.touchup;

import org.apache.log4j.Logger;
import org.bbop.paint.LogAction;
import org.bbop.paint.LogAlert;
import org.bbop.paint.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class History {

	private static final Logger logger = Logger.getLogger(History.class);

	public static void write(String family_name) {
		String family_dir = Preferences.inst().getTreedir() + family_name + '/';

		if (FileUtil.validPath(family_dir)) {
			String logFileName = family_dir + File.separator + family_name + ".log";
			List<String> contents = new ArrayList<String>();
			LogAction.report(contents);
			LogAlert.report(contents);
			try {
				FileUtil.writeFile(logFileName, contents);
			} catch (IOException e) {
				logger.error("Unable to log updates for " + family_name);
				logger.error(e.getMessage());
			}
		}
	}
}
