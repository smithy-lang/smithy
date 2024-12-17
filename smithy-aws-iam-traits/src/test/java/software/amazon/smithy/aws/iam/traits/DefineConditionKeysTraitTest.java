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
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class DefineConditionKeysTraitTest {
    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("define-condition-keys.smithy"))
                .assemble()
                .unwrap();

        Shape shape = result.expectShape(ShapeId.from("smithy.example#MyService"));
        DefineConditionKeysTrait trait = shape.expectTrait(DefineConditionKeysTrait.class);
        assertEquals(3, trait.getConditionKeys().size());
        assertFalse(trait.getConditionKey("myservice:Bar").get().isRequired());
        assertFalse(trait.getConditionKey("Foo").get().isRequired());
        assertTrue(trait.getConditionKey("myservice:Baz").get().isRequired());
    }
}
