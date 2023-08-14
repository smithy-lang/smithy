/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
