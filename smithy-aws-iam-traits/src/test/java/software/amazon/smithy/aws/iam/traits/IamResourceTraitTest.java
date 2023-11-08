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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class IamResourceTraitTest {
    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("iam-resource.smithy"))
                .assemble()
                .unwrap();

        Shape superResource = result.expectShape(ShapeId.from("smithy.example#SuperResource"));

        assertTrue(superResource.hasTrait(IamResourceTrait.class));
        assertEquals(superResource.expectTrait(IamResourceTrait.class).getName().get(), "super");
        assertEquals(superResource.expectTrait(IamResourceTrait.class).getRelativeDocumentation().get(), "API-Super.html");
        assertFalse(superResource.expectTrait(IamResourceTrait.class).isDisableConditionKeyInheritance());
    }
}
