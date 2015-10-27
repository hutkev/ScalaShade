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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Helper for classes that may contain a @ScalaSignature annotation
 */
class ScalaSigClass {

    private final ClassNode _clazz = new ClassNode();
    private int sigAnnotation = -1;
    private ScalaSig sig = null;

    /**
     * Create from path to class file
     *
     * @param path the class file
     */
    public ScalaSigClass(String path) {
        FileInputStream in;
        try {
            in = new FileInputStream(path);
        } catch (IOException e) {
            throw new CtxException("Could not open/read file: " + path);
        }
        load(path, in);
    }

    /**
     * Create from path and input stream
     *
     * @param path path of class, just for error reporting
     * @param in   stream of class byte code
     */
    public ScalaSigClass(String path, InputStream in) {
        load(path, in);
    }

    /**
     * Private constructor, loads the class & parses @ScalaSignature if present
     *
     * @param path path of class, just for error reporting
     * @param in   stream of class byte code
     */
    private void load(String path, InputStream in) {

        // Load class into ASM
        try {
            ClassReader cr = new ClassReader(in);
            cr.accept(_clazz, 0);
        } catch (IOException e) {
            throw new CtxException("Could not read file: " + path);
        }

        // Extract ScalaSignature annotation bytes & check all looks OK
        int at = 0;
        if (_clazz.visibleAnnotations != null) {
            //noinspection unchecked
            for (AnnotationNode an : visibleAnnotations(_clazz)) {
                if (an.desc.equals("Lscala/reflect/ScalaSignature;")) {
                    if (sigAnnotation != -1)
                        throw new CtxException("Multiple ScalaSignature annotations found in: " + path);
                    if (an.values.size() != 2)
                        throw new CtxException("ScalaSignature has wrong number of values in: " + path);
                    if (!(an.values.get(0) instanceof String))
                        throw new CtxException("ScalaSignature has wrong type for value 0 in: " + path);
                    if (!(an.values.get(1) instanceof String))
                        throw new CtxException("ScalaSignature has wrong type for value 1 in: " + path);
                    if (!an.values.get(0).equals("bytes"))
                        throw new CtxException("ScalaSignature has wrong first value in" + path);
                    String sigString = (String) an.values.get(1);

                    byte[] sigBytes = Encoding.decode(sigString);
                    if (sigBytes == null)
                        throw new CtxException("ScalaSignature could not be decoded in" + path);
                    sig = ScalaSig.parse(sigBytes);
                    sigAnnotation = at;
                }
                at++;
            }
        }
    }

    /**
     * Get access to the @ScalaSignature
     *
     * @return ScalaSig or null if no @ScalaSignature present
     */
    public ScalaSig getSig() {
        return sig;
    }

    /**
     * Write the class byte to a file, will include any modification to @ScalaSignature
     *
     * @param path where to write
     * @throws CtxException
     */
    public void writeTo(String path) throws CtxException {
        try {
            FileOutputStream os = new FileOutputStream(path);
            os.write(getBytes());
            os.close();
        } catch (IOException ex) {
            throw new CtxException("Could not open/read file: " + path);
        }
    }

    /**
     * Get class bytes, will include any modification to @ScalaSignature
     *
     * @return the (possibly updated) class byte code
     */
    public byte[] getBytes() {
        // Update annotation
        if (sigAnnotation != -1) {
            ( visibleAnnotations(_clazz).get(sigAnnotation)).values.set(1, Encoding.encode(sig.asBytes()));
        }

        // Convert to byte code
        ClassWriter cw = new ClassWriter(0);
        _clazz.accept(cw);
        return cw.toByteArray();
    }

    @SuppressWarnings("Unchecked")
    List<AnnotationNode> visibleAnnotations(ClassNode clazz) {
        return (List<AnnotationNode>)clazz.visibleAnnotations;
    }
}
