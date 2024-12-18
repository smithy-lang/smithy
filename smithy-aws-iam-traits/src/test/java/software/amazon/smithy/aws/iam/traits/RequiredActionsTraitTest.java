/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class RequiredActionsTraitTest {
    @Test
    @SuppressWarnings("deprecation")
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("required-actions.smithy"))
                .assemble()
                .unwrap();

        Shape myOperation = result.expectShape(ShapeId.from("smithy.example#MyOperation"));

        assertTrue(myOperation.hasTrait(RequiredActionsTrait.class));
        assertThat(myOperation.expectTrait(RequiredActionsTrait.class).getValues(),
                containsInAnyOrder(
                        "iam:PassRole",
                        "ec2:RunInstances"));
    }
}
