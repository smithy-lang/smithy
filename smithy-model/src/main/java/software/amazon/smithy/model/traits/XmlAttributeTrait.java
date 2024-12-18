/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
* Marks a structure member to be serialized to/from an XML attribute.
*/
public final class XmlAttributeTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#xmlAttribute");

    public XmlAttributeTrait(ObjectNode node) {
        super(ID, node);
    }

    public XmlAttributeTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<XmlAttributeTrait> {
        public Provider() {
            super(ID, XmlAttributeTrait::new);
        }
    }
}
