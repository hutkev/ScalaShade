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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Partial signature decoding, modification & encoding, reversed from
 * https://github.com/scala/scala/blob/2.11.x/src/scalap/scala/tools/scalap/scalax/rules/scalasig/ScalaSig.scala
 * <p/>
 * This deliberately decodes the minimum required to allow path replacement to avoid unwanted dependencies on the
 * format being used. Briefly the format is constructed as a table of entries. The key entry is an 'EXT_MOD_CLASS_REF_ID'
 * which joins together a term name (a string) to an optional parent 'EXT_MOD_CLASS_REF_ID'. Thus each 'EXT_MOD_CLASS_REF_ID'
 * defines and absolute namespace of 1..n components depending on how many parents exist.
 * <p/>
 * To change a namespace we locate the correct 'EXT_MOD_CLASS_REF_ID' and replace its term name and parent (recursively) by
 * creating new entries at the end of the list (to avoid disrupting any other dependencies that might exist) as needed.
 */

/**
 * Identifiers for tables entries that we are interested in
 */
class TblTypeID {
    final static int TERM_NAME_ID = 1;
    final static int EXT_MOD_CLASS_REF_ID = 10;
}

/**
 * Interface for all entry objects
 */
interface TableEntry {
    /**
     * Write byte representation of table entry into the stream
     *
     * @param out stream to write onto
     * @throws IOException
     */
    void write(ByteArrayOutputStream out) throws IOException;
}

/**
 * Collection of entries from the ScalaSignature. Most types of entry are simply stored as raw bytes. The two cases
 * we are interested in are decoded and indexed. Methods are provided for renaming a namespace.
 */
class Table {
    private final List<TableEntry> entries = new ArrayList<TableEntry>();
    private final HashMap<Integer, ExtModClassRefEntry> extModClassRefEntriesMap = new HashMap<Integer, ExtModClassRefEntry>();
    private final HashMap<Integer, TermNameEntry> termNameMap = new HashMap<Integer, TermNameEntry>();

    /**
     * Create a new table entry from its type & raw bytes.
     *
     * @param type the entry type
     * @param raw the raw bytes for the entry
     */
    void addEntry(int type, byte[] raw) {
        switch (type) {
            case TblTypeID.TERM_NAME_ID:
                TermNameEntry term = new TermNameEntry(raw);
                entries.add(term);
                termNameMap.put(entries.size() - 1, term);
                break;
            case TblTypeID.EXT_MOD_CLASS_REF_ID:
                ExtModClassRefEntry classRef = new ExtModClassRefEntry(raw);
                entries.add(classRef);
                extModClassRefEntriesMap.put(entries.size() - 1, classRef);
                break;
            default:
                entries.add(new RawEntry(type, raw));
                break;
        }
    }

    /**
     * Write the table as a byte stream
     *
     * @param out the stream to write to
     * @throws IOException
     */
    void write(ByteArrayOutputStream out) throws IOException {
        Nat.write(entries.size(), out);
        for (TableEntry e : entries) {
            e.write(out);
        }
    }

    /**
     * Replace a namespace in the table with another namespace
     *
     * @param replace the namespace to replace, use '.' separators as usual
     * @param with    the namespace to use instead, use '.' separators as usual
     * @return the number of namespaces that were updated
     */
    int replace(String replace, String with) {

        // Locate extModClassRef entries that are exact match for namespace
        ArrayList<Integer> matched = new ArrayList<Integer>();
        for (Map.Entry<Integer, ExtModClassRefEntry> entry : extModClassRefEntriesMap.entrySet()) {
            ExtModClassRefEntry e = entry.getValue();
            String ref = resolveClassRef(e);
            if (ref != null && ref.equals(replace)) {
                matched.add(entry.getKey());
            }
        }

        // Correct the entry
        for (int index : matched) {
            updateClassRef(extModClassRefEntriesMap.get(index), with.split("\\."));
        }
        return matched.size();
    }

    /**
     * Construct full namespace for an ExtModClassRef entry
     *
     * @param ref the entry
     * @return the namespace it encodes
     */
    private String resolveClassRef(ExtModClassRefEntry ref) {
        TermNameEntry e = termNameMap.get(ref.nameRef());
        if (e == null) return null;
        String base = "";
        if (ref.symbolRef() != -1) {
            ExtModClassRefEntry classRef = extModClassRefEntriesMap.get(ref.symbolRef());
            if (classRef == null) return null;
            base = resolveClassRef(classRef)+".";
        }
        return base + e.name();
    }

    /**
     * Update a ExtModClassRef to encode a different namespace, the existing entry is re-used but any parent
     * components of the namespace result in new ExtModClassRef being added to the table to avoid disrupting
     * other entries which may depend on the parent ExtModClassRef/TermName entries of the existing entry for
     * purposes we don't understand.
     *
     * @param ref  the entry to update
     * @param with array of namespace components to use instead
     */
    private void updateClassRef(ExtModClassRefEntry ref, String[] with) {
        entries.add(new TermNameEntry(with[with.length - 1]));
        int termRef = entries.size()-1;
        int symbolRef = -1;
        if (with.length>1)
            symbolRef = addClassRef(Arrays.copyOf(with, with.length - 1));
        ref.update(termRef, symbolRef);
    }

    /**
     * Create a new ExtModClassRef that encodes the passed namespace components
     *
     * @param with namespace components
     * @return index of new entry in table
     */
    private int addClassRef(String[] with) {
        // Recursively add parent class ref entries to get correct symbolRef
        int symbolRef = -1;
        if (with.length > 1) {
            symbolRef = addClassRef(Arrays.copyOf(with, with.length - 1));
        }

        // Add this one using last string as a term
        entries.add(new TermNameEntry(with[with.length - 1]));
        entries.add(new ExtModClassRefEntry(entries.size() - 1, symbolRef));
        return entries.size() - 1;
    }

    private static final String separator = System.getProperty("line.separator");

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (TableEntry entry : entries) {
            sb.append(entry.toString()).append(separator);
        }
        return sb.toString();
    }
}

/**
 * Generic container for a table entry, just holds on to raw bytes and does not attempt decoding.
 */
class RawEntry implements TableEntry {
    private final int type;
    private final byte[] raw;

    /**
     * Create from raw
     * @param type the entry type
     * @param raw the raw bytes
     */
    RawEntry(int type, byte[] raw) {
        this.type = type;
        this.raw = raw;
    }

    /**
     * Write entry back to a stream
     * @param bos stream to write to
     * @throws IOException
     */
    public void write(ByteArrayOutputStream bos) throws IOException {
        Nat.write(type, bos);
        Nat.write(raw.length, bos);
        bos.write(raw);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type=").append(type).append(" Raw=");
        for (int i = 0; i < Math.min(64, raw.length); i++) {
            if (raw[i] > 32 && raw[i] < 127)
                sb.append((char) raw[i]);
            else
                sb.append(".");
        }
        return sb.toString();
    }
}

/**
 * Container for table class ref entries. These have a nameRef which refers to a termName entry and a symbolRef which
 * refers to a parent ExtModClassRefEntry. To decode them you walk the hierarchy until you find one without a
 * symbolRef, its optional in the byte encoding. We represent that by setting the symbolRef to -1.
 */
class ExtModClassRefEntry implements TableEntry {
    private int _nameRef;
    private int _symbolRef;

    /**
     * Create from existing name & symbol entries
     * @param nameRef the index of a termName entry for the name
     * @param symbolRef the index of a "parent" ExtModClassRef
     */
    ExtModClassRefEntry(int nameRef, int symbolRef) {
        _nameRef = nameRef;
        _symbolRef = symbolRef;
    }

    /**
     * Decode from raw bytes
     * @param raw the bytes
     */
    ExtModClassRefEntry(byte[] raw) {
        ByteArrayInputStream in = new ByteArrayInputStream(raw);
        _nameRef = Nat.read(in);

        // Symbol is optional in the encoding
        _symbolRef = -1;
        if (in.available() > 0) {
            _symbolRef = Nat.read(in);
        }
    }

    int nameRef() {
        return _nameRef;
    }

    int symbolRef() {
        return _symbolRef;
    }

    /**
     * Update this entry with new name & symbol entries
     * @param nameRef the index of a termName entry for the name
     * @param symbolRef the index of a "parent" ExtModClassRef
     */
    void update(int nameRef, int symbolRef) {
        _nameRef = nameRef;
        _symbolRef = symbolRef;
    }

    /**
     * Write entry back to a stream
     * @param bos stream to write to
     * @throws IOException
     */
    public void write(ByteArrayOutputStream bos) throws IOException {
        Nat.write(TblTypeID.EXT_MOD_CLASS_REF_ID, bos);
        if (_symbolRef != -1) {
            Nat.write(Nat.size(_nameRef)+Nat.size(_symbolRef), bos);
            Nat.write(_nameRef, bos);
            Nat.write(_symbolRef, bos);
        } else {
            Nat.write(Nat.size(_nameRef), bos);
            Nat.write(_nameRef, bos);
        }
    }

    @Override
    public String toString() {
        return "Type=extModClassRef " + "nameRef=" + _nameRef + " _symbolRef=" + _symbolRef;
    }
}

/**
 * Container for TermName entries. This just contains a UTF-8 encoded string.
 */
class TermNameEntry implements TableEntry {
    private final String _name;

    /**
     * Construct from string
     * @param name the name
     */
    TermNameEntry(String name) {
        _name = name;
    }

    /**
     * Construct from raw bytes
     * @param raw raw bytes to construct from
     */
    TermNameEntry(byte[] raw) {
        _name = new String(raw, StandardCharsets.UTF_8);
    }

    String name() {
        return _name;
    }

    /**
     * Write entry back to a stream
     * @param bos stream to write to
     * @throws IOException
     */
    public void write(ByteArrayOutputStream bos) throws IOException {
        Nat.write(TblTypeID.TERM_NAME_ID, bos);
        byte[] bytes = _name.getBytes(StandardCharsets.UTF_8);
        Nat.write(bytes.length, bos);
        bos.write(bytes);
    }

    public String toString() {
        return "Type=termName name=" + _name;
    }
}

/**
 * Container for ScalaSignature data.
 */
class ScalaSig {

    private final int _majorVersion;
    private final int _minorVersion;
    private final Table _table;

    /**
     * Construct a new signature, use {@link #parse(byte[])} to create
     *
     * @param majorVersion major version
     * @param minorVersion minor version
     * @param table        table of entries in signature
     */
    private ScalaSig(int majorVersion, int minorVersion, Table table) {
        _majorVersion = majorVersion;
        _minorVersion = minorVersion;
        _table = table;
    }

    /**
     * Parse a signature from a byte stream
     *
     * @param in input bytes to parse
     * @return the decoded signature data
     * @throws CtxException
     */
    static ScalaSig parse(byte[] in) throws CtxException {
        ByteArrayInputStream bis = new ByteArrayInputStream(in);

        // Pull version info & check OK
        int major = Nat.read(bis);
        int minor = Nat.read(bis);
        if (major!=5 || minor!=0) {
            throw new CtxException("Unexpected signature version found: "+major+"."+minor);
        }

        // Pull table
        int tblEntries = Nat.read(bis);
        Table table = new Table();
        for (int e = 0; e < tblEntries; e++) {
            int type = Nat.read(bis);
            int size = Nat.read(bis);
            byte[] raw = new byte[size];
            if (bis.read(raw, 0, size) != size) {
                throw new CtxException("Unexpected EOF in signature data");
            }
            table.addEntry(type, raw);
        }

        // The input stream should be consumed at this point but a 'feature' of the encoding is
        // that there may be a trailer 0 byte, just check all look good
        int trail = bis.read();
        if (trail == 0)
            trail = bis.read();
        if (trail != -1)
            throw new CtxException("Unexpected additional byte found at end of signature");

        // All good so create signature
        return new ScalaSig(major, minor, table);
    }

    /**
     * Replace a namespace in the table with another namespace
     *
     * @param replace the namespace to replace, use '.' separators as usual
     * @param with    the namespace to use instead, use '.' separators as usual
     * @return the number of namespaces that were updated
     */
    int replace(String replace, String with) {
        return _table.replace(replace, with);
    }

    /**
     * Get a byte array containing the encoded signature
     * @return the byte array
     */
    byte[] asBytes() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Nat.write(_majorVersion, out);
            Nat.write(_minorVersion, out);
            _table.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new CtxException("Unexpected error converting signature to byte array", e);
        }
    }

    private static final String separator = System.getProperty("line.separator");

    @Override
    public String toString() {
        return "Major version: " + _majorVersion + separator +
                "Minor version: " + _minorVersion + separator + _table.toString();
    }
}

/**
 * Utility functions for reading/writing NAT encoded values.
 * This is a 8-to-7-bit encoding with the high bit used as a continuation marker on all but the final byte.
 */
class Nat {
    static int size(int value) {
        int count = 0;
        do {
            value = value >>7;
            count++;
        } while (value != 0);
        return count;
    }

    static int read(ByteArrayInputStream in) {
        return read(in, 0);
    }

    private static int read(ByteArrayInputStream in, int carry) {
        int b = in.read();
        if (b == -1)
            throw new CtxException("Unexpected EOF in signature data");
        int acc = (carry << 7) + (b & 0x7f);
        if ((b & 0x80) == 0)
            return acc;
        //noinspection SuspiciousNameCombination
        return read(in, acc);
    }

    static void write(int nat, ByteArrayOutputStream out) {
        write(nat, out, 0);
    }

    private static void write(int nat, ByteArrayOutputStream out, int flag) {
        int b = nat & 0x7f;
        int h = nat >> 7;
        if (h != 0) write(h, out, 0x80);
        out.write(b | flag);
    }
}
