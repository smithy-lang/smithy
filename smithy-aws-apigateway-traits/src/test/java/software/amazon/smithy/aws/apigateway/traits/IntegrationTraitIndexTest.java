/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.apigateway.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class IntegrationTraitIndexTest {
    @Test
    public void resolvesTraits() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("integration-index.json"))
                .assemble()
                .unwrap();

        IntegrationTraitIndex index = IntegrationTraitIndex.of(model);
        ShapeId service = ShapeId.from("ns.foo#Service");
        ShapeId a = ShapeId.from("ns.foo#A");
        ShapeId b = ShapeId.from("ns.foo#B");
        ShapeId o1 = ShapeId.from("ns.foo#O1");
        ShapeId o2 = ShapeId.from("ns.foo#O2");
        ShapeId o3 = ShapeId.from("ns.foo#O3");
        ShapeId o4 = ShapeId.from("ns.foo#O4");
        ShapeId o5 = ShapeId.from("ns.foo#O5");

        assertThat(getTrait(index, service, service).getUri(), equalTo("arn:Service"));
        assertThat(getTrait(index, service, a).getUri(), equalTo("arn:Service"));
        assertThat(getTrait(index, service, b).getUri(), equalTo("arn:B"));
        assertThat(getTrait(index, service, o1).getUri(), equalTo("arn:O1"));
        assertThat(getTrait(index, service, o2).getUri(), equalTo("arn:Service"));
        assertThat(getTrait(index, service, o3).getUri(), equalTo("arn:O3"));
        assertThat(getTrait(index, service, o4).getUri(), equalTo("arn:B"));
        assertThat(getTrait(index, service, o5).getUri(), equalTo("arn:O5"));
    }

    private IntegrationTrait getTrait(IntegrationTraitIndex index, ShapeId service, ShapeId shape) {
        return index.getIntegrationTrait(service, shape, IntegrationTrait.class).get();
    }
}
