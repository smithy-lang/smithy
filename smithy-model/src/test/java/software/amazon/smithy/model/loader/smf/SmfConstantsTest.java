/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.ShapeType;

public class SmfConstantsTest {

    @Test
    public void allShapeTypesExceptMemberAreMapped() {
        // MEMBER and SET are not supported in SMF
        EnumSet<ShapeType> expected = EnumSet.allOf(ShapeType.class);
        expected.remove(ShapeType.MEMBER);
        expected.remove(ShapeType.SET);

        for (ShapeType type : expected) {
            byte b = SmfConstants.shapeTypeToByte(type);
            ShapeType roundTripped = SmfConstants.byteToShapeType(b);
            assertEquals(type,
                    roundTripped,
                    "Round-trip failed for " + type);
        }
    }

    @Test
    public void memberShapeTypeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> SmfConstants.shapeTypeToByte(ShapeType.MEMBER));
    }

    @Test
    public void noShapeTypeByteCollisions() {
        Set<Byte> seen = new HashSet<>();
        EnumSet<ShapeType> types = EnumSet.allOf(ShapeType.class);
        types.remove(ShapeType.MEMBER);

        types.remove(ShapeType.SET);
        for (ShapeType type : types) {
            byte b = SmfConstants.shapeTypeToByte(type);
            assertTrue(seen.add(b),
                    "Collision: " + type + " maps to same byte as another type");
        }
    }

    @Test
    public void unknownShapeTypeByteThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> SmfConstants.byteToShapeType((byte) 0x17));
        assertThrows(IllegalArgumentException.class,
                () -> SmfConstants.byteToShapeType((byte) 0xFF));
    }

    @Test
    public void shapeTypeByteValuesAreContiguous() {
        // Shape types should be 0x00 through 0x16 with no gaps
        EnumSet<ShapeType> types = EnumSet.allOf(ShapeType.class);
        types.remove(ShapeType.MEMBER);

        types.remove(ShapeType.SET);
        Set<Byte> values = new HashSet<>();
        for (ShapeType type : types) {
            values.add(SmfConstants.shapeTypeToByte(type));
        }

        int expectedCount = types.size();
        byte min = Byte.MAX_VALUE;
        byte max = Byte.MIN_VALUE;
        for (byte b : values) {
            if (b < min)
                min = b;
            if (b > max)
                max = b;
        }
        assertEquals(0x00, min);
        assertEquals(expectedCount - 1, max);
    }

    @Test
    public void valueTagsAreDistinct() {
        Set<Byte> tags = new HashSet<>();
        tags.add(SmfConstants.VALUE_NULL);
        tags.add(SmfConstants.VALUE_FALSE);
        tags.add(SmfConstants.VALUE_TRUE);
        tags.add(SmfConstants.VALUE_INTEGER);
        tags.add(SmfConstants.VALUE_DOUBLE);
        tags.add(SmfConstants.VALUE_STRING);
        tags.add(SmfConstants.VALUE_LIST);
        tags.add(SmfConstants.VALUE_OBJECT);
        tags.add(SmfConstants.VALUE_EMPTY_OBJECT);
        tags.add(SmfConstants.VALUE_BIG_INTEGER);
        tags.add(SmfConstants.VALUE_BIG_DECIMAL);
        assertEquals(11, tags.size(), "Value tags have collisions");
    }

    @Test
    public void headerSizeIsEight() {
        assertEquals(8, SmfConstants.HEADER_SIZE);
    }

    @Test
    public void magicNumberMatchesAsciiSMBY() {
        assertEquals('S', (SmfConstants.MAGIC >> 24) & 0xFF);
        assertEquals('M', (SmfConstants.MAGIC >> 16) & 0xFF);
        assertEquals('F', (SmfConstants.MAGIC >> 8) & 0xFF);
        assertEquals(0, SmfConstants.MAGIC & 0xFF);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
