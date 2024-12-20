/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.traits;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

class RuleBasedEndpointsTraitTest {
    @Test
    public void loadsFromModel() {
        final Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("ruleBasedEndpoints.smithy"))
                .assemble()
                .unwrap();

        Optional<RuleBasedEndpointsTrait> trait = model
                .expectShape(ShapeId.from("ns.foo#Service1"))
                .asServiceShape()
                .get()
                .getTrait(RuleBasedEndpointsTrait.class);

        assertTrue(trait.isPresent());
    }
}
