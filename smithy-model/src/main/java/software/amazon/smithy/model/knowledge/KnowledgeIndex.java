/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import java.util.function.Function;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;

/**
 * A marker interface used to indicate that a class contains an index of
 * computed knowledge about a {@link Model}.
 *
 * <p>The purpose of a KnowledgeIndex is to reduce code duplication and
 * complexity of extracting and computing information about a model.
 * A KnowledgeIndex is often a mapping of {@link ShapeId} to some kind of
 * interesting computed information. For example, in order to resolve the
 * input/output/error structures referenced by an {@link OperationShape},
 * you need a {@link Model}, to ensure that the reference from the
 * operation to the structure is resolvable in the model, that the
 * shape it references is a structure, and then to cast the shape to a
 * {@link StructureShape}. Because this process can be complex and is
 * required by a large number of validators and tools, Smithy provides an
 * {@link OperationIndex} to compute it automatically.
 *
 * <p>By convention, each KnowledgeIndex should provide a public static method
 * named {@code of} that accepts a {@link Model} and returns an instance of
 * the KnowledgeIndex. The {@code of} method should invoke the
 * {@link Model#getKnowledge(Class, Function)} method to ensure that the
 * index is only computed once per model. Because they are cached and can be
 * used across threads, a KnowledgeIndex must be thread safe.
 *
 * <p>The following example demonstrates a standard KnowledgeIndex
 * implementation:
 *
 * <pre>{@code
 * public final class MyIndex implements KnowledgeIndex {
 *     public MyIndex(Model model) {
 *         // implement the code used to create the index.
 *     }
 *
 *     public static MyIndex of(Model model) {
 *         return model.getKnowledge(MyIndex.class, MyIndex::new);
 *     }
 *
 *     // Implement methods used to query the knowledge index.
 * }
 * }</pre>
 */
public interface KnowledgeIndex {}
