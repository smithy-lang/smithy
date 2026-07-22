/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

/**
 * LEB128 variable-length integer encoding for SMF.
 *
 * <p>VarUInt encodes unsigned 32-bit integers in 1-5 bytes.
 * VarInt encodes signed 64-bit integers using zigzag + LEB128 in 1-10 bytes.
 */
final class LEB128 {

    private static final int VARUINT_MAX_BYTES = 5;
    private static final int VARINT_MAX_BYTES = 10;

    private LEB128() {}

    /**
     * Reads an unsigned 32-bit VarUInt from the buffer at the given position.
     *
     * @param buf the byte array to read from.
     * @param pos the position to start reading.
     * @return the decoded value in the lower 32 bits, and the new position
     *         encoded as {@code ((long) newPos << 32) | (value & 0xFFFFFFFFL)}.
     *         Use {@link #value(long)} and {@link #position(long)} to extract.
     */
    static long readVarUInt(byte[] buf, int pos) {
        int result = 0;
        int shift = 0;
        int startPos = pos;
        while (true) {
            if (pos - startPos >= VARUINT_MAX_BYTES) {
                throw new IllegalArgumentException("VarUInt encoding exceeds 5 bytes");
            }
            int b = buf[pos++] & 0xFF;
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return pack(pos, result);
            }
            shift += 7;
        }
    }

    /**
     * Reads a signed 64-bit VarInt and returns the decoded value.
     * The new buffer position can be computed by calling this method's
     * companion {@link #varIntSize(byte[], int)}.
     */
    static long readVarInt(byte[] buf, int pos) {
        long result = 0;
        int shift = 0;
        int startPos = pos;
        while (true) {
            if (pos - startPos >= VARINT_MAX_BYTES) {
                throw new IllegalArgumentException("VarInt encoding exceeds 10 bytes");
            }
            long b = buf[pos++] & 0xFFL;
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return (result >>> 1) ^ -(result & 1);
            }
            shift += 7;
        }
    }

    /**
     * Reads a signed 64-bit VarInt and returns the decoded value and new position.
     * Use {@link #varIntValue(long[])} and {@link #varIntPosition(long[])} to extract.
     *
     * @param out a 2-element array: out[0] = decoded value, out[1] = new position.
     */
    static void readVarInt(byte[] buf, int pos, long[] out) {
        long result = 0;
        int shift = 0;
        int startPos = pos;
        while (true) {
            if (pos - startPos >= VARINT_MAX_BYTES) {
                throw new IllegalArgumentException("VarInt encoding exceeds 10 bytes");
            }
            long b = buf[pos++] & 0xFFL;
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                out[0] = (result >>> 1) ^ -(result & 1);
                out[1] = pos;
                return;
            }
            shift += 7;
        }
    }

    /**
     * Returns the number of bytes a VarInt occupies starting at pos.
     */
    static int varIntSize(byte[] buf, int pos) {
        int start = pos;
        while ((buf[pos++] & 0x80) != 0) {
            if (pos - start >= VARINT_MAX_BYTES) {
                throw new IllegalArgumentException("VarInt encoding exceeds 10 bytes");
            }
        }
        return pos - start;
    }

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

    // Pack position (upper 32) and unsigned value (lower 32) into a long.
    private static long pack(int pos, int value) {
        return ((long) pos << 32) | (value & 0xFFFFFFFFL);
    }

    /**
     * Extracts the decoded unsigned value from a packed readVarUInt result.
     */
    static int value(long packed) {
        return (int) packed;
    }

    /**
     * Extracts the new buffer position from a packed readVarUInt result.
     */
    static int position(long packed) {
        return (int) (packed >>> 32);
    }
}
