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

package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.Protocol;
import software.amazon.smithy.model.traits.ProtocolsTrait;
import software.amazon.smithy.utils.ListUtils;

public class AuthIndexTest {
    @Test
    public void supportedServiceSchemesDefaultsToAllProtocols() {
        ServiceShape service = ServiceShape.builder()
                .id("example.smithy#Service")
                .version("XYZ")
                .addTrait(ProtocolsTrait.builder()
                                  .addProtocol(Protocol.builder().name("json").addAuth("foo").addAuth("baz").build())
                                  .addProtocol(Protocol.builder().name("xml").addAuth("qux").addAuth("foo").build())
                                  .build())
                .build();
        Model model = Model.builder().addShape(service).build();
        AuthIndex authIndex = model.getKnowledge(AuthIndex.class);

        assertThat(authIndex.getDefaultServiceSchemes(service), equalTo(ListUtils.of("foo", "baz", "qux")));
    }

    @Test
    public void getSchemesOfOperationBoundToServiceAndProtocol() {
        OperationShape operation1 = OperationShape.builder().id("example.smithy#O1").build();
        OperationShape operation2 = OperationShape.builder()
                .id("example.smithy#O2")
                .addTrait(AuthTrait.builder().addValue("foo").build())
                .build();
        OperationShape operation3 = OperationShape.builder()
                .id("example.smithy#O3")
                .addTrait(AuthTrait.builder().addValue("baz").addValue("foo").build())
                .build();
        ServiceShape service = ServiceShape.builder()
                .id("example.smithy#Service")
                .version("XYZ")
                .addOperation(operation1)
                .addOperation(operation2)
                .addOperation(operation3)
                .addTrait(AuthTrait.builder().addValue("foo").build())
                .addTrait(ProtocolsTrait.builder()
                        .addProtocol(Protocol.builder().name("json").addAuth("foo").addAuth("baz").build())
                        .addProtocol(Protocol.builder().name("xml").addAuth("qux").build())
                        .build())
                .build();
        Model model = Model.builder().addShapes(service, operation1, operation2, operation3).build();
        AuthIndex authIndex = model.getKnowledge(AuthIndex.class);

        // Use the schemes defined on the shape itself or the schemes of the service.
        assertThat(authIndex.getOperationSchemes(service, operation1), equalTo(ListUtils.of("foo")));
        assertThat(authIndex.getOperationSchemes(service, operation2), equalTo(ListUtils.of("foo")));
        assertThat(authIndex.getOperationSchemes(service, operation3), equalTo(ListUtils.of("baz", "foo")));

        // Get the intersection of the schemes of the shape and the protocol.
        assertThat(authIndex.getOperationSchemes(service, operation1, "json"), equalTo(ListUtils.of("foo")));
        assertThat(authIndex.getOperationSchemes(service, operation2, "json"), equalTo(ListUtils.of("foo")));
        assertThat(authIndex.getOperationSchemes(service, operation3, "json"), equalTo(ListUtils.of("baz", "foo")));
    }

    @Test
    public void includesNoneEvenIfNotListedInSchemes() {
        OperationShape operation1 = OperationShape.builder()
                .id("example.smithy#O1")
                .addTrait(AuthTrait.builder().addValue("none").build())
                .build();
        ServiceShape service = ServiceShape.builder()
                .id("example.smithy#Service")
                .version("XYZ")
                .addOperation(operation1)
                .addTrait(ProtocolsTrait.builder()
                                  .addProtocol(Protocol.builder().name("json").addAuth("foo").build())
                                  .build())
                .build();
        Model model = Model.builder().addShapes(service, operation1).build();
        AuthIndex authIndex = model.getKnowledge(AuthIndex.class);

        assertThat(authIndex.getOperationSchemes(service, operation1), equalTo(ListUtils.of("none")));
        assertThat(authIndex.getOperationSchemes(service, operation1, "json"), equalTo(ListUtils.of("none")));
    }
}
