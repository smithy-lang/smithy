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

package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.ProtocolsTrait;
import software.amazon.smithy.model.transform.ModelTransformer;

public class IncludeProtocolsTest {

    @Test
    public void filtersUnsupportedProtocols() {
        ServiceShape service = ServiceShape.builder()
                .version("1")
                .addTrait(ProtocolsTrait.builder()
                        .putProtocol("foo", ProtocolsTrait.Protocol.builder().build())
                        .putProtocol("qux", ProtocolsTrait.Protocol.builder().build()).build())
                .id("ns.foo#baz")
                .build();
        ShapeIndex index = ShapeIndex.builder().addShape(service).build();
        Model model = Model.builder().shapeIndex(index).build();
        Model result = new IncludeProtocols()
                .createTransformer(Collections.singletonList("qux"))
                .apply(ModelTransformer.create(), model);
        ServiceShape shape = result.getShapeIndex().getShape(service.getId()).get().asServiceShape().get();

        assertThat(shape.getTrait(ProtocolsTrait.class).get().getProtocols().keySet(), contains("qux"));
    }
}
