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

    BLOB("blob"),
    BOOLEAN("boolean"),
    STRING("string"),
    TIMESTAMP("timestamp"),
    BYTE("byte"),
    SHORT("short"),
    INTEGER("integer"),
    LONG("long"),
    FLOAT("float"),
    DOCUMENT("document"),
    DOUBLE("double"),
    BIG_DECIMAL("bigDecimal"),
    BIG_INTEGER("bigInteger"),
    LIST("list"),
    SET("set"),
    MAP("map"),
    STRUCTURE("structure"),
    UNION("union"),
    SERVICE("service"),
    RESOURCE("resource"),
    OPERATION("operation"),
    MEMBER("member");

    private final String stringValue;

    ShapeType(String stringValue) {
        this.stringValue = stringValue;
    }

    @Override
    public String toString() {
        return stringValue;
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
