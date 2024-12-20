/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class IamResourceTraitTest {
    private static final ShapeId ID = ShapeId.from("smithy.example#SuperResource");

    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("iam-resource.smithy"))
                .assemble()
                .unwrap();

        Shape superResource = result.expectShape(ID);

        assertTrue(superResource.hasTrait(IamResourceTrait.class));
        assertEquals(superResource.expectTrait(IamResourceTrait.class).getName().get(), "super");
        assertEquals(superResource.expectTrait(IamResourceTrait.class).getRelativeDocumentation().get(),
                "API-Super.html");
        assertFalse(superResource.expectTrait(IamResourceTrait.class).isDisableConditionKeyInheritance());
    }

    @Test
    public void prefersIamResourceTraitName() {
        ResourceShape resource = ResourceShape.builder()
                .id(ID)
                .addTrait(IamResourceTrait.builder().name("ThisOne").build())
                .build();

        assertEquals("ThisOne", IamResourceTrait.resolveResourceName(resource));
    }

    @Test
    public void defaultsToResourceName() {
        ResourceShape resource = ResourceShape.builder().id(ID).build();

        assertEquals("SuperResource", IamResourceTrait.resolveResourceName(resource));
    }
}
