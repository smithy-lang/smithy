/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import java.util.function.Consumer;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
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
            directive.shape().accept(new TraitShapeGenerator(directive));
        } else {
            directive.shape().accept(new NestedShapeGenerator(directive));
        }
    }

    private static final class TraitShapeGenerator extends ShapeVisitor.DataShapeVisitor<Void> {
        private final GenerateTraitDirective directive;

        private TraitShapeGenerator(GenerateTraitDirective directive) {
            this.directive = directive;
        }

        @Override
        public Void booleanShape(BooleanShape shape) {
            throw new UnsupportedOperationException("Boolean shapes not supported for trait code generation. "
                    + "Consider using an Annotation (empty structure) trait instead");
        }

        @Override
        public Void intEnumShape(IntEnumShape shape) {
            new IntEnumTraitGenerator().accept(directive);
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            // If the shape type resolves is a simple java string we can have the
            // resulting trait inherit from the StringTrait base class, simplifying the generated code.
            if (TraitCodegenUtils.isJavaString(directive.symbolProvider().toSymbol(shape))) {
                new StringTraitGenerator().accept(directive);
            } else {
                new ValueTraitGenerator().accept(directive);
            }
            return null;
        }

        @Override
        public Void enumShape(EnumShape shape) {
            new EnumTraitGenerator().accept(directive);
            return null;
        }

        @Override
        public Void listShape(ListShape shape) {
            // If the shape is a list shape with only string members we want it to inherit from
            // the StringListShape base class rather than use the default collection trait generator
            if (!shape.hasTrait(UniqueItemsTrait.class)
                    && TraitCodegenUtils.isJavaString(directive.symbolProvider().toSymbol(shape.getMember()))
            ) {
                new StringListTraitGenerator().accept(directive);
            } else {
                new CollectionTraitGenerator().accept(directive);
            }
            return null;
        }

        @Override
        public Void byteShape(ByteShape shape) {
            new ValueTraitGenerator().accept(directive);
            return null;
        }

        @Override
        public Void shortShape(ShortShape shape) {
            new ValueTraitGenerator().accept(directive);
            return null;
        }

        @Override
        public Void integerShape(IntegerShape shape) {
            new ValueTraitGenerator().accept(directive);
            return null;
        }

        @Override
        public Void longShape(LongShape shape) {
            new ValueTraitGenerator().accept(directive);
            return null;
        }

        @Override
        public Void floatShape(FloatShape shape) {
            new ValueTraitGenerator().accept(directive);
            return null;
        }

        @Override
        public Void documentShape(DocumentShape shape) {
            new DocumentTraitGenerator().accept(directive);
            return null;
        }

        @Override
        public Void doubleShape(DoubleShape shape) {
            new ValueTraitGenerator().accept(directive);
            return null;
        }

        @Override
        public Void bigIntegerShape(BigIntegerShape shape) {
            new ValueTraitGenerator().accept(directive);
            return null;
        }

        @Override
        public Void bigDecimalShape(BigDecimalShape shape) {
            new ValueTraitGenerator().accept(directive);
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            new CollectionTraitGenerator().accept(directive);
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            new CollectionTraitGenerator().accept(directive);
            return null;
        }

        @Override
        public Void timestampShape(TimestampShape shape) {
            new ValueTraitGenerator().accept(directive);
            return null;
        }

        @Override
        public Void unionShape(UnionShape shape) {
            throw new UnsupportedOperationException("Trait code generation does not support union traits"
                    + " at this time. Failed to generate Trait class for: " + shape);
        }

        @Override
        public Void blobShape(BlobShape shape) {
            throw new UnsupportedOperationException("Trait code generation does not support blob traits"
                    + " at this time. Failed to generate Trait class for: " + shape);
        }

        @Override
        public Void memberShape(MemberShape shape) {
            throw new IllegalArgumentException("Member shapes cannot be generated as traits. Attempted to generate "
                    + " trait definition for :" + shape);
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
