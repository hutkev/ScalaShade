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

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Utility for shading all classes in a JAR file. Classes are identified as entries with a
 * name ending in ".class". During saving classes containing a @SparkSignature are updated
 * if required, all other classes and JAR contents are unchanged.
 */
class JarShade {
    private final String inputJarPath;
    private final JarFile jarFile;

    /**
     * Construct passing existing jar
     *
     * @param jar the jar file
     * @throws CtxException
     */
    public JarShade(File jar) {
        try {
            inputJarPath = jar.getAbsolutePath();
            jarFile = new JarFile(jar);
        } catch (IOException e) {
            throw new CtxException("Could not open jar for reading: " + jar, e);
        }
    }

    /**
     * Copy contents of jar to new location with updates classes as needed.
     *
     * @param jar     location of new jar, will be created/overwritten as needed
     * @param from    Absolute namespace to change
     * @param to      Absolute namespace to use instead
     * @param verbose If true, extra debug is printed
     */
    public void writeTo(File jar, String from, String to, boolean verbose) {

        // Open new JAR
        JarOutputStream jos;
        try {
            jos = new JarOutputStream(new FileOutputStream(jar));
        } catch (IOException e) {
            throw new CtxException("Could not open jar for writing: " + jar, e);
        }

        // Iterate over existing jar
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            // Directories are create the same
            if (entry.isDirectory()) {
                try {
                    jos.putNextEntry(new JarEntry(entry.getName()));
                    jos.closeEntry();
                } catch (IOException e) {
                    throw new CtxException("Could not write jar directory entry for " + entry.getName() + " in: " + jar.getAbsolutePath());
                }
            } else {

                // Open other entries for reading
                InputStream in;
                try {
                    in = jarFile.getInputStream(entry);
                } catch (IOException e) {
                    throw new CtxException("Could not read entry for " + entry.getName() + " in: " + inputJarPath, e);
                }

                if (entry.getName().endsWith(".class")) {
                    // If we have a class try process @ScalaSignature
                    try {
                        ScalaSigClass sigClass = new ScalaSigClass(entry.getName(), in);
                        ScalaSig sig = sigClass.getSig();
                        if (sig != null && sig.replace(from, to) > 0) {
                            // This one need re-writing, swap input stream to updated version
                            in.close();
                            in = new ByteArrayInputStream(sigClass.getBytes());
                            if (verbose)
                                System.out.println("Modified:  " + entry.getName());
                        } else {
                            // Nothing to change here, re-open input stream as signature processing
                            // may have consumed some of the stream
                            try {
                                in.close();
                                in = jarFile.getInputStream(entry);
                            } catch (IOException e) {
                                throw new CtxException("Could not read entry for " + entry.getName() + " in: " + inputJarPath, e);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to shade " + entry.getName() + " in " + inputJarPath);
                        e.printStackTrace();
                    }
                }

                // Write the new entry, 'in' could be original or an updated version
                try {
                    jos.putNextEntry(new JarEntry(entry.getName()));
                    byte[] byteBuffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(byteBuffer)) != -1) {
                        jos.write(byteBuffer, 0, bytesRead);
                    }
                    jos.flush();
                    jos.closeEntry();
                } catch (IOException e) {
                    throw new CtxException("Could not write entry for " + entry.getName() + " in: " + jar.getAbsolutePath(), e);
                }
            }
        }

        // All done
        try {
            jos.close();
        } catch (IOException e) {
            throw new CtxException("Error closing jar : " + jar.getAbsolutePath(), e);
        }
    }
}
