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

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.AuthenticationSchemesTrait;
import software.amazon.smithy.model.traits.AuthenticationTrait;
import software.amazon.smithy.model.traits.ProtocolsTrait;

public class AuthenticationSchemesIndexTest {
    @Test
    public void getsSupportedSchemesAndDefaultSchemesOfService() {
        ServiceShape service = ServiceShape.builder()
                .id("example.smithy#Service")
                .version("XYZ")
                .addTrait(AuthenticationTrait.builder()
                        .putAuthenticationScheme("foo", AuthenticationTrait.AuthScheme.builder().build())
                        .putAuthenticationScheme("baz", AuthenticationTrait.AuthScheme.builder().build())
                        .build())
                .build();
        ShapeIndex index = ShapeIndex.builder().addShape(service).build();
        Model model = Model.builder().shapeIndex(index).build();
        AuthenticationSchemeIndex authIndex = model.getKnowledge(AuthenticationSchemeIndex.class);

        assertThat(authIndex.getSupportedServiceSchemes(service), equalTo(List.of("foo", "baz")));
        assertThat(authIndex.getDefaultServiceSchemes(service), equalTo(List.of("foo", "baz")));
    }

    @Test
    public void getsDefaultSchemesOfServiceWithAuthSchemesTrait() {
        ServiceShape service = ServiceShape.builder()
                .id("example.smithy#Service")
                .version("XYZ")
                .addTrait(AuthenticationTrait.builder()
                        .putAuthenticationScheme("foo", AuthenticationTrait.AuthScheme.builder().build())
                        .putAuthenticationScheme("baz", AuthenticationTrait.AuthScheme.builder().build())
                        .build())
                .addTrait(AuthenticationSchemesTrait.builder().addValue("baz").build())
                .build();
        ShapeIndex index = ShapeIndex.builder().addShape(service).build();
        Model model = Model.builder().shapeIndex(index).build();
        AuthenticationSchemeIndex authIndex = model.getKnowledge(AuthenticationSchemeIndex.class);

        assertThat(authIndex.getDefaultServiceSchemes(service), equalTo(List.of("baz")));
    }

    @Test
    public void getsSupportedSchemesOfServiceProtocol() {
        ServiceShape service = ServiceShape.builder()
                .id("example.smithy#Service")
                .version("XYZ")
                .addTrait(AuthenticationTrait.builder()
                        .putAuthenticationScheme("foo", AuthenticationTrait.AuthScheme.builder().build())
                        .putAuthenticationScheme("baz", AuthenticationTrait.AuthScheme.builder().build())
                        .build())
                .addTrait(AuthenticationSchemesTrait.builder().addValue("baz").build())
                .addTrait(ProtocolsTrait.builder()
                        .putProtocol("json", ProtocolsTrait.Protocol.builder().addAuthentication("baz").build())
                        .build())
                .build();
        ShapeIndex index = ShapeIndex.builder().addShape(service).build();
        Model model = Model.builder().shapeIndex(index).build();
        AuthenticationSchemeIndex authIndex = model.getKnowledge(AuthenticationSchemeIndex.class);

        assertThat(authIndex.getSupportedServiceSchemes(service, "json"), equalTo(List.of("baz")));
    }

    @Test
    public void getSchemesOfOperationBoundToServiceAndProtocol() {
        OperationShape operation1 = OperationShape.builder().id("example.smithy#O1").build();
        OperationShape operation2 = OperationShape.builder()
                .id("example.smithy#O2")
                .addTrait(AuthenticationSchemesTrait.builder().addValue("foo").build())
                .build();
        OperationShape operation3 = OperationShape.builder()
                .id("example.smithy#O3")
                .addTrait(AuthenticationSchemesTrait.builder().addValue("baz").addValue("foo").build())
                .build();
        ServiceShape service = ServiceShape.builder()
                .id("example.smithy#Service")
                .version("XYZ")
                .addOperation(operation1)
                .addOperation(operation2)
                .addOperation(operation3)
                .addTrait(AuthenticationTrait.builder()
                        .putAuthenticationScheme("foo", AuthenticationTrait.AuthScheme.builder().build())
                        .putAuthenticationScheme("baz", AuthenticationTrait.AuthScheme.builder().build())
                        .build())
                .addTrait(AuthenticationSchemesTrait.builder().addValue("baz").build())
                .addTrait(ProtocolsTrait.builder()
                        .putProtocol("json", ProtocolsTrait.Protocol.builder().addAuthentication("baz").build())
                        .build())
                .build();
        ShapeIndex index = ShapeIndex.builder().addShapes(service, operation1, operation2, operation3).build();
        Model model = Model.builder().shapeIndex(index).build();
        AuthenticationSchemeIndex authIndex = model.getKnowledge(AuthenticationSchemeIndex.class);

        // Use the schemes defined on the shape itself or the schemes of the service.
        assertThat(authIndex.getOperationSchemes(service, operation1), equalTo(List.of("baz")));
        assertThat(authIndex.getOperationSchemes(service, operation2), equalTo(List.of("foo")));
        assertThat(authIndex.getOperationSchemes(service, operation3), equalTo(List.of("baz", "foo")));

        // Get the intersection of the schemes of the shape and the protocol.
        assertThat(authIndex.getOperationSchemes(service, operation1, "json"), equalTo(List.of("baz")));
        assertThat(authIndex.getOperationSchemes(service, operation2, "json"), equalTo(List.of()));
        assertThat(authIndex.getOperationSchemes(service, operation3, "json"), equalTo(List.of("baz")));
    }

    @Test
    public void getSchemesOfOperationBoundToServiceAndProtocolOrderedByProtocol() {
        OperationShape operation1 = OperationShape.builder().id("example.smithy#O1").build();
        ServiceShape service = ServiceShape.builder()
                .id("example.smithy#Service")
                .version("XYZ")
                .addOperation(operation1)
                .addTrait(AuthenticationTrait.builder()
                        .putAuthenticationScheme("foo", AuthenticationTrait.AuthScheme.builder().build())
                        .putAuthenticationScheme("baz", AuthenticationTrait.AuthScheme.builder().build())
                        .build())
                .addTrait(ProtocolsTrait.builder()
                        .putProtocol("json", ProtocolsTrait.Protocol.builder()
                                .addAuthentication("baz")
                                .addAuthentication("foo")
                                .build())
                        .build())
                .build();
        ShapeIndex index = ShapeIndex.builder().addShapes(service, operation1).build();
        Model model = Model.builder().shapeIndex(index).build();
        AuthenticationSchemeIndex authIndex = model.getKnowledge(AuthenticationSchemeIndex.class);

        // Use the schemes defined on the shape itself or the schemes of the service.
        assertThat(authIndex.getOperationSchemes(service, operation1), equalTo(List.of("foo", "baz")));

        // Get the intersection of the schemes of the shape and the protocol.
        assertThat(authIndex.getOperationSchemes(service, operation1, "json"), equalTo(List.of("baz", "foo")));
    }

    @Test
    public void getsAllServiceSchemesWhenProtocolHasNoSchemes() {
        OperationShape operation1 = OperationShape.builder().id("example.smithy#O1").build();
        ServiceShape service = ServiceShape.builder()
                .id("example.smithy#Service")
                .version("XYZ")
                .addOperation(operation1)
                .addTrait(AuthenticationTrait.builder()
                        .putAuthenticationScheme("foo", AuthenticationTrait.AuthScheme.builder().build())
                        .putAuthenticationScheme("baz", AuthenticationTrait.AuthScheme.builder().build())
                        .build())
                .addTrait(ProtocolsTrait.builder()
                        .putProtocol("json", ProtocolsTrait.Protocol.builder().build())
                        .build())
                .build();
        ShapeIndex index = ShapeIndex.builder().addShapes(service, operation1).build();
        Model model = Model.builder().shapeIndex(index).build();
        AuthenticationSchemeIndex authIndex = model.getKnowledge(AuthenticationSchemeIndex.class);

        assertThat(authIndex.getOperationSchemes(service, operation1, "json"), equalTo(List.of("foo", "baz")));
    }

    @Test
    public void getsAllSortedServiceSchemesWhenProtocolHasNoSchemes() {
        ServiceShape service = ServiceShape.builder()
                .id("example.smithy#Service")
                .version("XYZ")
                .addTrait(AuthenticationTrait.builder()
                        .putAuthenticationScheme("qux", AuthenticationTrait.AuthScheme.builder().build())
                        .putAuthenticationScheme("foo", AuthenticationTrait.AuthScheme.builder().build())
                        .putAuthenticationScheme("baz", AuthenticationTrait.AuthScheme.builder().build())
                        .build())
                .addTrait(AuthenticationSchemesTrait.builder().addValue("baz").addValue("foo").build())
                .addTrait(ProtocolsTrait.builder()
                        .putProtocol("json", ProtocolsTrait.Protocol.builder().build())
                        .build())
                .build();
        ShapeIndex index = ShapeIndex.builder().addShapes(service).build();
        Model model = Model.builder().shapeIndex(index).build();
        AuthenticationSchemeIndex authIndex = model.getKnowledge(AuthenticationSchemeIndex.class);

        assertThat(authIndex.getSupportedServiceSchemes(service, "json"), equalTo(List.of("baz", "foo", "qux")));
    }
}
