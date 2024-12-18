/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.EnumTrait;

/**
 * Directive used to generate an enum shape or enum string shape.
 *
 * @param <C> CodegenContext type.
 * @param <S> Codegen settings type.
 * @see DirectedCodegen#generateEnumShape
 */
public final class GenerateEnumDirective<C extends CodegenContext<S, ?, ?>, S> extends ShapeDirective<Shape, C, S> {

    GenerateEnumDirective(C context, ServiceShape service, Shape shape) {
        super(context, service, validateShape(shape));
    }

    private static Shape validateShape(Shape shape) {
        if (shape.isEnumShape() || (shape.isStringShape() && shape.hasTrait(EnumTrait.class))) {
            return shape;
        }
        throw new IllegalArgumentException("GenerateEnum requires an enum shape or a string shape with the enum trait");
    }

    /**
     * Represents the type of enum to generate. Smithy IDL 2.0 introduces actual
     * enum shapes.
     */
    public enum EnumType {
        /** A string shape marked with the enum trait is being generated. */
        STRING,
        ENUM
    }

    /**
     * Gets the type of enum being generated, whether it's a string shape marked with
     * the enum trait or, in Smithy IDL 2.0, an actual enum shape.
     *
     * <p>Note that Smithy IDL 2.0 generators can perform a pre-processing transform
     * to convert eligible string shape enums to proper enums, removing the need to
     * check this property.
     *
     * @return Gets the type of enum being generated.
     * @see #getEnumTrait()
     */
    public EnumType getEnumType() {
        return shape().isEnumShape() ? EnumType.ENUM : EnumType.STRING;
    }

    /**
     * Gets the {@link EnumTrait} of the shape.
     *
     * @return Returns the enum trait.
     */
    public EnumTrait getEnumTrait() {
        return shape().expectTrait(EnumTrait.class);
    }

    public EnumShape expectEnumShape() {
        return shape().asEnumShape()
                .orElseThrow(() -> new ExpectationNotMetException(
                        "Expected an enum shape, but found " + shape(),
                        shape()));
    }
}
