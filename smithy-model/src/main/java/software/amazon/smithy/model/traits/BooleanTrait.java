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

import java.util.function.Function;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Trait implementation that expects an empty object or a boolean
 * value of true.
 */
public abstract class BooleanTrait extends AbstractTrait {
    public BooleanTrait(ShapeId id, SourceLocation sourceLocation) {
        super(id, sourceLocation);
    }

    @Override
    protected final Node createNode() {
        return new BooleanNode(true, getSourceLocation());
    }

    /**
     * Trait provider that expects a boolean value of true.
     */
    public static class Provider<T extends BooleanTrait> extends AbstractTrait.Provider {
        private final Function<SourceLocation, T> traitFactory;

        /**
         * @param id ID of the trait being created.
         * @param traitFactory Factory function used to create the trait.
         */
        public Provider(ShapeId id, Function<SourceLocation, T> traitFactory) {
            super(id);
            this.traitFactory = traitFactory;
        }

        @Override
        public T createTrait(ShapeId id, Node value) {
            if (value.isObjectNode() && value.expectObjectNode().getMembers().isEmpty()) {
                return traitFactory.apply(value.getSourceLocation());
            }

            BooleanNode booleanNode = value.expectBooleanNode();
            if (!booleanNode.getValue()) {
                throw new SourceException(String.format(
                        "Boolean trait `%s` expects a value of `true`, but found `false`", getShapeId()), value);
            }

            return traitFactory.apply(value.getSourceLocation());
        }
    }
}
