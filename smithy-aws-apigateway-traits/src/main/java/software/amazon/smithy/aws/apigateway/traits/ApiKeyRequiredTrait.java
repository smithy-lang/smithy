/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

/**
 * Indicates that an operation requires an API key for API Gateway usage
 * plan enforcement.
 */
public final class ApiKeyRequiredTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("aws.apigateway#apiKeyRequired");

    public ApiKeyRequiredTrait(ObjectNode node) {
        super(ID, node);
    }

    public ApiKeyRequiredTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<ApiKeyRequiredTrait> {
        public Provider() {
            super(ID, ApiKeyRequiredTrait::new);
        }
    }
}
