/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import software.amazon.smithy.model.shapes.ShapeType;

/**
 * Constants for the Smithy Model Format (SMF).
 */
final class SmfConstants {

    static final int MAGIC = 0x534D4600; // "SMF\0"
    static final byte FORMAT_VERSION = 0x01;

    // Header offsets
    static final int HEADER_SIZE = 8;
    static final int OFFSET_MAGIC = 0;
    static final int OFFSET_FORMAT_VERSION = 4;
    static final int OFFSET_SMITHY_VERSION_MAJOR = 5;
    static final int OFFSET_SMITHY_VERSION_MINOR = 6;
    static final int OFFSET_FLAGS = 7;

    // Header flags
    static final int FLAG_HAS_METADATA = 0x01;
    static final int FLAG_HAS_SHAPE_INDEX = 0x02;

    // Shape type byte values
    static final byte SHAPE_BLOB = 0x00;
    static final byte SHAPE_BOOLEAN = 0x01;
    static final byte SHAPE_STRING = 0x02;
    static final byte SHAPE_BYTE = 0x03;
    static final byte SHAPE_SHORT = 0x04;
    static final byte SHAPE_INTEGER = 0x05;
    static final byte SHAPE_LONG = 0x06;
    static final byte SHAPE_FLOAT = 0x07;
    static final byte SHAPE_DOUBLE = 0x08;
    static final byte SHAPE_BIG_DECIMAL = 0x09;
    static final byte SHAPE_BIG_INTEGER = 0x0A;
    static final byte SHAPE_TIMESTAMP = 0x0B;
    static final byte SHAPE_DOCUMENT = 0x0C;
    static final byte SHAPE_ENUM = 0x0D;
    static final byte SHAPE_INT_ENUM = 0x0E;
    static final byte SHAPE_LIST = 0x0F;
    static final byte SHAPE_MAP = 0x10;
    static final byte SHAPE_STRUCTURE = 0x11;
    static final byte SHAPE_UNION = 0x12;
    static final byte SHAPE_OPERATION = 0x13;
    static final byte SHAPE_RESOURCE = 0x14;
    static final byte SHAPE_SERVICE = 0x15;

    // Dynamic value type tags
    static final byte VALUE_NULL = 0x00;
    static final byte VALUE_FALSE = 0x01;
    static final byte VALUE_TRUE = 0x02;
    static final byte VALUE_INTEGER = 0x03;
    static final byte VALUE_DOUBLE = 0x04;
    static final byte VALUE_STRING = 0x05;
    static final byte VALUE_LIST = 0x06;
    static final byte VALUE_OBJECT = 0x07;
    static final byte VALUE_EMPTY_OBJECT = 0x08;
    static final byte VALUE_BIG_INTEGER = 0x09;
    static final byte VALUE_BIG_DECIMAL = 0x0A;

    // Operation flags
    static final int OP_HAS_INPUT = 0x01;
    static final int OP_HAS_OUTPUT = 0x02;
    static final int OP_HAS_ERRORS = 0x04;

    // Resource lifecycle flags
    static final int RES_HAS_PUT = 0x01;
    static final int RES_HAS_CREATE = 0x02;
    static final int RES_HAS_READ = 0x04;
    static final int RES_HAS_UPDATE = 0x08;
    static final int RES_HAS_DELETE = 0x10;
    static final int RES_HAS_LIST = 0x20;

    // Fixed-size index entry: symref(4) + type(1) + offset(4) + neighborStart(4) + neighborCount(2) = 15
    static final int INDEX_ENTRY_SIZE = 15;

    private SmfConstants() {}

    /**
     * Converts a ShapeType to its SMF byte value.
     */
    static byte shapeTypeToByte(ShapeType type) {
        switch (type) {
            case BLOB:
                return SHAPE_BLOB;
            case BOOLEAN:
                return SHAPE_BOOLEAN;
            case STRING:
                return SHAPE_STRING;
            case BYTE:
                return SHAPE_BYTE;
            case SHORT:
                return SHAPE_SHORT;
            case INTEGER:
                return SHAPE_INTEGER;
            case LONG:
                return SHAPE_LONG;
            case FLOAT:
                return SHAPE_FLOAT;
            case DOUBLE:
                return SHAPE_DOUBLE;
            case BIG_DECIMAL:
                return SHAPE_BIG_DECIMAL;
            case BIG_INTEGER:
                return SHAPE_BIG_INTEGER;
            case TIMESTAMP:
                return SHAPE_TIMESTAMP;
            case DOCUMENT:
                return SHAPE_DOCUMENT;
            case ENUM:
                return SHAPE_ENUM;
            case INT_ENUM:
                return SHAPE_INT_ENUM;
            case LIST:
                return SHAPE_LIST;
            case MAP:
                return SHAPE_MAP;
            case STRUCTURE:
                return SHAPE_STRUCTURE;
            case UNION:
                return SHAPE_UNION;
            case OPERATION:
                return SHAPE_OPERATION;
            case RESOURCE:
                return SHAPE_RESOURCE;
            case SERVICE:
                return SHAPE_SERVICE;
            default:
                throw new IllegalArgumentException("Unsupported shape type: " + type);
        }
    }

    /**
     * Converts an SMF byte value to a ShapeType.
     */
    static ShapeType byteToShapeType(byte value) {
        switch (value) {
            case SHAPE_BLOB:
                return ShapeType.BLOB;
            case SHAPE_BOOLEAN:
                return ShapeType.BOOLEAN;
            case SHAPE_STRING:
                return ShapeType.STRING;
            case SHAPE_BYTE:
                return ShapeType.BYTE;
            case SHAPE_SHORT:
                return ShapeType.SHORT;
            case SHAPE_INTEGER:
                return ShapeType.INTEGER;
            case SHAPE_LONG:
                return ShapeType.LONG;
            case SHAPE_FLOAT:
                return ShapeType.FLOAT;
            case SHAPE_DOUBLE:
                return ShapeType.DOUBLE;
            case SHAPE_BIG_DECIMAL:
                return ShapeType.BIG_DECIMAL;
            case SHAPE_BIG_INTEGER:
                return ShapeType.BIG_INTEGER;
            case SHAPE_TIMESTAMP:
                return ShapeType.TIMESTAMP;
            case SHAPE_DOCUMENT:
                return ShapeType.DOCUMENT;
            case SHAPE_ENUM:
                return ShapeType.ENUM;
            case SHAPE_INT_ENUM:
                return ShapeType.INT_ENUM;
            case SHAPE_LIST:
                return ShapeType.LIST;
            case SHAPE_MAP:
                return ShapeType.MAP;
            case SHAPE_STRUCTURE:
                return ShapeType.STRUCTURE;
            case SHAPE_UNION:
                return ShapeType.UNION;
            case SHAPE_OPERATION:
                return ShapeType.OPERATION;
            case SHAPE_RESOURCE:
                return ShapeType.RESOURCE;
            case SHAPE_SERVICE:
                return ShapeType.SERVICE;
            default:
                throw new IllegalArgumentException("Unknown SMF shape type byte: 0x"
                        + Integer.toHexString(value & 0xFF));
        }
    }
}
