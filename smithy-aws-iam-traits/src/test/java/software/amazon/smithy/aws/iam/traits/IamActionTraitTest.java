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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class IamActionTraitTest {
    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("iam-action.smithy"))
                .assemble()
                .unwrap();

        Shape fooOperation = result.expectShape(ShapeId.from("smithy.example#Foo"));

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
}
