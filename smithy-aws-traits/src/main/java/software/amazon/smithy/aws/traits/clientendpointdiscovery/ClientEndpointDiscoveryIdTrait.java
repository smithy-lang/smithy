/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.clientendpointdiscovery;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

/**
 * Indicates members of the operation input which should be use to discover
 * endpoints.
 */
public final class ClientEndpointDiscoveryIdTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("aws.api#clientEndpointDiscoveryId");

    public ClientEndpointDiscoveryIdTrait(ObjectNode node) {
        super(ID, node);
    }

    public ClientEndpointDiscoveryIdTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<ClientEndpointDiscoveryIdTrait> {
        public Provider() {
            super(ID, ClientEndpointDiscoveryIdTrait::new);
        }
    }
}
