/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class CfnResourceTraitTest {
    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cfn-resources.smithy"))
                .assemble()
                .unwrap();

        Shape fooResource = result.expectShape(ShapeId.from("smithy.example#FooResource"));
        assertTrue(fooResource.hasTrait(CfnResourceTrait.class));
        CfnResourceTrait fooTrait = fooResource.expectTrait(CfnResourceTrait.class);
        assertFalse(fooTrait.getName().isPresent());
        assertTrue(fooTrait.getAdditionalSchemas().isEmpty());

        Shape barResource = result.expectShape(ShapeId.from("smithy.example#BarResource"));
        assertTrue(barResource.hasTrait(CfnResourceTrait.class));
        CfnResourceTrait barTrait = barResource.expectTrait(CfnResourceTrait.class);
        assertThat(barTrait.getName().get(), equalTo("CustomResource"));
        assertFalse(barTrait.getAdditionalSchemas().isEmpty());
        assertThat(barTrait.getAdditionalSchemas(), contains(ShapeId.from("smithy.example#ExtraBarRequest")));
    }

    @Test
    public void handlesNameProperty() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("test-service.smithy"))
                .assemble()
                .unwrap();

        assertFalse(
                result.expectShape(ShapeId.from("smithy.example#FooResource"))
                        .expectTrait(CfnResourceTrait.class)
                        .getName()
                        .isPresent());
        assertThat(
                result.expectShape(ShapeId.from("smithy.example#BarResource"))
                        .expectTrait(CfnResourceTrait.class)
                        .getName()
                        .get(),
                equalTo("Bar"));
        assertThat(
                result.expectShape(ShapeId.from("smithy.example#BazResource"))
                        .expectTrait(CfnResourceTrait.class)
                        .getName()
                        .get(),
                equalTo("Basil"));
    }
}
