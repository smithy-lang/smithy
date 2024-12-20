/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.ValidationEvent;

abstract class LoadOperation implements FromSourceLocation {

    interface Visitor {
        void putMetadata(PutMetadata operation);

        void applyTrait(ApplyTrait operation);

        void defineShape(DefineShape shape);

        void forwardReference(ForwardReference operation);

        void event(Event operation);

        void modelVersion(ModelVersion operation);
    }

    final Version version;

    LoadOperation(Version version) {
        this.version = version;
    }

    abstract void accept(Visitor visitor);

    static final class PutMetadata extends LoadOperation {
        final String key;
        final Node value;

        PutMetadata(Version version, String key, Node value) {
            super(version);
            this.key = key;
            this.value = value;
        }

        @Override
        public SourceLocation getSourceLocation() {
            return value.getSourceLocation();
        }

        @Override
        void accept(Visitor visitor) {
            visitor.putMetadata(this);
        }
    }

    static final class ApplyTrait extends LoadOperation {
        final String namespace;
        final ShapeId target;
        final ShapeId trait;
        final Node value;
        final SourceLocation location;

        ApplyTrait(
                Version version,
                SourceLocation location,
                String namespace,
                ShapeId target,
                ShapeId trait,
                Node value
        ) {
            super(version);
            this.namespace = namespace;
            this.target = target;
            this.trait = trait;
            this.value = value;
            this.location = location;
        }

        static ApplyTrait from(ShapeId target, Trait trait) {
            return new ApplyTrait(Version.UNKNOWN,
                    trait.getSourceLocation(),
                    target.getNamespace(),
                    target,
                    trait.toShapeId(),
                    trait.toNode());
        }

        @Override
        public SourceLocation getSourceLocation() {
            return location;
        }

        @Override
        void accept(Visitor visitor) {
            visitor.applyTrait(this);
        }
    }

    static final class DefineShape extends LoadOperation implements ToShapeId {

        private final AbstractShapeBuilder<?, ?> builder;
        private Set<ShapeId> dependencies;
        private Map<String, MemberShape.Builder> members;
        private List<ShapeModifier> modifiers;

        DefineShape(Version version, AbstractShapeBuilder<?, ?> builder) {
            super(version);
            if (builder.getShapeType() == ShapeType.MEMBER) {
                throw new UnsupportedOperationException("Members must be added to top-level DefineShape instances");
            }
            this.builder = builder;
        }

        @Override
        void accept(Visitor visitor) {
            visitor.defineShape(this);
        }

        @Override
        public ShapeId toShapeId() {
            return builder.getId();
        }

        @Override
        public SourceLocation getSourceLocation() {
            return builder.getSourceLocation();
        }

        Set<ShapeId> dependencies() {
            return dependencies == null ? Collections.emptySet() : dependencies;
        }

        void addDependency(ShapeId id) {
            if (dependencies == null) {
                dependencies = new LinkedHashSet<>();
            }
            dependencies.add(id);
        }

        ShapeType getShapeType() {
            return builder.getShapeType();
        }

        AbstractShapeBuilder<?, ?> builder() {
            return builder;
        }

        void addMember(MemberShape.Builder member) {
            if (members == null) {
                members = new LinkedHashMap<>();
            }
            members.put(member.getId().getMember().get(), member);
        }

        boolean hasMember(String memberName) {
            return members != null && members.containsKey(memberName);
        }

        Map<String, MemberShape.Builder> memberBuilders() {
            return members == null ? Collections.emptyMap() : members;
        }

        List<ShapeModifier> modifiers() {
            return modifiers == null ? Collections.emptyList() : modifiers;
        }

        void addModifier(ShapeModifier modifier) {
            if (modifiers == null) {
                modifiers = new ArrayList<>();
            }
            modifiers.add(modifier);
        }
    }

    static final class ForwardReference extends LoadOperation {
        final String namespace;
        final String name;
        private final BiFunction<ShapeId, ShapeType, ValidationEvent> receiver;

        ForwardReference(String namespace, String name, BiFunction<ShapeId, ShapeType, ValidationEvent> receiver) {
            super(Version.UNKNOWN);
            this.namespace = namespace;
            this.name = name;
            this.receiver = receiver;
        }

        @Override
        void accept(Visitor visitor) {
            visitor.forwardReference(this);
        }

        ValidationEvent resolve(ShapeId id, ShapeType type) {
            return receiver.apply(id, type);
        }
    }

    static final class Event extends LoadOperation {
        final ValidationEvent event;

        Event(ValidationEvent event) {
            super(Version.UNKNOWN);
            this.event = event;
        }

        @Override
        void accept(Visitor visitor) {
            visitor.event(this);
        }
    }

    static final class ModelVersion extends LoadOperation {
        final SourceLocation sourceLocation;

        ModelVersion(Version version, SourceLocation sourceLocation) {
            super(version);
            this.sourceLocation = sourceLocation;
        }

        @Override
        void accept(Visitor visitor) {
            visitor.modelVersion(this);
        }

        @Override
        public SourceLocation getSourceLocation() {
            return sourceLocation;
        }
    }
}
