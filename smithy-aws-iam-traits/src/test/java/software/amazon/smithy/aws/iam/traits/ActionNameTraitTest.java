/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class ActionNameTraitTest {
    @Test
    @SuppressWarnings("deprecation")
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("actionname-override.smithy"))
                .assemble()
                .unwrap();

        Shape shape = result.expectShape(ShapeId.from("smithy.example#Echo"));
        ActionNameTrait trait = shape.expectTrait(ActionNameTrait.class);
        assertThat(trait.getValue(), equalTo("overridingActionName"));
    }
}
