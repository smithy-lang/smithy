/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.Objects;
import java.util.Optional;

/** An enumeration of the different types in a model. */
public enum ShapeType {

    BLOB("blob", BlobShape.class, Category.SIMPLE),
    BOOLEAN("boolean", BooleanShape.class, Category.SIMPLE),
    STRING("string", StringShape.class, Category.SIMPLE),
    TIMESTAMP("timestamp", TimestampShape.class, Category.SIMPLE),
    BYTE("byte", ByteShape.class, Category.SIMPLE),
    SHORT("short", ShortShape.class, Category.SIMPLE),
    INTEGER("integer", IntegerShape.class, Category.SIMPLE),
    LONG("long", LongShape.class, Category.SIMPLE),
    FLOAT("float", FloatShape.class, Category.SIMPLE),
    DOCUMENT("document", DocumentShape.class, Category.SIMPLE),
    DOUBLE("double", DoubleShape.class, Category.SIMPLE),
    BIG_DECIMAL("bigDecimal", BigDecimalShape.class, Category.SIMPLE),
    BIG_INTEGER("bigInteger", BigIntegerShape.class, Category.SIMPLE),
    ENUM("enum", EnumShape.class, Category.SIMPLE) {
        @Override
        public boolean isShapeType(ShapeType other) {
            return this == other || other == STRING;
        }
    },
    INT_ENUM("intEnum", IntEnumShape.class, Category.SIMPLE) {
        @Override
        public boolean isShapeType(ShapeType other) {
            return this == other || other == INTEGER;
        }
    },
    LIST("list", ListShape.class, Category.AGGREGATE) {
        @Override
        public boolean isShapeType(ShapeType other) {
            return this == other || other == SET;
        }
    },
    SET("set", SetShape.class, Category.AGGREGATE) {
        @Override
        public boolean isShapeType(ShapeType other) {
            return this == other || other == LIST;
        }
    },
    MAP("map", MapShape.class, Category.AGGREGATE),
    STRUCTURE("structure", StructureShape.class, Category.AGGREGATE),
    UNION("union", UnionShape.class, Category.AGGREGATE),

    MEMBER("member", MemberShape.class, Category.MEMBER),

    SERVICE("service", ServiceShape.class, Category.SERVICE),
    RESOURCE("resource", ResourceShape.class, Category.SERVICE),
    OPERATION("operation", OperationShape.class, Category.SERVICE);

    public enum Category {
        SIMPLE, AGGREGATE, SERVICE, MEMBER
    }

    private final String stringValue;
    private final Class<? extends Shape> shapeClass;
    private final Category category;

    ShapeType(String stringValue, Class<? extends Shape> shapeClass, Category category) {
        this.stringValue = stringValue;
        this.shapeClass = shapeClass;
        this.category = category;
    }

    @Override
    public String toString() {
        return stringValue;
    }

    /**
     * Gets the class that implements this shape type.
     *
     * @return Returns the shape class.
     */
    public Class<? extends Shape> getShapeClass() {
        return shapeClass;
    }

    /**
     * Returns the category of the shape type, as defined in the Smithy
     * specification (one of SIMPLE, AGGREGATE, or SERVICE).
     *
     * @return Returns the category of the type.
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Returns whether this shape type is equivalent to the given shape type,
     * accounting for things like enums being considered specializations of strings.
     *
     * @param other The other shape type to compare against.
     * @return Returns true if the shape types are equivalent.
     */
    public boolean isShapeType(ShapeType other) {
        return Objects.equals(this, other);
    }

    /**
     * Create a new Shape.Type from a string.
     *
     * @param text Text to convert into a Shape.Type
     * @return Returns the Type in an Optional.
     */
    public static Optional<ShapeType> fromString(String text) {
        for (ShapeType e : values()) {
            if (e.stringValue.equals(text)) {
                return Optional.of(e);
            }
        }

        return Optional.empty();
    }

    /**
     * Creates a shape builder that corresponds to the shape type.
     *
     * @return Returns a shape builder corresponding to the type.
     */
    public AbstractShapeBuilder<?, ?> createBuilderForType() {
        switch (this) {
            case BLOB:
                return BlobShape.builder();
            case BOOLEAN:
                return BooleanShape.builder();
            case STRING:
                return StringShape.builder();
            case ENUM:
                return EnumShape.builder();
            case TIMESTAMP:
                return TimestampShape.builder();
            case BYTE:
                return ByteShape.builder();
            case SHORT:
                return ShortShape.builder();
            case INTEGER:
                return IntegerShape.builder();
            case INT_ENUM:
                return IntEnumShape.builder();
            case LONG:
                return LongShape.builder();
            case FLOAT:
                return FloatShape.builder();
            case DOCUMENT:
                return DocumentShape.builder();
            case DOUBLE:
                return DoubleShape.builder();
            case BIG_DECIMAL:
                return BigDecimalShape.builder();
            case BIG_INTEGER:
                return BigIntegerShape.builder();
            case LIST:
                return ListShape.builder();
            case SET:
                return SetShape.builder();
            case MAP:
                return MapShape.builder();
            case STRUCTURE:
                return StructureShape.builder();
            case UNION:
                return UnionShape.builder();
            case SERVICE:
                return ServiceShape.builder();
            case RESOURCE:
                return ResourceShape.builder();
            case OPERATION:
                return OperationShape.builder();
            case MEMBER:
                return MemberShape.builder();
            default:
                throw new IllegalStateException("Invalid shape type: " + this);
        }
    }
}
