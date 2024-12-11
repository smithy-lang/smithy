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
 * An endpoints modifier trait that indicates a service has only dual stack endpoints,
 * does not support IPV4 only endpoints, and should not have the useDualStackEndpoint endpoint parameter.
 */
public final class DualStackOnlyEndpointsTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("aws.endpoints#dualStackOnlyEndpoints");

    public DualStackOnlyEndpointsTrait(ObjectNode node) {
        super(ID, node);
    }

    public DualStackOnlyEndpointsTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<DualStackOnlyEndpointsTrait> {
        public Provider() {
            super(ID, DualStackOnlyEndpointsTrait::new);
        }
    }
}
