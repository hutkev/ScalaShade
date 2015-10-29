/*
 * Copyright 2015 Kevin Jones
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.org.keng.scalashade;

import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;

/**
 * Main entry, parse args and execute
 */
public class Main {

    private final static String header = "Correct Scala runtime type information in classes or jars after shading";
    private final static String footer = "v0.1 https://github.com/hutkev/ScalaShade";

    public static void main(String[] args) throws IOException {

        // Pull args apart
        Options options = new Options();
        options.addOption("h", "help", false, "help");
        options.addOption("v", "verbose", false, "logs classes being modified");
        options.addOption("d", "debug", false, "dump entry table when handling a class");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Argument Parsing failed.  Reason: " + e.getMessage());
            return;
        }

        if (cmd.getArgList().size() != 4 || cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("scala-shade [opts] <input jar/class> <output jar/class> <from-namespace> <to-namespace>", header, options, footer);
            return;
        }

        // Set parameters/flags
        boolean verbose = cmd.hasOption("v");
        boolean debug = cmd.hasOption("d");
        String in = cmd.getArgList().get(0);
        String out = cmd.getArgList().get(1);
        String from = cmd.getArgList().get(2);
        String to = cmd.getArgList().get(3);

        File inFile = new File(in);
        if (FileUtil.isClass(inFile)) {
            // Looks like we got a class file, so deal with it directly
            try {
                ScalaSigClass sigClass = new ScalaSigClass(in);
                ScalaSig sig = sigClass.getSig();
                if (sig!=null && debug) {
                    System.err.println(sig.toString());
                }
                if (sig != null && sig.replace(from, to) > 0) {
                    sigClass.writeTo(out);
                    if (verbose)
                        System.out.println("Modified:  " + in);
                    if (debug)
                        System.err.println(sig.toString());
                } else {
                    FileUtil.copyFile(inFile, new File(out));
                }
            } catch (CtxException e) {
                e.printStackTrace();
            }
        } else if (FileUtil.isJar(inFile)) {
            // Looks like we got a jar, use helper to handle
            try {
                JarShade jarShade = new JarShade(new File(in));
                jarShade.writeTo(new File(out), from, to, verbose);
            } catch (CtxException ex) {
                ex.printStackTrace();
            }
        } else {
            System.err.println("Input file " + in + " does not appear to be either a class file or jar.");
        }
    }
}
