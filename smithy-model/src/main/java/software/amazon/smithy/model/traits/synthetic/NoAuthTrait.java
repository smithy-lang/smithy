/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits.synthetic;

import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.ServiceIndex.AuthSchemeMode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

/**
 * An auth scheme trait for {@code smithy.api#noAuth} which indicates no authentication. This is not a real trait
 * in the semantic model, but a valid auth scheme for use in {@link ServiceIndex#getEffectiveAuthSchemes} with
 * {@link AuthSchemeMode#NO_AUTH_AWARE}.
 */
public final class NoAuthTrait extends AnnotationTrait {

    public static final ShapeId ID = ShapeId.from("smithy.api#noAuth");

    public NoAuthTrait() {
        super(ID, Node.objectNode());
    }

    @Override
    public boolean isSynthetic() {
        return true;
    }
}
