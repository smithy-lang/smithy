/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.codegen.core.directed;

import software.amazon.smithy.codegen.core.CodegenContext;
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
        if (!shape.isStringShape() || !shape.hasTrait(EnumTrait.class)) {
            throw new IllegalArgumentException("GenerateEnum requires a string shape with the enum trait");
        }

        return shape;
    }

    /**
     * Represents the type of enum to generate. Smithy IDL 2.0 introduces actual
     * enum shapes.
     */
    public enum EnumType {
        /** A string shape marked with the enum trait is being generated. */
        STRING,

        // TODO: idl-2.0
        // ENUM
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
        // TODO: update when idl-2.0 is released.
        return EnumType.STRING;
    }

    /**
     * Gets the {@link EnumTrait} of the shape.
     *
     * @return Returns the enum trait.
     */
    public EnumTrait getEnumTrait() {
        return shape().expectTrait(EnumTrait.class);
    }

    /*
    TODO: Uncomment after IDL-2.0 is shipped.
    public EnumShape expectEnumShape() {
        return shape().asEnumShape().orElseThrow(() -> new ExpectationNotMetException(
                "Expected an enum shape, but found " + shape(), shape()));
    }
    */
}
