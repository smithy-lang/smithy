/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

/**
 * LEB128 variable-length integer encoding for SMF (writer side).
 *
 * <p>VarUInt encodes unsigned 32-bit integers in 1-5 bytes. VarInt encodes
 * signed 64-bit integers using zigzag + LEB128 in 1-10 bytes.
 *
 * <p>The reader decodes VarUInt/VarInt inline against its buffer limit (see
 * {@code SmfReader}) so that malformed input yields a clean
 * {@link SmfFormatException} rather than an unbounded read; this class only
 * provides the writer-side encoders and size helpers.
 */
final class LEB128 {

    private LEB128() {}

    /**
     * Writes an unsigned 32-bit VarUInt to the buffer at the given position.
     *
     * @return the new position after writing.
     */
    static int writeVarUInt(byte[] buf, int pos, int value) {
        while ((value & ~0x7F) != 0) {
            buf[pos++] = (byte) ((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buf[pos++] = (byte) value;
        return pos;
    }

    /**
     * Writes a signed 64-bit VarInt (zigzag-encoded) to the buffer.
     *
     * @return the new position after writing.
     */
    static int writeVarInt(byte[] buf, int pos, long value) {
        // Zigzag encode
        long encoded = (value << 1) ^ (value >> 63);
        while ((encoded & ~0x7FL) != 0) {
            buf[pos++] = (byte) ((encoded & 0x7F) | 0x80);
            encoded >>>= 7;
        }
        buf[pos++] = (byte) encoded;
        return pos;
    }

    /**
     * Returns the number of bytes needed to encode a VarUInt value.
     */
    static int varUIntSize(int value) {
        int size = 1;
        while ((value & ~0x7F) != 0) {
            size++;
            value >>>= 7;
        }
        return size;
    }

    /**
     * Returns the number of bytes needed to encode a VarInt value.
     */
    static int varIntSize(long value) {
        long encoded = (value << 1) ^ (value >> 63);
        int size = 1;
        while ((encoded & ~0x7FL) != 0) {
            size++;
            encoded >>>= 7;
        }
        return size;
    }
}
