/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

public class AddAuthorizersTest {
    @Test
    public void addsAuthorizers() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("authorizers.json"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("ns.foo#SomeService"));
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .classLoader(getClass().getClassLoader())
                .convert(model);
        SecurityScheme sigV4 = result.getComponents().getSecuritySchemes().get("sigv4");

        assertThat(result.getComponents().getSecuritySchemes().get("aws.v4"), nullValue());
        assertThat(sigV4.getType(), equalTo("apiKey"));
        assertThat(sigV4.getName().get(), equalTo("Authorization"));
        assertThat(sigV4.getIn().get(), equalTo("header"));
        assertThat(sigV4.getExtension("x-amazon-apigateway-authtype").get(), equalTo(Node.from("awsSigv4")));
        ObjectNode authorizer = sigV4.getExtension("x-amazon-apigateway-authorizer").get().expectObjectNode();
        assertThat(authorizer.getStringMember("type").get().getValue(), equalTo("request"));
        assertThat(authorizer.getStringMember("authorizerUri").get().getValue(), equalTo("arn:foo:baz"));
        assertThat(authorizer.getStringMember("authorizerCredentials").get().getValue(), equalTo("arn:foo:bar"));
        assertThat(authorizer.getStringMember("identitySource").get().getValue(), equalTo("mapping.expression"));
        assertThat(authorizer.getStringMember("identityValidationExpression").get().getValue(), equalTo("[A-Z]+"));
        assertThat(authorizer.getNumberMember("authorizerResultTtlInSeconds").get().getValue(), equalTo(100));
        assertThat(authorizer.getStringMember("authorizerPayloadFormatVersion").get().getValue(), equalTo("2.0"));
        assertThat(authorizer.getBooleanMember("enableSimpleResponses").get().getValue(), equalTo(true));
    }

    @Test
    public void addsOnlyAuthType() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("basic-authorizers.json"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("ns.foo#SomeService"));
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .classLoader(getClass().getClassLoader())
                .convert(model);
        SecurityScheme sigV4 = result.getComponents().getSecuritySchemes().get("sigv4");

        assertThat(result.getComponents().getSecuritySchemes().get("aws.v4"), nullValue());
        assertThat(sigV4.getType(), equalTo("apiKey"));
        assertThat(sigV4.getName().get(), equalTo("Authorization"));
        assertThat(sigV4.getIn().get(), equalTo("header"));
        assertThat(sigV4.getExtension("x-amazon-apigateway-authtype").get(), equalTo(Node.from("awsSigv4")));
        assertFalse(sigV4.getExtension("x-amazon-apigateway-authorizer").isPresent());
    }

    @Test
    public void addsCustomAuthType() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("custom-auth-type-authorizer.json"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("ns.foo#SomeService"));
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .classLoader(getClass().getClassLoader())
                .convert(model);
        SecurityScheme sigV4 = result.getComponents().getSecuritySchemes().get("sigv4");

        assertThat(result.getComponents().getSecuritySchemes().get("aws.v4"), nullValue());
        assertThat(sigV4.getType(), equalTo("apiKey"));
        assertThat(sigV4.getName().get(), equalTo("Authorization"));
        assertThat(sigV4.getIn().get(), equalTo("header"));
        assertThat(sigV4.getExtension("x-amazon-apigateway-authtype").get(), equalTo(Node.from("myCustomType")));
        assertFalse(sigV4.getExtension("x-amazon-apigateway-authorizer").isPresent());
    }

    @Test
    public void emptyCustomAuthTypeNotSet() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("empty-custom-auth-type-authorizer.json"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("ns.foo#SomeService"));
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .classLoader(getClass().getClassLoader())
                .convert(model);
        SecurityScheme apiKey = result.getComponents().getSecuritySchemes().get("api_key");

        assertThat(apiKey.getType(), equalTo("apiKey"));
        assertThat(apiKey.getName().get(), equalTo("x-api-key"));
        assertThat(apiKey.getIn().get(), equalTo("header"));
        assertFalse(apiKey.getExtension("x-amazon-apigateway-authtype").isPresent());
        assertFalse(apiKey.getExtension("x-amazon-apigateway-authorizer").isPresent());
    }

    @Test
    public void addsOperationLevelApiKeyScheme() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("operation-http-api-key-security.json"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .classLoader(getClass().getClassLoader())
                .convert(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("operation-http-api-key-security.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void resolvesEffectiveAuthorizersForEachOperation() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("effective-authorizers.smithy"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#ServiceA"));
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .classLoader(getClass().getClassLoader())
                .convert(model);

        // The security of the service is just "foo".
        assertThat(result.getSecurity(), contains(MapUtils.of("foo", ListUtils.of())));
        // The "baz" and "foo" securitySchemes must be present.
        assertThat(result.getComponents().getSecuritySchemes().keySet(), containsInAnyOrder("baz", "foo"));
        // The security schemes of operationA must be empty.
        assertThat(result.getPaths().get("/operationA").getGet().get().getSecurity(), is(Optional.empty()));
        // The security schemes of operationB must be "baz".
        assertThat(result.getPaths().get("/operationB").getGet().get().getSecurity(),
                is(Optional.of(ListUtils.of(MapUtils.of("baz", ListUtils.of())))));
    }
}
