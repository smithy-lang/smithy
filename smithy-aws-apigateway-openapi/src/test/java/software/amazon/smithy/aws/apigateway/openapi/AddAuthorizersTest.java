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
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.SecurityScheme;
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
        OpenApi result = OpenApiConverter.create()
                .classLoader(getClass().getClassLoader())
                .convert(model, ShapeId.from("ns.foo#SomeService"));
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
    }

    @Test
    public void addsOnlyAuthType() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("basic-authorizers.json"))
                .assemble()
                .unwrap();
        OpenApi result = OpenApiConverter.create()
                .classLoader(getClass().getClassLoader())
                .convert(model, ShapeId.from("ns.foo#SomeService"));
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
        OpenApi result = OpenApiConverter.create()
                .classLoader(getClass().getClassLoader())
                .convert(model, ShapeId.from("ns.foo#SomeService"));
        SecurityScheme sigV4 = result.getComponents().getSecuritySchemes().get("sigv4");

        assertThat(result.getComponents().getSecuritySchemes().get("aws.v4"), nullValue());
        assertThat(sigV4.getType(), equalTo("apiKey"));
        assertThat(sigV4.getName().get(), equalTo("Authorization"));
        assertThat(sigV4.getIn().get(), equalTo("header"));
        assertThat(sigV4.getExtension("x-amazon-apigateway-authtype").get(), equalTo(Node.from("myCustomType")));
        assertFalse(sigV4.getExtension("x-amazon-apigateway-authorizer").isPresent());
    }

    @Test
    public void resolvesEffectiveAuthorizersForEachOperation() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("effective-authorizers.smithy"))
                .assemble()
                .unwrap();
        OpenApi result = OpenApiConverter.create()
                .classLoader(getClass().getClassLoader())
                .convert(model, ShapeId.from("smithy.example#ServiceA"));

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
