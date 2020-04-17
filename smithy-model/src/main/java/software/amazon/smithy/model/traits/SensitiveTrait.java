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

package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that the data stored in the shape or member is sensitive and
 * should be handled with care.
 */
public final class SensitiveTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#sensitive");

    public SensitiveTrait(ObjectNode node) {
        super(ID, node);
    }

    public SensitiveTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<SensitiveTrait> {
        public Provider() {
            super(ID, SensitiveTrait::new);
        }
    }
}
