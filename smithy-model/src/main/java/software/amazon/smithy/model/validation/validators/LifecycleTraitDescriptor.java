/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractResourceLifecycleTrait;
import software.amazon.smithy.model.traits.CreatesResourcesTrait;
import software.amazon.smithy.model.traits.DeletesResourcesTrait;
import software.amazon.smithy.model.traits.PutsResourcesTrait;
import software.amazon.smithy.model.traits.ReadsResourcesTrait;
import software.amazon.smithy.model.traits.UpdatesResourcesTrait;
import software.amazon.smithy.utils.ListUtils;

/**
 * Describes one resource lifecycle trait and the side of the operation that its
 * identifiers and properties resolve against.
 *
 * <p>Shared by the validators that need to know where a lifecycle-trait binding
 * locates a resource's identifiers or properties, so the side of each trait is
 * defined in one place.
 */
final class LifecycleTraitDescriptor {

    /**
     * The side of an operation that a binding resolves against.
     */
    enum Side {
        INPUT, OUTPUT;

        /**
         * Gets the shape ID of this side of the given operation.
         *
         * @param operation The operation.
         * @return The operation's input or output shape ID.
         */
        ShapeId of(OperationShape operation) {
            return this == OUTPUT ? operation.getOutputShape() : operation.getInputShape();
        }
    }

    /**
     * All resource lifecycle traits, in create, put, read, update, delete order.
     */
    static final List<LifecycleTraitDescriptor> ALL = ListUtils.of(
            new LifecycleTraitDescriptor(CreatesResourcesTrait.class,
                    CreatesResourcesTrait.ID,
                    "createsResources",
                    Side.OUTPUT,
                    Side.INPUT,
                    ResourceShape::getCreate),
            new LifecycleTraitDescriptor(PutsResourcesTrait.class,
                    PutsResourcesTrait.ID,
                    "putsResources",
                    Side.INPUT,
                    Side.INPUT,
                    ResourceShape::getPut),
            new LifecycleTraitDescriptor(ReadsResourcesTrait.class,
                    ReadsResourcesTrait.ID,
                    "readsResources",
                    Side.INPUT,
                    Side.OUTPUT,
                    ResourceShape::getRead),
            new LifecycleTraitDescriptor(UpdatesResourcesTrait.class,
                    UpdatesResourcesTrait.ID,
                    "updatesResources",
                    Side.INPUT,
                    Side.INPUT,
                    ResourceShape::getUpdate),
            new LifecycleTraitDescriptor(DeletesResourcesTrait.class,
                    DeletesResourcesTrait.ID,
                    "deletesResources",
                    Side.INPUT,
                    null,
                    ResourceShape::getDelete));

    final Class<? extends AbstractResourceLifecycleTrait<?>> traitClass;
    final ShapeId traitId;
    final String name;
    final Side identifierSide;
    final Side propertySide; // null when the trait binds no properties (delete).
    private final Function<ResourceShape, Optional<ShapeId>> lifecycleOperation;

    private LifecycleTraitDescriptor(
            Class<? extends AbstractResourceLifecycleTrait<?>> traitClass,
            ShapeId traitId,
            String name,
            Side identifierSide,
            Side propertySide,
            Function<ResourceShape, Optional<ShapeId>> lifecycleOperation
    ) {
        this.traitClass = traitClass;
        this.traitId = traitId;
        this.name = name;
        this.identifierSide = identifierSide;
        this.propertySide = propertySide;
        this.lifecycleOperation = lifecycleOperation;
    }

    /**
     * Checks whether an operation is the resource's standard lifecycle operation of the same
     * kind as this trait, for example its {@code read} operation for {@code @readsResources}.
     *
     * <p>A binding on such an operation is redundant: the standard lifecycle binding already
     * relates the operation to the resource.
     *
     * @param resource The resource the binding targets.
     * @param operationId The operation carrying the trait.
     * @return Returns true if the operation fills the matching lifecycle slot of the resource.
     */
    boolean isLifecycleBound(ResourceShape resource, ShapeId operationId) {
        return lifecycleOperation.apply(resource).map(operationId::equals).orElse(false);
    }
}
