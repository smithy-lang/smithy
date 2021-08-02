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

package software.amazon.smithy.model.loader;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Represents a model file as defined in the Smithy specification.
 *
 * <p>A model file is used as a self-contained scope of shapes and
 * metadata. Model files are created in isolation, then merged by
 * a {@link ModelAssembler}.
 */
interface ModelFile {

    /**
     * Gets the shape IDs that are defined in this ModelFile.
     *
     * <p>This is called before any other method in the ModelFile.
     *
     * @return Returns the shape IDs defined in this file.
     */
    Set<ShapeId> shapeIds();

    /**
     * Gets the {@link ShapeType} of a shape by ID.
     *
     * <p>This is used, for example, to coerce annotation traits into the
     * appropriate type when parsing trait node values.
     *
     * @param id Shape ID to check.
     * @return Returns the {@link ShapeType} if known, or {@code null} if not found.
     */
    ShapeType getShapeType(ShapeId id);

    /**
     * Get the metadata defined in the ModelFile.
     *
     * @return Returns the defined, non-null metadata.
     */
    Map<String, Node> metadata();

    /**
     * Resolves any forward references and returns all of the traits that were
     * applied to shapes in this ModelFile.
     *
     * @param ids All of the shape IDs found across all ModelFiles being assembled.
     * @param typeProvider A function that can return type information about shapes.
     * @return Returns a container of traits to apply to shapes.
     */
    TraitContainer resolveShapes(Set<ShapeId> ids, Function<ShapeId, ShapeType> typeProvider);

    /**
     * Finalizes and creates shapes in the ModelFile.
     *
     * <p>This is called after {@link #resolveShapes}.
     *
     * @param resolvedTraits Traits to apply to the shapes in the ModelFile.
     * @return Returns the created shapes.
     */
    CreatedShapes createShapes(TraitContainer resolvedTraits);

    /**
     * Gets a mutable list of {@link ValidationEvent} objects encountered when
     * loading this ModelFile.
     *
     * @return Returns the list of events.
     */
    List<ValidationEvent> events();

    /**
     * Return value of creating shapes from a {@link ModelFile}.
     */
    final class CreatedShapes {

        private final Collection<Shape> shapes;
        private final List<PendingShape> pending;

        CreatedShapes(Collection<Shape> shapes, List<PendingShape> pending) {
            this.shapes = shapes;
            this.pending = pending;
        }

        CreatedShapes(Collection<Shape> shapes) {
            this(shapes, Collections.emptyList());
        }

        /**
         * Gets the shapes that were created.
         *
         * @return Returns created shapes.
         */
        Collection<Shape> getCreatedShapes() {
            return shapes;
        }

        /**
         * Gets the shapes that are pending other shapes to resolve as mixins.
         *
         * @return Returns the pending shapes.
         */
        List<PendingShape> getPendingShapes() {
            return pending;
        }
    }
}
