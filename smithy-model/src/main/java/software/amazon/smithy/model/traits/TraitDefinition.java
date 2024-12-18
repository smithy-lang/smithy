/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodePointer;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Trait definition trait.
 */
public final class TraitDefinition extends AbstractTrait implements ToSmithyBuilder<TraitDefinition> {
    public static final ShapeId ID = ShapeId.from("smithy.api#trait");

    /** The structural exclusion semantics of the trait. */
    public enum StructurallyExclusive implements ToNode {
        /** The trait can only be applied to a single member of a structure. */
        MEMBER,

        /** Only a single structure member can target a shape marked with the trait. */
        TARGET;

        @Override
        public String toString() {
            return super.toString().toLowerCase(Locale.ENGLISH);
        }

        @Override
        public Node toNode() {
            return Node.from(toString());
        }

        public static StructurallyExclusive fromNode(Node node) {
            String value = node.expectStringNode()
                    .expectOneOf(
                            StructurallyExclusive.MEMBER.toString(),
                            StructurallyExclusive.TARGET.toString());
            return StructurallyExclusive.valueOf(value.toUpperCase(Locale.ENGLISH));
        }
    }

    /**
     * Represents an individual trait diff rule to define backward compatibility rules.
     */
    public static final class BreakingChangeRule implements ToNode {
        private final NodePointer path;
        private final Severity severity;
        private final ChangeType change;
        private final String message;

        public BreakingChangeRule(NodePointer path, Severity severity, ChangeType change, String message) {
            this.path = path;
            this.severity = severity;
            this.change = change;
            this.message = message;
        }

        public Optional<NodePointer> getPath() {
            return Optional.ofNullable(path);
        }

        public NodePointer getDefaultedPath() {
            return path == null ? NodePointer.empty() : path;
        }

        public Optional<Severity> getSeverity() {
            return Optional.ofNullable(severity);
        }

        public Severity getDefaultedSeverity() {
            return severity == null ? Severity.ERROR : severity;
        }

        public ChangeType getChange() {
            return change;
        }

        public Optional<String> getMessage() {
            return Optional.ofNullable(message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, severity, change, message);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof BreakingChangeRule) {
                BreakingChangeRule other = (BreakingChangeRule) obj;
                return Objects.equals(path, other.path)
                        && Objects.equals(severity, other.severity)
                        && Objects.equals(message, other.message)
                        && change == other.change;
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return super.toString();
        }

        @Override
        public Node toNode() {
            return Node.objectNodeBuilder()
                    .withOptionalMember("path", getPath().map(NodePointer::toString).map(Node::from))
                    .withOptionalMember("severity", getSeverity().map(Severity::toNode))
                    .withMember("change", change.toNode())
                    .withOptionalMember("message", getMessage().map(Node::from))
                    .build();
        }

        /**
         * Creates a TraitDiffRule from a Node.
         *
         * @param node Node to deserialize.
         * @return Returns the created TraitDiffRule.
         * @throws ExpectationNotMetException if the node is invalid.
         */
        public static BreakingChangeRule fromNode(Node node) {
            ObjectNode obj = node.expectObjectNode();
            NodePointer path = obj.getStringMember("path").map(NodePointer::fromNode).orElse(null);
            Severity severity = obj.getStringMember("severity").map(Severity::fromNode).orElse(null);
            ChangeType change = ChangeType.fromNode(obj.expectStringMember("change"));
            String message = obj.getStringMemberOrDefault("message", null);
            if (severity == Severity.SUPPRESSED) {
                throw new ExpectationNotMetException("Invalid severity", obj.expectMember("severity"));
            }
            return new BreakingChangeRule(path, severity, change, message);
        }
    }

    public enum ChangeType implements ToNode {

        /** Emit when a trait or value is added that previously did not exist. */
        ADD,

        /** Emit when a trait or value is removed. */
        REMOVE,

        /** Emit when a trait is added or removed. */
        PRESENCE,

        /** Emit when a trait already existed, continues to exist, but it is modified. */
        UPDATE,

        /** Emit when any change occurs. */
        ANY;

        /**
         * Creates a ChangeType value from a node.
         *
         * @param node Node to parse.
         * @return Returns the parsed ChangeType.
         * @throws ExpectationNotMetException if the node is invalid.
         */
        public static ChangeType fromNode(Node node) {
            try {
                return ChangeType.valueOf(node.expectStringNode().getValue().toUpperCase(Locale.ENGLISH));
            } catch (RuntimeException e) {
                String message = "Expected a string containing a valid trait diff type: " + e.getMessage();
                throw new ExpectationNotMetException(message, node);
            }
        }

        @Override
        public Node toNode() {
            return Node.from(toString());
        }

        @Override
        public String toString() {
            return super.toString().toLowerCase(Locale.ENGLISH);
        }
    }

    private final Selector selector;
    private final List<ShapeId> conflicts;
    private final StructurallyExclusive structurallyExclusive;
    private final List<BreakingChangeRule> breakingChanges;

    public TraitDefinition(TraitDefinition.Builder builder) {
        super(ID, builder.sourceLocation);
        selector = builder.selector;
        conflicts = builder.conflicts.copy();
        structurallyExclusive = builder.structurallyExclusive;
        breakingChanges = builder.breakingChanges.copy();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder()
                .sourceLocation(getSourceLocation())
                .selector(selector)
                .structurallyExclusive(structurallyExclusive)
                .breakingChanges(breakingChanges);
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

    /**
     * @return Returns the breaking change rules of the trait.
     */
    public List<BreakingChangeRule> getBreakingChanges() {
        return breakingChanges;
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder().sourceLocation(getSourceLocation());

        if (selector != Selector.IDENTITY) {
            builder.withMember("selector", selector.toString());
        }

        if (!conflicts.isEmpty()) {
            builder.withMember("conflicts",
                    conflicts.stream()
                            .map(ShapeId::toString)
                            .map(Node::from)
                            .collect(ArrayNode.collect()));
        }

        builder.withOptionalMember("structurallyExclusive",
                getStructurallyExclusive().map(StructurallyExclusive::toNode));

        if (!breakingChanges.isEmpty()) {
            List<Node> result = new ArrayList<>(breakingChanges.size());
            breakingChanges.forEach(d -> result.add(d.toNode()));
            builder.withMember("breakingChanges", Node.fromNodes(result));
        }

        return builder.build();
    }

    // Avoid potential equality issues related to inconsequential toNode differences.
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TraitDefinition)) {
            return false;
        } else if (other == this) {
            return true;
        } else {
            TraitDefinition od = (TraitDefinition) other;
            return selector.equals(od.selector)
                    && conflicts.equals(od.conflicts)
                    && Objects.equals(structurallyExclusive, od.structurallyExclusive)
                    && breakingChanges.equals(od.breakingChanges);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(toShapeId(), selector, conflicts, structurallyExclusive, breakingChanges);
    }

    /**
     * Builder to create a TraitDefinition.
     */
    public static final class Builder extends AbstractTraitBuilder<TraitDefinition, Builder> {
        private Selector selector = Selector.IDENTITY;
        private final BuilderRef<List<ShapeId>> conflicts = BuilderRef.forList();
        private StructurallyExclusive structurallyExclusive;
        private final BuilderRef<List<BreakingChangeRule>> breakingChanges = BuilderRef.forList();

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
            conflicts.get().add(id);
            return this;
        }

        public Builder removeConflict(ToShapeId id) {
            conflicts.get().remove(id.toShapeId());
            return this;
        }

        public Builder structurallyExclusive(StructurallyExclusive structurallyExclusive) {
            this.structurallyExclusive = structurallyExclusive;
            return this;
        }

        public Builder breakingChanges(List<BreakingChangeRule> diff) {
            clearBreakingChanges();
            diff.forEach(this::addBreakingChange);
            return this;
        }

        public Builder clearBreakingChanges() {
            this.breakingChanges.clear();
            return this;
        }

        public Builder addBreakingChange(BreakingChangeRule rule) {
            this.breakingChanges.get().add(Objects.requireNonNull(rule));
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
            ObjectNode members = value.isNullNode() ? Node.objectNode() : value.expectObjectNode();
            Builder builder = builder().sourceLocation(value);
            members.expectObjectNode()
                    .getMember("selector", Selector::fromNode, builder::selector)
                    .getMember("structurallyExclusive", StructurallyExclusive::fromNode, builder::structurallyExclusive)
                    .getArrayMember("conflicts", nodes -> {
                        for (Node element : nodes) {
                            builder.addConflict(element.expectStringNode().getValue());
                        }
                    })
                    .getArrayMember("breakingChanges", BreakingChangeRule::fromNode, builder::breakingChanges);
            TraitDefinition result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }
}
