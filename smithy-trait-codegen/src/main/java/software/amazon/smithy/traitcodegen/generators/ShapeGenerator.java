/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.generators;

import java.util.function.Consumer;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Base class used for the generation of traits and nested shapes from a {@link GenerateTraitDirective}.
 *
 * <p>This class will determine if a shape is a trait (i.e. has the {@link TraitDefinition} trait) or if the
 * shape provided should be treated as a nested shape (i.e. defines a simple pojo).
 */
@SmithyInternalApi
public final class ShapeGenerator implements Consumer<GenerateTraitDirective> {
    @Override
    public void accept(GenerateTraitDirective directive) {
        if (directive.shape().hasTrait(TraitDefinition.class)) {
            new TraitGenerator().accept(directive);
        } else {
            directive.shape().accept(new NestedShapeGenerator(directive));
        }
    }

    private static final class NestedShapeGenerator extends ShapeVisitor.Default<Void> {

        private final GenerateTraitDirective directive;

        private NestedShapeGenerator(GenerateTraitDirective directive) {
            this.directive = directive;
        }

        @Override
        protected Void getDefault(Shape shape) {
            // Most nested shapes do not generate new classes.
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            new StructureShapeGenerator().accept(directive);
            return null;
        }

        @Override
        public Void enumShape(EnumShape shape) {
            new EnumShapeGenerator.StringEnumShapeGenerator().accept(directive);
            return null;
        }

        @Override
        public Void intEnumShape(IntEnumShape shape) {
            new EnumShapeGenerator.IntEnumShapeGenerator().accept(directive);
            return null;
        }

        @Override
        public Void unionShape(UnionShape shape) {
            throw new UnsupportedOperationException("Generation of nested types for Union shapes "
                    + " is not supported at this time.");
        }

        @Override
        public Void memberShape(MemberShape shape) {
            throw new IllegalArgumentException("NestedShapeGenerator should not visit member shapes. "
                    + " Attempted to visit " + shape);
        }
    }
}
