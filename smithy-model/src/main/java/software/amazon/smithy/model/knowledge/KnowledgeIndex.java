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

package software.amazon.smithy.model.knowledge;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StructureShape;

/**
 * Contains an index of computed knowledge about a {@link Model}.
 *
 * <p>A KnowledgeIndex is created to reduce code duplication and
 * complexity of extracting and computing information about a model.
 * A KnowledgeIndex is often a mapping of {@link ShapeId} to some kind of
 * interesting computed information. For example, in order to resolve the
 * input/output/error structures referenced by an {@link OperationShape},
 * you need a {@link ShapeIndex}, to ensure that the reference from the
 * operation to the structure is resolvable in the shape index, that the
 * shape it references is a structure, and then to cast the shape to a
 * {@link StructureShape}. Because this process is error prone, verbose,
 * and is required by a large number of validators and tools, Smithy
 * provides a {@link OperationIndex} to compute it automatically.
 *
 * <p>The {@link Model#getKnowledge} method should be used to create instances
 * of a KnowledgeIndex. Creating instances of a KnowledgeIndex using this
 * method reduces the number of times the results of a KnowledgeIndex needs
 * to be computed for a specific Model. In order to use this method,
 * implementations of a KnowledgeIndex must provide a public constructor
 * that accepts a {@link Model}.
 */
public interface KnowledgeIndex {
    /**
     * Creates a KnowledgeIndex for a particular class.
     *
     * @param model Model to provide to the knowledge index constructor.
     * @param type Class to create.
     * @param <T> Type of knowledge index to create.
     * @return Returns the created KnowledgeIndex.
     * @throws RuntimeException if the index cannot be created.
     */
    @SuppressWarnings("unchecked")
    static <T extends KnowledgeIndex> T create(Class<T> type, Model model) {
        try {
            return type.getConstructor(Model.class).newInstance(model);
        } catch (NoSuchMethodException e) {
            String message = String.format(
                    "KnowledgeIndex for type `%s` does not expose a public constructor that accepts a Model", type);
            throw new RuntimeException(message, e);
        } catch (ReflectiveOperationException e) {
            String message = String.format(
                    "Unable to create a KnowledgeIndex for type `%s`: %s", type, e.getMessage());
            throw new RuntimeException(message, e);
        }
    }
}
