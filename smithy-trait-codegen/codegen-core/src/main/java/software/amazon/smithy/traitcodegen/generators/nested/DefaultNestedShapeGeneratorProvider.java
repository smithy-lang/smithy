/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators.nested;

import java.util.function.Consumer;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.GeneratorProvider;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public class DefaultNestedShapeGeneratorProvider extends ShapeVisitor.Default<Consumer<GenerateTraitDirective>>
        implements GeneratorProvider {
    @Override
    public Consumer<GenerateTraitDirective> getGenerator(Shape shape) {
        return shape.accept(this);
    }

    @Override
    protected Consumer<GenerateTraitDirective> getDefault(Shape shape) {
        // Most shapes do not generated additional classes when nested within a trait.
        return (Ignored) -> {
            // do nothing
        };
    }

    @Override
    public Consumer<GenerateTraitDirective> operationShape(OperationShape shape) {
        throw new UnsupportedOperationException("Cannot generate nested type for operation shapes");
    }

    @Override
    public Consumer<GenerateTraitDirective> resourceShape(ResourceShape shape) {
        throw new UnsupportedOperationException("Cannot generate nested type for resource shapes");
    }

    @Override
    public Consumer<GenerateTraitDirective> serviceShape(ServiceShape shape) {
        throw new UnsupportedOperationException("Cannot generate nested type for service shapes");
    }

    @Override
    public Consumer<GenerateTraitDirective> unionShape(UnionShape shape) {
        throw new UnsupportedOperationException("Cannot generate nested type for Union shapes");
    }

    @Override
    public Consumer<GenerateTraitDirective> structureShape(StructureShape shape) {
        return new StructureGenerator();
    }

    @Override
    public Consumer<GenerateTraitDirective> intEnumShape(IntEnumShape shape) {
        return new EnumGenerator.IntEnumGenerator();
    }

    @Override
    public Consumer<GenerateTraitDirective> enumShape(EnumShape shape) {
        return new EnumGenerator.StringEnumGenerator();
    }
}
