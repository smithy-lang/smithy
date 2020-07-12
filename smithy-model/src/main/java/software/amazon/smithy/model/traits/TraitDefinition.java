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
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Trait definition trait.
 */
public final class TraitDefinition extends AbstractTrait implements ToSmithyBuilder<TraitDefinition> {
    public static final ShapeId ID = ShapeId.from("smithy.api#trait");

    /** The structural exclusion semantics of the trait. */
    public enum StructurallyExclusive {
        /** The trait can only be applied to a single member of a structure. */
        MEMBER,

        /** Only a single structure member can target a shape marked with the trait. */
        TARGET;

        @Override
        public String toString() {
            return super.toString().toLowerCase(Locale.ENGLISH);
        }
    }

    public static final String SELECTOR_KEY = "selector";
    public static final String STRUCTURALLY_EXCLUSIVE_KEY = "structurallyExclusive";
    public static final String CONFLICTS_KEY = "conflicts";

    private final Selector selector;
    private final List<ShapeId> conflicts;
    private final StructurallyExclusive structurallyExclusive;

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
     * Gets the {@code structurallyExclusive} property of the trait.
     *
     * @return Returns the {@code structurallyExclusive} property of the trait.
     */
    public Optional<StructurallyExclusive> getStructurallyExclusive() {
        return Optional.ofNullable(structurallyExclusive);
    }

    /**
     * @return Returns true if the trait is {@code structurallyExclusive} by member.
     */
    public boolean isStructurallyExclusiveByMember() {
        return structurallyExclusive == StructurallyExclusive.MEMBER;
    }

    /**
     * @return Returns true if the trait is {@code structurallyExclusive} by target.
     */
    public boolean isStructurallyExclusiveByTarget() {
        return structurallyExclusive == StructurallyExclusive.TARGET;
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

        builder.withOptionalMember(
                STRUCTURALLY_EXCLUSIVE_KEY,
                getStructurallyExclusive().map(StructurallyExclusive::toString).map(Node::from));

        return builder.build();
    }

    /**
     * Builder to create a TraitDefinition.
     */
    public static final class Builder extends AbstractTraitBuilder<TraitDefinition, Builder> {
        private Selector selector = Selector.IDENTITY;
        private final List<ShapeId> conflicts = new ArrayList<>();
        private StructurallyExclusive structurallyExclusive;

        private Builder() {}

        public Builder selector(Selector selector) {
            this.selector = selector;
            return this;
        }

        public Builder addConflict(String trait) {
            Objects.requireNonNull(trait);
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

        public Builder structurallyExclusive(StructurallyExclusive structurallyExclusive) {
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
            ObjectNode members = value.isNullNode()
                    ? Node.objectNode()
                    : value.expectObjectNode();

            Builder builder = builder().sourceLocation(value);

            members.getMember(TraitDefinition.SELECTOR_KEY)
                    .map(Selector::fromNode)
                    .ifPresent(builder::selector);

            members.getStringMember(TraitDefinition.STRUCTURALLY_EXCLUSIVE_KEY)
                    .map(node -> node.expectOneOf(
                            StructurallyExclusive.MEMBER.toString(),
                            StructurallyExclusive.TARGET.toString()))
                    .map(string -> string.toUpperCase(Locale.ENGLISH))
                    .map(StructurallyExclusive::valueOf)
                    .ifPresent(builder::structurallyExclusive);

            members.getMember(TraitDefinition.CONFLICTS_KEY)
                    .ifPresent(values -> loadArrayOfString(TraitDefinition.CONFLICTS_KEY, values)
                            .forEach(builder::addConflict));

            return builder.build();
        }
    }
}
