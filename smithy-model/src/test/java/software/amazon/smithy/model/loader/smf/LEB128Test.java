/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for the LEB128 writer-side encoders and size helpers. Decoding is
 * exercised through {@code SmfReader} (which decodes inline against a buffer
 * limit); see {@code SmfReaderTest} for malformed-input decode coverage.
 */
public class LEB128Test {

    // --- VarUInt ---

    @Test
    public void varUIntZeroIsSingleByte() {
        byte[] buf = new byte[5];
        int written = LEB128.writeVarUInt(buf, 0, 0);
        assertEquals(1, written);
        assertEquals(0x00, buf[0]);
    }

    @Test
    public void varUInt127IsSingleByte() {
        byte[] buf = new byte[5];
        int written = LEB128.writeVarUInt(buf, 0, 127);
        assertEquals(1, written);
        assertEquals(0x7F, buf[0] & 0xFF);
    }

    @Test
    public void varUInt128IsTwoBytes() {
        byte[] buf = new byte[5];
        int written = LEB128.writeVarUInt(buf, 0, 128);
        assertEquals(2, written);
        assertEquals(0x80, buf[0] & 0xFF);
        assertEquals(0x01, buf[1] & 0xFF);
    }

    @Test
    public void varUIntMaxValueIsFiveBytes() {
        byte[] buf = new byte[5];
        int written = LEB128.writeVarUInt(buf, 0, Integer.MAX_VALUE);
        assertEquals(5, written);
    }

    @Test
    public void varUIntNegativeOneAsUnsignedIsFiveBytes() {
        // -1 as unsigned int is 0xFFFFFFFF (max uint32)
        byte[] buf = new byte[5];
        int written = LEB128.writeVarUInt(buf, 0, -1);
        assertEquals(5, written);
    }

    @Test
    public void varUIntWritesAtOffset() {
        byte[] buf = new byte[10];
        buf[0] = 0x42; // garbage
        int end = LEB128.writeVarUInt(buf, 3, 300);
        assertEquals(0x42, buf[0]);
        assertEquals(3 + 2, end); // 300 encodes in 2 bytes
    }

    // --- VarUInt size calculation ---

    @Test
    public void varUIntSizeCalculation() {
        assertEquals(1, LEB128.varUIntSize(0));
        assertEquals(1, LEB128.varUIntSize(127));
        assertEquals(2, LEB128.varUIntSize(128));
        assertEquals(2, LEB128.varUIntSize(16383));
        assertEquals(3, LEB128.varUIntSize(16384));
        assertEquals(5, LEB128.varUIntSize(Integer.MAX_VALUE));
        assertEquals(5, LEB128.varUIntSize(-1)); // unsigned max
    }

    // --- VarInt ---

    @Test
    public void varIntZeroIsSingleByte() {
        byte[] buf = new byte[10];
        int written = LEB128.writeVarInt(buf, 0, 0);
        assertEquals(1, written);
        assertEquals(0x00, buf[0]);
    }

    @Test
    public void varIntMinusOneIsSingleByte() {
        byte[] buf = new byte[10];
        int written = LEB128.writeVarInt(buf, 0, -1);
        assertEquals(1, written);
        assertEquals(0x01, buf[0] & 0xFF); // zigzag(-1) = 1
    }

    @Test
    public void varIntOneIsSingleByte() {
        byte[] buf = new byte[10];
        int written = LEB128.writeVarInt(buf, 0, 1);
        assertEquals(1, written);
        assertEquals(0x02, buf[0] & 0xFF); // zigzag(1) = 2
    }

    @Test
    public void varIntLongMaxIsTenBytes() {
        byte[] buf = new byte[10];
        assertEquals(10, LEB128.writeVarInt(buf, 0, Long.MAX_VALUE));
    }

    @Test
    public void varIntLongMinIsTenBytes() {
        byte[] buf = new byte[10];
        assertEquals(10, LEB128.writeVarInt(buf, 0, Long.MIN_VALUE));
    }

    // --- VarInt size calculation ---

    @Test
    public void varIntSizeCalculation() {
        assertEquals(1, LEB128.varIntSize(0));
        assertEquals(1, LEB128.varIntSize(-1));
        assertEquals(1, LEB128.varIntSize(1));
        assertEquals(1, LEB128.varIntSize(63));
        assertEquals(1, LEB128.varIntSize(-64));
        assertEquals(2, LEB128.varIntSize(64));
        assertEquals(2, LEB128.varIntSize(-65));
        assertEquals(10, LEB128.varIntSize(Long.MAX_VALUE));
        assertEquals(10, LEB128.varIntSize(Long.MIN_VALUE));
    }

    // --- Zigzag encoding verification ---

    @Test
    public void zigzagEncodingIsCorrect() {
        // Verify the zigzag pattern: 0->0, -1->1, 1->2, -2->3, 2->4, ...
        byte[] buf = new byte[10];
        LEB128.writeVarInt(buf, 0, 0);
        assertEquals(0x00, buf[0] & 0xFF);
        LEB128.writeVarInt(buf, 0, -1);
        assertEquals(0x01, buf[0] & 0xFF);
        LEB128.writeVarInt(buf, 0, 1);
        assertEquals(0x02, buf[0] & 0xFF);
        LEB128.writeVarInt(buf, 0, -2);
        assertEquals(0x03, buf[0] & 0xFF);
        LEB128.writeVarInt(buf, 0, 2);
        assertEquals(0x04, buf[0] & 0xFF);
    }
}
