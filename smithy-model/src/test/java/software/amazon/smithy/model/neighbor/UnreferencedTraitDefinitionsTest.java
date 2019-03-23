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
import static org.hamcrest.Matchers.equalTo;

import java.util.Collections;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;

public class UnreferencedTraitDefinitionsTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(UnreferencedTraitDefinitionsTest.class.getResource("unreferenced-test.json"))
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void shouldReportDefinitionsForTraitsThatAreNotUsed() {
        UnreferencedTraitDefinitions unreferencedTraitDefinitions = new UnreferencedTraitDefinitions();

        assertThat(unreferencedTraitDefinitions.compute(model),
                equalTo(model.getTraitDefinition("ns.foo#quux").map(Collections::singleton).get()));
    }
}
