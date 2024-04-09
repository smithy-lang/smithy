/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

/**
 *  A meta-trait that marks a trait as an endpoint modifier.
 *
 *  Traits that are marked with this trait are applied to service shapes or operation shapes to
 *  indicate how a client can resolve endpoints for that service or operation.
 */
public final class EndpointModifierTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("aws.endpoints#endpointsModifier");

    public EndpointModifierTrait(ObjectNode node) {
        super(ID, node);
    }

    public EndpointModifierTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<EndpointModifierTrait> {
        public Provider() {
            super(ID, EndpointModifierTrait::new);
        }
    }
}
