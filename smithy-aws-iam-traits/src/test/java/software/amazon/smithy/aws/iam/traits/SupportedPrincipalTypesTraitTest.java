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

public class SupportedPrincipalTypesTraitTest {
    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("supported-principal-types.smithy"))
                .assemble()
                .unwrap();

        Shape myService = result.expectShape(ShapeId.from("smithy.example#MyService"));
        Shape myOperation = result.expectShape(ShapeId.from("smithy.example#MyOperation"));

        assertTrue(myService.hasTrait(SupportedPrincipalTypesTrait.class));
        assertThat(myService.expectTrait(SupportedPrincipalTypesTrait.class).getValues(),
                containsInAnyOrder(
                        "IAMUser",
                        "IAMRole"));

        assertTrue(myOperation.hasTrait(SupportedPrincipalTypesTrait.class));
        assertThat(myOperation.expectTrait(SupportedPrincipalTypesTrait.class).getValues(),
                containsInAnyOrder(
                        "Root",
                        "FederatedUser"));
    }
}
