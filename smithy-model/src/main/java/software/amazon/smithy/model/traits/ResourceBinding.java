/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Common surface shared by the binding structures of the resource lifecycle
 * traits: every binding names an affected resource and may declare where to
 * find that resource's identifiers.
 *
 * <p>{@link ResourceLifecycleBinding} adds property location for the create,
 * put, read, and update traits; {@link ResourceDeletionBinding} omits
 * properties because a resource is deleted by its identifiers alone.
 */
@SmithyUnstableApi
public interface ResourceBinding extends ToNode {

    /**
     * @return Gets the shape ID of the affected resource.
     */
    ShapeId getResource();

    /**
     * @return Gets the explicit map of resource identifier name to a locator for its value.
     */
    Map<String, ResourceMemberBinding> getIdentifiers();

    /**
     * @return Gets the structural JMESPath from whose members identifiers are inferred by name.
     */
    Optional<String> getIdentifiersFrom();
}
