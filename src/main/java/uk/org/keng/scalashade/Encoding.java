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

/**
 * This encoding/decoding is based on http://lampwww.epfl.ch/~dubochet/new_pickle.pdf. It is used in the ScalaSignature
 * class annotation to encode type information. An implementation of the encoding can be found in
 * scala.reflect.internal.pickling.Encoding
 * <p/>
 * Briefly encoding is:
 * 1. Encode input bytes into bytes using only 7-bits to give 0 to 127 range
 * 2. Add 1 to encoded bytes and then replace any 0 by 0xc0 0x80 to remove 0 from encoding
 * 3. Convert bytes to string
 */

class Encoding {

    /**
     * Test a string to see if it is a valid encoding, i.e. has character in correct range
     *
     * @param encoded the string to test
     * @return true is valid
     */
    public static boolean isValidEncoding(String encoded) {
        for (int charIndex = 0; charIndex < encoded.length(); charIndex++) {
            char c = encoded.charAt(charIndex);
            //noinspection StatementWithEmptyBody
            if (c < 128) {
                // OK, normal 7-bit character
            } else {
                if (c == 0xc0 && charIndex + 1 < encoded.length() && encoded.charAt(charIndex + 1) == 0x80) {
                    // OK, sequence for 0
                    charIndex++;
                } else {
                    // Everything else is illegal
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Encode bytes into a string using the scheme.
     *
     * @param raw input bytes
     * @return encoding of raw
     */
    public static String encode(byte[] raw) {

        // Convert to 7-bit
        byte[] encoded = new byte[encodeLength(raw.length)];
        for (int i = 0; i < raw.length; i++) {
            encodeByte(encoded, i, raw[i]);
        }

        // Encode that in String
        StringBuilder sb = new StringBuilder();
        for (byte b : encoded) {
            char c = (char) ((b + 1) & 0x7F);
            if (c != 0) {
                sb.append(c);
            } else {
                sb.append((char) 0xc0);
                sb.append((char) 0x80);
            }
        }
        return sb.toString();
    }

    /**
     * Decode a string back to a byte representation
     *
     * @param encoded the encoded bytes
     * @return the raw bytes
     */
    public static byte[] decode(String encoded) {

        // Convert to byte array while validating and removing outer coding
        byte[] input = new byte[encoded.length()];
        int byteIndex = 0;
        for (int charIndex = 0; charIndex < encoded.length(); charIndex++) {
            char c = encoded.charAt(charIndex);
            if (c < 128) {
                input[byteIndex++] = (byte) ((c - 1) & 0x7F);
            } else {
                if (c == 0xc0 && charIndex + 1 < encoded.length() && encoded.charAt(charIndex + 1) == 0x80) {
                    input[byteIndex++] = 0;
                    charIndex++;
                } else {
                    return null;
                }
            }
        }

        // Convert back to 8-bit
        byte[] output = new byte[decodeLength(byteIndex)];
        for (int i = 0; i < output.length; i++) {
            output[i] = decodeByte(input, i);
        }
        return output;
    }

    /**
     * Calculate the length of an 7-to-8 bit encoding from the input. Note: This is just
     * the 7-to-8 bit encoding & does not take into account the two byte encoding used
     * for zero.
     *
     * @param inputLength how many bytes are to be encoded
     * @return size of resulting encoding
     */
    private static int encodeLength(int inputLength) {
        int rem = (inputLength % 7);
        return ((inputLength / 7) * 8) + (rem > 0 ? rem + 1 : 0);
    }

    /**
     * Calculate the number of decoded bytes given the encoded length. Note: This is just
     * the 7-to-8 bit encoding & does not take into account the two byte encoding used
     * for zero.
     *
     * @param inputLength how large is the encoded string
     * @return size of decoded bytes
     * @throws CtxException if inputLength can not be correct for an encoded buffer
     */
    private static int decodeLength(int inputLength) {
        int rem = (inputLength % 8);
        if (rem == 1)
            throw new CtxException("Input length is not valid for encoded data");
        return ((inputLength / 8) * 7) + (rem > 0 ? rem - 1 : 0);
    }

    /**
     * Encode a byte into an encoded byte array
     *
     * @param buffer the encoded data, this must be at least encodeLength(input.length) in size
     * @param at the ordinal of the 8-bit value to be set, 0 to input.length
     * @param value the value to encode into the array
     */
    private static void encodeByte(byte[] buffer, int at, byte value) {
        // Calc encoded start byte and how many low bits encoded
        // High bits are in following byte
        int lowBits = 7 - (at % 7);
        int startByte = (at / 7) * 8 + (at % 7);

        // Update buffer using a mask that excludes low bits & top bit which is dead
        byte lowMask = (byte) (0x7f & ~((1 << (7 - lowBits)) - 1));
        byte low = (byte) ((value << (7 - lowBits)) & lowMask);
        buffer[startByte] = (byte) ((buffer[startByte] & ~lowMask) + low);

        // Same again for high bits but into next buffer byte
        byte highMask = (byte) (0x7f & ~((1 << (8 - lowBits)) - 1));
        byte high = (byte) (0x7f & ((value >> lowBits) & ~highMask));
        buffer[startByte + 1] = (byte) ((buffer[startByte + 1] & highMask) + high);
    }

    /**
     * Extract a byte in an encoded byte array.
     *
     * @param buffer the encoded data
     * @param at     the ordinal of the 8-byte value to be extracted, 0 to decodeLength(buffer)
     * @return the extracted byte
     */
    private static byte decodeByte(byte[] buffer, int at) {
        // Calc encoded start byte and how many low bits encoded
        // High bits are in following byte
        int lowBits = 7 - (at % 7);
        int startByte = (at / 7) * 8 + (at % 7);

        // Extract low bit and high bits, shift, and bring back together
        byte low = (byte) (buffer[startByte] >> (7 - lowBits));
        byte high = (byte) (buffer[startByte + 1] << lowBits);
        return (byte) (high + low);
    }
}

