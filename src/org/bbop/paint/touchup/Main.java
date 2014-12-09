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
import org.bbop.paint.LogAction;
import org.bbop.paint.LogAlert;

import javax.swing.*;

public class Main {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected Thread runner;

	private static String[] args;

	private static final Logger log = Logger.getLogger(Main.class);

	/**
	 * Method declaration
	 *
	 *
	 * @param args
	 *
	 * @see
	 */
	public static void main(String[] args) {
		Main.args = args;
		Main theRunner = new Main();

		SwingUtilities.invokeLater(theRunner.mainRun);

		Runtime.getRuntime().addShutdownHook(new Thread(theRunner.mainRun) {
			public void run() {
				Preferences.inst().writePreferences(Preferences.inst());
			}
		});
	}

	Runnable mainRun =
			new Runnable() {
		// this thread runs in the AWT event queue
		public void run() {
			Brush paintbrush = Brush.inst();
			if (args.length == 0) {
				System.err.println("Please provide a family ID.");
				System.exit(1);
			}
			String family_name = args[0];
			if (args.length > 1)
				Preferences.inst().setBasedir(args[1]);
			try {
				if (paintbrush.loadPaint(family_name)) {
					paintbrush.savePaint();
					LogAction.report(family_name);
					LogAlert.report(family_name);
				} 
				else {
					log.error("Unable to load " + family_name);
				}
				System.exit(0);
			}
			catch (Exception e) { // should catch RuntimeException
				e.printStackTrace();
				System.exit(2);
			}
		}
	};
}
