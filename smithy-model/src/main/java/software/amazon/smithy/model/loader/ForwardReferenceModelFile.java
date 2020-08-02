/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.utils.Pair;

/**
 * A ModelFile that contains forward references.
 *
 * @see IdlModelParser
 */
final class ForwardReferenceModelFile extends AbstractMutableModelFile {

    /** The nullable namespace. Is null until it's set. */
    private String namespace;

    /** A queue of forward references. A queue is used since references can be added during resolution. */
    private final Deque<Pair<String, BiConsumer<ShapeId, Function<ShapeId, ShapeType>>>> forwardReferences
            = new ArrayDeque<>();

    private final Map<String, Pair<ShapeId, SourceLocation>> useShapes = new HashMap<>();

    /**
     * @param traitFactory Factory used to create traits when merging traits.
     */
    ForwardReferenceModelFile(TraitFactory traitFactory) {
        super(traitFactory);
    }

    /**
     * Get the currently set namespace.
     *
     * @return Returns the currently set namespace or {@code null} if not set.
     */
    String namespace() {
        return namespace;
    }

    /**
     * Sets the current namespace.
     *
     * @param namespace Namespace to set.
     */
    void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Invoked when a shape is "used" in the model file.
     *
     * @param id Shape ID to use.
     * @param location The source location of where this use occurred.
     */
    void useShape(ShapeId id, SourceLocation location) {
        // Duplicate use statements.
        if (useShapes.containsKey(id.getName())) {
            ShapeId previous = useShapes.get(id.getName()).left;
            String message = String.format("Cannot use name `%s` because it conflicts with `%s`", id, previous);
            throw new ModelSyntaxException(message, location);
        }

        useShapes.put(id.getName(), Pair.of(id, location));
    }

    @Override
    void onShape(AbstractShapeBuilder<?, ?> builder) {
        if (useShapes.containsKey(builder.getId().getName())) {
            ShapeId previous = useShapes.get(builder.getId().getName()).left;
            String message = String.format("Shape name `%s` conflicts with imported shape `%s`",
                                           builder.getId().getName(), previous);
            throw new ModelSyntaxException(message, builder);
        }

        super.onShape(builder);
    }

    /**
     * Adds a forward reference that will be resolved when
     * {@link #resolveShapes} is called.
     *
     * @param name The name of the shape that needs to be resolved.
     * @param consumer The consumer that receives the resolved shape ID.
     */
    void addForwardReference(String name, Consumer<ShapeId> consumer) {
        forwardReferences.add(Pair.of(name, (id, typeProvider) -> consumer.accept(id)));
    }

    /**
     * Adds a forward reference that will be resolved when
     * {@link #resolveShapes} is called.
     *
     * <p>This variant of {@code addForwardReference} issued when the consumer
     * also needs to know the type of shape that is being resolved. For
     * example, this is necessary in order to coerce annotation traits
     * (traits that define no value) into the expected type for a shape
     * (e.g., a list or object).
     *
     * @param name The name of the shape that needs to be resolved.
     * @param consumer The consumer that receives the resolved shape ID.
     */
    void addForwardReference(String name, BiConsumer<ShapeId, Function<ShapeId, ShapeType>> consumer) {
        forwardReferences.add(Pair.of(name, consumer));
    }

    @Override
    public TraitContainer resolveShapes(Set<ShapeId> ids, Function<ShapeId, ShapeType> typeProvider) {
        while (!forwardReferences.isEmpty()) {
            Pair<String, BiConsumer<ShapeId, Function<ShapeId, ShapeType>>> pair = forwardReferences.pop();
            String name = pair.left;
            BiConsumer<ShapeId, Function<ShapeId, ShapeType>> consumer = pair.right;

            ShapeId resolved;
            // Use absolute IDs as-is.
            if (name.contains("#")) {
                resolved = ShapeId.from(name);
            } else if (useShapes.containsKey(name)) {
                // Check use statements.
                resolved = useShapes.get(name).left;
            } else {
                // Check if there's a shape with this name in the current namespace.
                resolved = ShapeId.from(namespace() + "#" + name);

                // If not defined in the namespace, then check the prelude.
                if (!ids.contains(resolved)) {
                    ShapeId preludeTest = ShapeId.from(Prelude.NAMESPACE + '#' + name);
                    if (ids.contains(preludeTest)) {
                        resolved = preludeTest;
                    }
                }
            }

            consumer.accept(resolved, typeProvider);
        }

        return traitContainer;
    }
}
