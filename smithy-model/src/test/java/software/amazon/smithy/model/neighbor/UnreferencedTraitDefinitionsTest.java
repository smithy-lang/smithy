/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.neighbor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class UnreferencedTraitDefinitionsTest {
    @Test
    public void shouldReportDefinitionsForTraitsThatAreNotUsed() {
        Model model = Model.assembler()
                .addImport(UnreferencedTraitDefinitionsTest.class.getResource("unreferenced-test.json"))
                .assemble()
                .unwrap();
        UnreferencedTraitDefinitions unreferencedTraitDefinitions = new UnreferencedTraitDefinitions();

        assertThat(unreferencedTraitDefinitions.compute(model),
                contains(model.expectShape(ShapeId.from("ns.foo#quux"))));
    }
}
