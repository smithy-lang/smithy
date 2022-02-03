/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.traits.auth;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

public final class PresignableTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("aws.auth#presignable");

    public PresignableTrait(ObjectNode node) {
        super(ID, node);
    }

    public PresignableTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<PresignableTrait> {
        public Provider() {
            super(ID, PresignableTrait::new);
        }
    }
}
