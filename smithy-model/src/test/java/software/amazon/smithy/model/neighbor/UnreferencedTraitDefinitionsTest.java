/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
