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

package software.amazon.smithy.model.loader;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Resolves forward references for parsers, providing them resolved shape IDs and the shape type if found.
 */
@FunctionalInterface
interface IdlReferenceResolver {
    /**
     * Defer the resolution of a shape ID within a namespace.
     *
     * @param name     Name of the shape to resolve.
     * @param receiver Receiver that receives the resolved shape ID and type, or null if the shape was not found.
     *                 The receiver can return a {@link ValidationEvent} if the shape could not be resolved, or it
     *                 can return null if the shape was resolved.
     */
    void resolve(String name, BiFunction<ShapeId, ShapeType, ValidationEvent> receiver);

    /**
     * Defer the resolution of a shape ID within a namespace.
     *
     * @param name     Name of the shape to resolve.
     * @param receiver Receiver that receives the resolved shape ID and type, or null if the shape was not found.
     */
    default void resolve(String name, BiConsumer<ShapeId, ShapeType> receiver) {
        resolve(name, (id, type) -> {
            receiver.accept(id, type);
            return null;
        });
    }
}
