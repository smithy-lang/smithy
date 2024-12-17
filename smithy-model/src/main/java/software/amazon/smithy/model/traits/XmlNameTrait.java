/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;

/**
* Provides a custom name to use when serializing a structure member
* name as a XML object property.
*/
public final class XmlNameTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#xmlName");

    public XmlNameTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public XmlNameTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<XmlNameTrait> {
        public Provider() {
            super(ID, XmlNameTrait::new);
        }
    }
}
