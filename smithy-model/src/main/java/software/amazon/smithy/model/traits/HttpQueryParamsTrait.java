/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
 * Binds a map structure member to the HTTP query string.
 */
public class HttpQueryParamsTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#httpQueryParams");


    public HttpQueryParamsTrait(ObjectNode node) {
        super(ID, node);
    }

    public HttpQueryParamsTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<HttpQueryParamsTrait> {
        public Provider() {
            super(ID, HttpQueryParamsTrait::new);
        }
    }
}
