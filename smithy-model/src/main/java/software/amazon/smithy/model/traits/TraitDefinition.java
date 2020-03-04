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

import static software.amazon.smithy.model.node.Node.loadArrayOfString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Trait definition trait.
 */
public final class TraitDefinition extends AbstractTrait implements ToSmithyBuilder<TraitDefinition> {
    public static final ShapeId ID = ShapeId.from("smithy.api#trait");

    public static final String SELECTOR_KEY = "selector";
    public static final String STRUCTURALLY_EXCLUSIVE_KEY = "structurallyExclusive";
    public static final String CONFLICTS_KEY = "conflicts";

    private static final Set<String> TRAIT_DEFINITION_PROPERTY_NAMES = SetUtils.of(
            SELECTOR_KEY, STRUCTURALLY_EXCLUSIVE_KEY, CONFLICTS_KEY);

    private final Selector selector;
    private final List<ShapeId> conflicts;
    private final boolean structurallyExclusive;

    public TraitDefinition(TraitDefinition.Builder builder) {
        super(ID, builder.sourceLocation);
        selector = builder.selector;
        conflicts = ListUtils.copyOf(builder.conflicts);
        structurallyExclusive = builder.structurallyExclusive;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder().selector(selector).structurallyExclusive(structurallyExclusive);
        conflicts.forEach(builder::addConflict);
        return builder;
    }

    /**
     * Gets the valid places in a model that this trait can be applied.
     *
     * @return Returns the trait selector.
     */
    public Selector getSelector() {
        return selector;
    }

    /**
     * Gets the trait names that conflict with this trait.
     *
     * @return Returns the conflicting trait names.
     */
    public List<ShapeId> getConflicts() {
        return conflicts;
    }

    /**
     * @return Returns true if the trait is structurally exclusive.
     */
    public boolean isStructurallyExclusive() {
        return structurallyExclusive;
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder().sourceLocation(getSourceLocation());

        if (selector != Selector.IDENTITY) {
            builder.withMember(SELECTOR_KEY, selector.toString());
        }

        if (!conflicts.isEmpty()) {
            builder.withMember(CONFLICTS_KEY, conflicts.stream()
                    .map(ShapeId::toString)
                    .map(Node::from)
                    .collect(ArrayNode.collect()));
        }

        if (isStructurallyExclusive()) {
            builder.withMember(STRUCTURALLY_EXCLUSIVE_KEY, Node.from(true));
        }

        return builder.build();
    }

    /**
     * Builder to create a TraitDefinition.
     */
    public static final class Builder extends AbstractTraitBuilder<TraitDefinition, Builder> {
        private Selector selector = Selector.IDENTITY;
        private final List<ShapeId> conflicts = new ArrayList<>();
        private boolean structurallyExclusive;

        private Builder() {}

        public Builder selector(Selector selector) {
            this.selector = selector;
            return this;
        }

        public Builder addConflict(String trait) {
            Objects.requireNonNull(trait);

            // Use absolute trait names.
            if (!trait.contains("#")) {
                trait = Prelude.NAMESPACE + "#" + trait;
            }

            return addConflict(ShapeId.from(trait));
        }

        public Builder addConflict(ShapeId id) {
            Objects.requireNonNull(id);
            conflicts.add(id);
            return this;
        }

        public Builder removeConflict(ToShapeId id) {
            conflicts.remove(id.toShapeId());
            return this;
        }

        public Builder structurallyExclusive(boolean structurallyExclusive) {
            this.structurallyExclusive = structurallyExclusive;
            return this;
        }

        @Override
        public TraitDefinition build() {
            return new TraitDefinition(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public TraitDefinition createTrait(ShapeId target, Node value) {
            // The handling of a trait definition is special-cased, so coercion
            // from a null value to an object is required.
            ObjectNode members = Trait.coerceTraitValue(value, ShapeType.STRUCTURE).expectObjectNode();
            members.warnIfAdditionalProperties(TRAIT_DEFINITION_PROPERTY_NAMES);
            Builder builder = builder().sourceLocation(value);

            members.getMember(TraitDefinition.SELECTOR_KEY)
                    .map(Selector::fromNode)
                    .ifPresent(builder::selector);

            members.getBooleanMember(TraitDefinition.STRUCTURALLY_EXCLUSIVE_KEY)
                    .map(BooleanNode::getValue)
                    .ifPresent(builder::structurallyExclusive);

            members.getMember(TraitDefinition.CONFLICTS_KEY)
                    .ifPresent(values -> loadArrayOfString(TraitDefinition.CONFLICTS_KEY, values)
                            .forEach(builder::addConflict));

            return builder.build();
        }
    }
}
