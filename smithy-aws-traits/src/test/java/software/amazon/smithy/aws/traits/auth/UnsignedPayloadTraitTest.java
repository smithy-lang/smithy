/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class UnsignedPayloadTraitTest {
    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("unsigned-request-payload.json"))
                .assemble()
                .unwrap();

        assertTrue(result
                .getShape(ShapeId.from("ns.foo#Unsigned1"))
                .flatMap(shape -> shape.getTrait(UnsignedPayloadTrait.class))
                .isPresent());
    }
}
