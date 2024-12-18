/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.traits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

class DualStackOnlyEndpointsTraitTest {
    @Test
    public void loadsFromModel() {
        final Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("dualStackOnlyEndpoints.smithy"))
                .assemble()
                .unwrap();

        Optional<DualStackOnlyEndpointsTrait> trait = model
                .expectShape(ShapeId.from("ns.foo#Service1"))
                .asServiceShape()
                .get()
                .getTrait(DualStackOnlyEndpointsTrait.class);

        assertTrue(trait.isPresent());
    }
}
