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

package org.bbop.phylo.touchup;

import org.bbop.phylo.annotate.AnnotationUtil;
import org.bbop.phylo.gaf.GafPropagator;
import org.bbop.phylo.model.Family;
import org.bbop.phylo.panther.IDmap;
import org.bbop.phylo.tracking.LogAction;
import org.bbop.phylo.tracking.LogAlert;
import org.bbop.phylo.tracking.LogUtil;
import org.bbop.phylo.tracking.Logger;
import org.bbop.phylo.util.FileUtil;
import org.bbop.phylo.util.OWLutil;
import org.bbop.phylo.util.ResourceLoader;
import org.bbop.phylo.util.TaxonChecker;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main {
    /**
     *
     */
    protected Thread runner;

    private static String[] args;

    private Family family;

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Main.class);

    /**
     * Method declaration
     *
     *
     * @param args either a family name, a file containing a list of family names, or a directory where the families are
     *
     * @see
     */
    public static void main(final String[] args) {
        Main.args = args;
        Main theRunner = new Main();

        SwingUtilities.invokeLater(theRunner.mainRun);

        Runtime.getRuntime().addShutdownHook(new Thread(theRunner.mainRun) {
            public void run() {
                Preferences.inst().writePreferences(Preferences.inst());
            }
        });
    }

    private final Runnable mainRun =
            new Runnable() {
                // this thread runs in the AWT event queue
                public void run() {
                    int family_count = 0;
                    if (args.length == 0 || (args.length % 2) == 1) {
                        provideHelp();
                        System.exit(0);
                    }
                    List<String> families;
                    String start_with_family = null;
                    String family_file = null;
                    for (int i = 0; i < args.length; i += 2) {
                        if (args[i].contains("t")) {
                            Preferences.inst().setTreeDir(args[i + 1]);
                        } else if (args[i].contains("s")) {
                            start_with_family = args[i + 1];
                        } else if (args[i].contains("f") || args[i].contains("d") || args[i].contains("l")) {
                            family_file = args[i + 1];
                        } else {
                            provideHelp();
                            System.exit(0);
                        }
                    }

                    File f = new File(family_file);

                    if (f.isDirectory()) {
                        Preferences.inst().setGafDir(family_file);
                        String [] files = f.list();
                        Arrays.sort(files);
                        families = new ArrayList<>();
                        boolean start = false;
                        for (String file : files) {
                            if (file.startsWith("PTHR")) {
                                start |= start_with_family == null || file.startsWith(start_with_family);
                                if (start) {
                                    families.add(file);
                                }
                            }
                        }
                    } else if (f.canRead()) {
                        families = FileUtil.readFile(new File(family_file));
                        for (int i = families.size() - 1; i >= 0; i--) {
                            // allow for commenting out lines in the input file
                            if (families.get(i).length() == 0 || families.get(i).startsWith("//")) {
                                families.remove(i);
                            }
                        }
                    } else {
                        families = new ArrayList<>();
                        if (family_file.startsWith("PTHR")) {
                            families.add(family_file);
                        }
                    }
                    try {
                        family_count = touchup(families);
                        log.info("Touched up " + family_count + " PAINT families");
                        System.exit(0);
                    }
                    catch (Exception e) { // should catch RuntimeException
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            };

    private int touchup(List<String> families) {
        log.info(families.size() + " families to touch up");
        Map<String, FamilySummary> run_summary = new HashMap<>();
        int count = 0;
        for (String family_name : families) {
            log.info("Touching up " + family_name + " (" + (families.indexOf(family_name) + 1) + " of " + families.size() + ")");
            clear();
            Family family = new Family();
            boolean available = gafFileExists(family_name);
            if (!available) {
                log.info("Missing GAF file for " + family_name);
            } else {
                available &= family.fetch(family_name);
                if (available) {
                    boolean proceed = TaxonChecker.isLive();
                    if (proceed) {
                        proceed &= resetAnnotations(family);
                        if (proceed) {
                            savePaint();
                            Logger.write(family_name);
                            FamilySummary family_summary = new FamilySummary();
                            run_summary.put(family_name, family_summary);
                        }
                    }
                    if (!proceed) {
                        logSummary(run_summary, count);
                        return run_summary.size();
                    }
                } else {
                    log.error("Unable to load tree for " + family_name);
                }
            }
            count++;
        }
        logSummary(run_summary, count);
        return run_summary.size();
    }

    private void clear() {
        IDmap.inst().clearGeneIDs();
        LogAction.clearLog();
        LogAlert.clearLog();
        OWLutil.clearTerms();
        System.gc();
    }

    private boolean resetAnnotations(Family family) {
        boolean reset = true;
        try {
            AnnotationUtil.collectExpAnnotationsBatched(family);
			/*
			 * The file may be null, in which case the following two methods
			 * simply return
			 */
            Logger.importPrior(family.getFamily_name());

            GafPropagator.importAnnotations(family);

        } catch (Exception e) {
            reset = false;
        }
        return reset;
    }

    private void savePaint() {
        if (family != null) {
            // Need to add the supporting experimental annotations to the withs
            family.save();
        }
    }

    private void logSummary(Map<String, FamilySummary> summaries, int count) {
        String program_name = ResourceLoader.inst().loadVersion();
        File log_dir = new File(Preferences.inst().getGafDir());
        if (FileUtil.validPath(log_dir)) {
            File logFileName = new File(log_dir, program_name + Constant.LOG_SUFFIX);
            List<String> contents = new ArrayList<>();
            contents.add("# " + program_name + " Log Report for " + LogUtil.dateNow());
            contents.add("");
            contents.add("\n");
            Set<String> families = summaries.keySet();
            int review_cnt = 0;
            for (String family_name : families) {
                FamilySummary report = summaries.get(family_name);
                review_cnt += report.summarize(family_name, contents);
            }
            try {
                contents.set(1, "Touched up " + summaries.size() + " PAINT families, " + (
                        summaries.size() - count) + " are missing GAF files and " + review_cnt + " need reviewing.");
                FileUtil.writeFile(logFileName, contents);
            } catch (IOException e) {
                log.error("Unable to log touchup summary: " + e.getMessage());
            }
        }
    }

    private boolean gafFileExists(String family_name) {
        File family_dir = new File(Preferences.inst().getGafDir(), family_name);
        boolean ok = FileUtil.validPath(family_dir);
        if (ok) {
            File gaf_file = new File(family_dir, family_name + Constant.GAF_SUFFIX);
            ok = FileUtil.validFile(gaf_file);
            if (!ok) {
                log.info("Can't read: " + gaf_file);
            }

        } else {
            log.info("Invalid path: " + family_dir);
        }
        return ok;
    }

    private void provideHelp() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("USAGE:\n");
        buffer.append("\t -f <familyname> // touches up a single PAINT family\n");
        buffer.append("\t -l <filename> // obtains family names from contents of the file\n");
        buffer.append("\t -d <directoryname> // touches up families listed in this directory\n");
        buffer.append("\t -d <directoryname> -s <starting family name> // as above, but solely the alphabetically latter part of directory\n");
        buffer.append("\n\t // optionally a -t argument may be added to any of the above to indicate the correct PANTHER tree directory\n");
        log.error(buffer);
    }
}
