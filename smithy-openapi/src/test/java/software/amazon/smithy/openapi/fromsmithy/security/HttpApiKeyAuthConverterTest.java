/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpApiKeyAuthTrait;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.IoUtils;

public class HttpApiKeyAuthConverterTest {
    @Test
    public void addsCustomApiKeyAuth() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("http-api-key-security.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create().config(config).convert(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("http-api-key-security.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void addsCustomApiKeyBearerAuth() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("http-api-key-bearer-security.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create().config(config).convert(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("http-api-key-bearer-security.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void returnsTraitHeader() {
        HttpApiKeyAuthConverter converter = new HttpApiKeyAuthConverter();
        HttpApiKeyAuthTrait trait = HttpApiKeyAuthTrait.builder()
                .name("x-api-key")
                .in(HttpApiKeyAuthTrait.Location.HEADER)
                .build();

        assertThat(converter.getAuthRequestHeaders(null, trait), containsInAnyOrder("x-api-key"));
    }
}
