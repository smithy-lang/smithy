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

import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.Protocol;
import software.amazon.smithy.model.traits.ProtocolsTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.utils.ListUtils;

public class IncludeProtocolsTest {

    @Test
    public void filtersUnsupportedProtocols() {
        ServiceShape service = ServiceShape.builder()
                .version("1")
                .addTrait(ProtocolsTrait.builder()
                        .addProtocol(Protocol.builder().name("foo").build())
                        .addProtocol(Protocol.builder().name("qux").build()).build())
                .id("ns.foo#baz")
                .build();
        Model model = Model.builder().addShape(service).build();
        Model result = new IncludeProtocols()
                .createTransformer(Collections.singletonList("qux"))
                .apply(ModelTransformer.create(), model);
        ServiceShape shape = result.getShape(service.getId()).get().asServiceShape().get();

        Assertions.assertEquals(shape.getTrait(ProtocolsTrait.class).get().getProtocolNames(), ListUtils.of("qux"));
    }
}
