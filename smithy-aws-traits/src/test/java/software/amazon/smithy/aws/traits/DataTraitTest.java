/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class DataTraitTest {

    private Model getModel() {
        return Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("data-classification-model.json"))
                .assemble()
                .unwrap();
    }

    @Test
    public void loadsWithString() {
        Model model = getModel();
        assertTrue(model.getShape(ShapeId.from("ns.foo#A"))
                .flatMap(shape -> shape.getTrait(DataTrait.class))
                .filter(trait -> trait.getValue().equals("account"))
                .isPresent());

        assertTrue(model.getShape(ShapeId.from("ns.foo#B"))
                .flatMap(shape -> shape.getTrait(DataTrait.class))
                .filter(trait -> trait.getValue().equals("tagging"))
                .isPresent());
    }
}
