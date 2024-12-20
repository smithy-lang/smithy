/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class MixinTrait extends AbstractTrait implements ToSmithyBuilder<MixinTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#mixin");

    private final Set<ShapeId> localTraits;

    private MixinTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        this.localTraits = SetUtils.orderedCopyOf(builder.localTraits);
    }

    /**
     * Gets the mixin local traits.
     *
     * @return returns the mixin local traits.
     */
    public Set<ShapeId> getLocalTraits() {
        return localTraits;
    }

    @Override
    protected Node createNode() {
        // smithy.api#mixin is always present, so no need to serialize it.
        if (localTraits.size() <= 1) {
            return Node.objectNode();
        }

        List<Node> nonImplicitValues = new ArrayList<>();
        for (ShapeId trait : localTraits) {
            if (!trait.equals(ID)) {
                nonImplicitValues.add(Node.from(trait.toString()));
            }
        }

        return Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withMember("localTraits", Node.fromNodes(nonImplicitValues))
                .build();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .localTraits(localTraits);
    }

    /**
     * Helper method used to filter out non-local traits from a map of traits.
     *
     * <p>If the map is empty or does not contain a mixin trait, then it is
     * returned as-is. If the map does contain the mixin trait, then a new map
     * is created that does not contain any of the localTraits specified on
     * the trait.
     *
     * @param traits Traits to filter based on the localTraits property of the mixin.
     * @return Returns the filtered traits.
     */
    public static Map<ShapeId, Trait> getNonLocalTraitsFromMap(Map<ShapeId, Trait> traits) {
        if (traits.isEmpty() || !traits.containsKey(ID)) {
            return traits;
        }

        Map<ShapeId, Trait> filtered = new HashMap<>(traits);

        // Technically the trait could be a dynamic trait in some wacky,
        // hand-made model that isn't sent through the assembler. That is
        // so beyond unlikely, that a hard cast here works fine.
        MixinTrait mixinTrait = (MixinTrait) traits.get(MixinTrait.ID);

        for (ShapeId toRemove : mixinTrait.getLocalTraits()) {
            filtered.remove(toRemove);
        }

        return filtered;
    }

    /**
     * @return Returns a new MixinTrait builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create a MixinTrait.
     */
    public static final class Builder extends AbstractTraitBuilder<MixinTrait, Builder> {
        private final Set<ShapeId> localTraits = new LinkedHashSet<>();

        @Override
        public MixinTrait build() {
            // The mixin trait is always implicitly local.
            localTraits.add(ID);
            return new MixinTrait(this);
        }

        public Builder localTraits(Collection<ShapeId> traits) {
            localTraits.clear();
            localTraits.addAll(traits);
            return this;
        }

        public Builder addLocalTrait(ShapeId trait) {
            localTraits.add(trait);
            return this;
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public MixinTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            value.expectObjectNode().getArrayMember("localTraits", ShapeId::fromNode, builder::localTraits);
            return builder.build();
        }
    }
}
