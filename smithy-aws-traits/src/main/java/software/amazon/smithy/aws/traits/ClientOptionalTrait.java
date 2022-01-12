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

package software.amazon.smithy.aws.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

/**
 * Indicates that a structure member should be treated as optional in
 * generated AWS clients, ignoring any required or default traits.
 */
public final class ClientOptionalTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("aws.api#clientOptional");

    public ClientOptionalTrait(ObjectNode node) {
        super(ID, node);
    }

    public ClientOptionalTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<ClientOptionalTrait> {
        public Provider() {
            super(ID, ClientOptionalTrait::new);
        }
    }
}
