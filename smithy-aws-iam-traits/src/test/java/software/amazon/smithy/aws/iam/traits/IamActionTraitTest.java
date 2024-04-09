/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.iam.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DocumentationTrait;

public class IamActionTraitTest {
    private static final ShapeId ID = ShapeId.from("smithy.example#Foo");

    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("iam-action.smithy"))
                .assemble()
                .unwrap();

        Shape fooOperation = result.expectShape(ID);

        assertTrue(fooOperation.hasTrait(IamActionTrait.class));
        IamActionTrait trait = fooOperation.expectTrait(IamActionTrait.class);
        assertEquals(trait.getName().get(), "foo");
        assertEquals(trait.getDocumentation().get(), "docs");
        assertEquals(trait.getRelativeDocumentation().get(), "page.html#actions");
        assertThat(trait.getRequiredActions(), contains("iam:PassRole", "ec2:RunInstances"));
        assertThat(trait.getCreatesResources(), contains("kettle"));

        assertTrue(trait.getResources().isPresent());
        ActionResources actionResources = trait.getResources().get();
        assertTrue(actionResources.getRequired().containsKey("bar"));
        assertThat(actionResources.getRequired().get("bar").getConditionKeys(), contains("foo:asdf"));
        assertTrue(actionResources.getRequired().containsKey("bap"));
        assertThat(actionResources.getRequired().get("bap").getConditionKeys(), contains("foo:zxcv", "foo:hjkl"));
        assertTrue(actionResources.getOptional().containsKey("baz"));
        assertTrue(actionResources.getOptional().get("baz").getConditionKeys().isEmpty());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void prefersIamActionTraitName() {
        OperationShape op = OperationShape.builder().id(ID)
                .addTrait(IamActionTrait.builder().name("ThisOne").build())
                .addTrait(new ActionNameTrait("Unused"))
                .build();

        assertEquals("ThisOne", IamActionTrait.resolveActionName(op));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void usesDeprecatedActionNameTrait() {
        OperationShape op = OperationShape.builder().id(ID)
                .addTrait(new ActionNameTrait("ThisOne"))
                .build();

        assertEquals("ThisOne", IamActionTrait.resolveActionName(op));
    }

    @Test
    public void defaultsToOperationName() {
        OperationShape op = OperationShape.builder().id(ID).build();

        assertEquals("Foo", IamActionTrait.resolveActionName(op));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void prefersIamActionTraitDocumentation() {
        OperationShape op = OperationShape.builder().id(ID)
                .addTrait(IamActionTrait.builder().documentation("ThisOne").build())
                .addTrait(new ActionPermissionDescriptionTrait("Unused"))
                .addTrait(new DocumentationTrait("Unused"))
                .build();

        assertEquals("ThisOne", IamActionTrait.resolveActionDocumentation(op));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void usesDeprecatedActionPermissionDescriptionTrait() {
        OperationShape op = OperationShape.builder().id(ID)
                .addTrait(new ActionPermissionDescriptionTrait("ThisOne"))
                .addTrait(new DocumentationTrait("Unused"))
                .build();

        assertEquals("ThisOne", IamActionTrait.resolveActionDocumentation(op));
    }

    @Test
    public void usesDocumentationTrait() {
        OperationShape op = OperationShape.builder().id(ID)
                .addTrait(new DocumentationTrait("ThisOne"))
                .build();

        assertEquals("ThisOne", IamActionTrait.resolveActionDocumentation(op));
    }

    @Test
    public void defaultsToNoDocumentation() {
        OperationShape op = OperationShape.builder().id(ID).build();

        assertNull(IamActionTrait.resolveActionDocumentation(op));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void prefersIamActionTraitRequiredActions() {
        OperationShape op = OperationShape.builder().id(ID)
                .addTrait(IamActionTrait.builder().addRequiredAction("ThisOne").build())
                .addTrait(new ActionNameTrait("Unused"))
                .build();

        assertThat(IamActionTrait.resolveRequiredActions(op), contains("ThisOne"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void usesDeprecatedRequiredActionsTrait() {
        OperationShape op = OperationShape.builder().id(ID)
                .addTrait(RequiredActionsTrait.builder().addValue("ThisOne").build())
                .build();

        assertThat(IamActionTrait.resolveRequiredActions(op), contains("ThisOne"));
    }

    @Test
    public void defaultsToEmptyRequiredActions() {
        OperationShape op = OperationShape.builder().id(ID).build();

        assertThat(IamActionTrait.resolveRequiredActions(op), empty());
    }
}
