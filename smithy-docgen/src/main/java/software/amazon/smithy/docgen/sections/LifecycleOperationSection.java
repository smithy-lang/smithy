/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.sections;

import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.generators.ResourceGenerator;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Contains the documentation for individual resource lifecycle operations.
 *
 * @param context The context used to generate documentation.
 * @param resource The resource the operation is bound to.
 * @param operation The lifecycle operation being listed.
 * @param lifecycleType The type of lifecycle binding.
 *
 * @see LifecycleSection For the section containing all the resource lifecycle
 * operations.
 * @see BoundOperationSection For individual operations bound to the resource's
 * {@code operations} or {@code collectionOperations} properties.
 * @see BoundOperationsSection For all operations bound to the resource's
 * {@code operations} or {@code collectionOperations} properties.
 * @see ResourceGenerator for information
 * about other sections present on the documentation pages for resrouces.
 */
@SmithyUnstableApi
public record LifecycleOperationSection(
        DocGenerationContext context,
        ResourceShape resource,
        OperationShape operation,
        LifecycleType lifecycleType) implements CodeSection {

    // smithy-model doesn't have a pared-down enum for these lifecycle types, instead
    // using the broader RelationshipType. That's fine for smithy-model, which needs
    // those other relationship types, but we don't want to confuse people here with
    // tons of irrelevant enum values. So instead this pared-down enum is provided.
    /**
     * The type of lifecycle binding an operation can use.
     *
     * @see <a href="https://smithy.io/2.0/spec/service-types.html#resource-lifecycle-operations">
     * Smithy's resource lifecycle docs</a>
     */
    public enum LifecycleType {
        /**
         * Indicates the operation is bound as the resource's
         * <a href="https://smithy.io/2.0/spec/service-types.html#put-lifecycle">
         * put operation</a>.
         */
        PUT,

        /**
         * Indicates the operation is bound as the resource's
         * <a href="https://smithy.io/2.0/spec/service-types.html#create-lifecycle">
         * create operation</a>.
         */
        CREATE,

        /**
         * Indicates the operation is bound as the resource's
         * <a href="https://smithy.io/2.0/spec/service-types.html#read-lifecycle">
         * read operation</a>.
         */
        READ,

        /**
         * Indicates the operation is bound as the resource's
         * <a href="https://smithy.io/2.0/spec/service-types.html#update-lifecycle">
         * update operation</a>.
         */
        UPDATE,

        /**
         * Indicates the operation is bound as the resource's
         * <a href="https://smithy.io/2.0/spec/service-types.html#delete-lifecycle">
         * delete operation</a>.
         */
        DELETE,

        /**
         * Indicates the operation is bound as the resource's
         * <a href="https://smithy.io/2.0/spec/service-types.html#list-lifecycle">
         * list operation</a>.
         */
        LIST
    }
}
