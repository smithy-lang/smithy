/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.shapes;

import java.util.Optional;

/** An enumeration of the different types in a model. */
public enum ShapeType {

    BLOB("blob", BlobShape.class),
    BOOLEAN("boolean", BooleanShape.class),
    STRING("string", StringShape.class),
    TIMESTAMP("timestamp", TimestampShape.class),
    BYTE("byte", ByteShape.class),
    SHORT("short", ShortShape.class),
    INTEGER("integer", IntegerShape.class),
    LONG("long", LongShape.class),
    FLOAT("float", FloatShape.class),
    DOCUMENT("document", DocumentShape.class),
    DOUBLE("double", DoubleShape.class),
    BIG_DECIMAL("bigDecimal", BigDecimalShape.class),
    BIG_INTEGER("bigInteger", BigIntegerShape.class),
    LIST("list", ListShape.class),
    SET("set", SetShape.class),
    MAP("map", MapShape.class),
    STRUCTURE("structure", StructureShape.class),
    UNION("union", UnionShape.class),
    SERVICE("service", ServiceShape.class),
    RESOURCE("resource", ResourceShape.class),
    OPERATION("operation", OperationShape.class),
    MEMBER("member", MemberShape.class);

    private final String stringValue;
    private final Class<? extends Shape> shapeClass;

    ShapeType(String stringValue, Class<? extends Shape> shapeClass) {
        this.stringValue = stringValue;
        this.shapeClass = shapeClass;
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
}
