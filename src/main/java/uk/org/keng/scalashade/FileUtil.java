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
import java.nio.channels.FileChannel;
import java.util.jar.JarFile;

/**
 * Utility functions for file handling
 */
class FileUtil {

    /**
     * Test if a file looks like it contains byte code
     * @param file the file to test
     * @return true if file starts with Java byte code signature
     */
    public static boolean isClass(File file) {
        try {
            DataInputStream in = new DataInputStream(new FileInputStream(file));
            return in.readInt() == 0xcafebabe;
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    /**
     * Test if a file looks like it contains a Jar
     * @param file the file to test
     * @return true if can be opened as a Jar
     */
    public static boolean isJar(File file) {
        try {
            new JarFile(file);
            return true;
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    /**
     * Copy a file to a new location, will overwrite existing file if can
     * @param sourceFile the file to copy
     * @param destFile the file to create/replace
     * @throws CtxException
     */
    public static void copyFile(File sourceFile, File destFile) {
        try {
            if (!destFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                destFile.createNewFile();
            }
        } catch (IOException e) {
            throw new CtxException("Failed to create file: " + destFile.getAbsolutePath(), e);
        }

        FileChannel source;
        long sourceSize;
        FileChannel destination;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            sourceSize = source.size();
        } catch (IOException e) {
            throw new CtxException("Failed to open file for reading: " + sourceFile.getAbsolutePath(), e);
        }

        try {
            destination = new FileOutputStream(destFile).getChannel();
        } catch (IOException e) {
            try {
                source.close();
            } catch (IOException i) { /* Ignore */ }
            throw new CtxException("Failed to open file for writing: " + destFile.getAbsolutePath(), e);
        }

        try {
            destination.transferFrom(source, 0, sourceSize);
        } catch (IOException e) {
            throw new CtxException("Failed to copy data between " + sourceFile.getAbsolutePath()
                    + " and " + destFile.getAbsolutePath(), e);
        } finally {
            try {
                source.close();
                destination.close();
            } catch (IOException i) {
                // Ignore
            }
        }
    }
}
