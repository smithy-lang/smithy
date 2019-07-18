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

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Defines a custom HTTP status code for error structures.
 */
public final class HttpErrorTrait extends AbstractTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#httpError");

    private final int code;

    public HttpErrorTrait(int code, FromSourceLocation sourceLocation) {
        super(ID, sourceLocation);
        this.code = code;
    }

    public HttpErrorTrait(int code) {
        this(code, SourceLocation.NONE);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            return new HttpErrorTrait(value.expectNumberNode().getValue().intValue(), value.getSourceLocation());
        }
    }

    public int getCode() {
        return code;
    }

    @Override
    protected Node createNode() {
        return new NumberNode(code, getSourceLocation());
    }
}
