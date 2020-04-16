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

import java.util.Collections;
import java.util.function.Function;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Trait implementation for traits that are an empty object.
 */
public abstract class AnnotationTrait extends AbstractTrait {
    public AnnotationTrait(ShapeId id, SourceLocation sourceLocation) {
        super(id, sourceLocation);
    }

    @Override
    protected final Node createNode() {
        return new ObjectNode(Collections.emptyMap(), getSourceLocation());
    }

    /**
     * Trait provider that expects a boolean value of true.
     */
    public static class Provider<T extends AnnotationTrait> extends AbstractTrait.Provider {
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
            if (value.isObjectNode()) {
                return traitFactory.apply(value.getSourceLocation());
            }

            throw new ExpectationNotMetException(String.format(
                    "Annotation traits  must be an object or omitted in the IDL, but found %s",
                    value.getType()), value);
        }
    }
}
