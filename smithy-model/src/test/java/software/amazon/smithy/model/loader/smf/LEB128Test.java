/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class LEB128Test {

    // --- VarUInt round-trip ---

    @ParameterizedTest
    @ValueSource(ints = {0,
            1,
            63,
            127,
            128,
            255,
            256,
            16383,
            16384,
            65535,
            1_000_000,
            Integer.MAX_VALUE})
    public void varUIntRoundTrip(int value) {
        byte[] buf = new byte[5];
        int written = LEB128.writeVarUInt(buf, 0, value);
        long packed = LEB128.readVarUInt(buf, 0);
        assertEquals(value, LEB128.value(packed));
        assertEquals(written, LEB128.position(packed));
    }

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
    public void varUIntNegativeOneAsUnsigned() {
        // -1 as unsigned int is 0xFFFFFFFF (max uint32)
        byte[] buf = new byte[5];
        int written = LEB128.writeVarUInt(buf, 0, -1);
        assertEquals(5, written);
        long packed = LEB128.readVarUInt(buf, 0);
        assertEquals(-1, LEB128.value(packed)); // bits preserved
    }

    @Test
    public void varUIntRejectsEncodingLongerThanFiveBytes() {
        // Craft a 6-byte encoding (all continuation bits set)
        byte[] buf = {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0x01};
        assertThrows(IllegalArgumentException.class, () -> LEB128.readVarUInt(buf, 0));
    }

    @Test
    public void varUIntReadsAtOffset() {
        byte[] buf = new byte[10];
        buf[0] = 0x42; // garbage
        LEB128.writeVarUInt(buf, 3, 300);
        long packed = LEB128.readVarUInt(buf, 3);
        assertEquals(300, LEB128.value(packed));
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

    // --- VarInt round-trip ---

    @ParameterizedTest
    @ValueSource(longs = {0,
            1,
            -1,
            63,
            -64,
            127,
            -128,
            128,
            -129,
            16383,
            -16384,
            1_000_000,
            -1_000_000,
            Long.MAX_VALUE,
            Long.MIN_VALUE})
    public void varIntRoundTrip(long value) {
        byte[] buf = new byte[10];
        int written = LEB128.writeVarInt(buf, 0, value);
        long decoded = LEB128.readVarInt(buf, 0);
        assertEquals(value, decoded);
        assertEquals(written, LEB128.varIntSize(buf, 0) + 0); // size matches written
    }

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
        int written = LEB128.writeVarInt(buf, 0, Long.MAX_VALUE);
        assertEquals(10, written);
    }

    @Test
    public void varIntLongMinIsTenBytes() {
        byte[] buf = new byte[10];
        int written = LEB128.writeVarInt(buf, 0, Long.MIN_VALUE);
        assertEquals(10, written);
    }

    @Test
    public void varIntRejectsEncodingLongerThanTenBytes() {
        // Craft an 11-byte encoding
        byte[] buf = new byte[11];
        for (int i = 0; i < 10; i++) {
            buf[i] = (byte) 0x80;
        }
        buf[10] = 0x01;
        assertThrows(IllegalArgumentException.class, () -> LEB128.readVarInt(buf, 0));
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
