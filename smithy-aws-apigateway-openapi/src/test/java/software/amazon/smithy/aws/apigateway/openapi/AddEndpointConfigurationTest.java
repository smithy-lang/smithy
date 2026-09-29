/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.ServerObject;

public class AddEndpointConfigurationTest {
    private static final String EXTENSION_NAME = "x-amazon-apigateway-endpoint-configuration";

    @Test
    public void addsExtensionToServerObjectWithVpcEndpointIdsAndDisableFlag() {
        OpenApi result = convert("endpoint-configuration.smithy");

        // The extension goes on the Server object for OpenAPI 3.0.
        assertThat(result.getExtension(EXTENSION_NAME).isPresent(), is(false));
        assertThat(result.getServers().size(), equalTo(1));

        ServerObject server = result.getServers().get(0);
        assertThat(server.getUrl(), equalTo("/"));
        assertThat(server.getExtension(EXTENSION_NAME).isPresent(), is(true));
        ObjectNode extension = server.getExtension(EXTENSION_NAME).get().expectObjectNode();

        assertThat(extension.expectArrayMember("vpcEndpointIds")
                .getElements()
                .stream()
                .map(node -> ((StringNode) node).getValue())
                .collect(Collectors.toList()),
                contains("vpce-0212a4ababd5b8c3e", "vpce-01d622316a7df47f9"));
        assertThat(extension.expectBooleanMember("disableExecuteApiEndpoint").getValue(),
                equalTo(true));

        // types and ipAddressType are not part of the extension.
        assertThat(extension.getMember("types").isPresent(), is(false));
        assertThat(extension.getMember("ipAddressType").isPresent(), is(false));
    }

    @Test
    public void addsExtensionToExistingServerObjects() {
        // Smithy currently lacks a trait that produces OAS Server objects for
        // AddEndpointConfiguration to append the extension to. We'll use a custom
        // OpenApiMapper extension that runs before all mappers
        OpenApiMapper serverAddingMapper = new OpenApiMapper() {
            @Override
            public byte getOrder() {
                return -128;
            }

            @Override
            public OpenApi after(Context<? extends Trait> context, OpenApi openapi) {
                return openapi.toBuilder()
                        .addServer(ServerObject.builder()
                                .url("https://api.example.com")
                                .description("Production")
                                .build())
                        .build();
            }
        };

        Model assembled = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("endpoint-configuration.smithy"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .classLoader(getClass().getClassLoader())
                .addOpenApiMapper(serverAddingMapper)
                .convert(assembled);

        // The existing server is kept (not replaced by a default "/" server)
        // and receives the extension.
        assertThat(result.getServers().size(), equalTo(1));
        ServerObject server = result.getServers().get(0);
        assertThat(server.getUrl(), equalTo("https://api.example.com"));
        assertThat(server.getDescription().get(), equalTo("Production"));
        assertThat(server.getExtension(EXTENSION_NAME).isPresent(), is(true));
        // Extension is not top level
        assertThat(result.getExtension(EXTENSION_NAME).isPresent(), is(false));
    }

    @Test
    public void omitsExtensionWhenOnlyTypesIsSet() {
        OpenApi result = convert("endpoint-configuration-minimal.smithy");

        // Only types is set on the trait, and types is not part of the
        // extension. The mapper must not emit an empty extension or add
        // a default server.
        assertThat(result.getExtension(EXTENSION_NAME).isPresent(), is(false));
        assertThat(result.getServers().isEmpty(), is(true));
    }

    @Test
    public void wrapsVpcEndpointIdCloudFormationVariablesInFnSub() {
        // The CFN substitution runs in updateNode, so convert to node to
        // pick up Fn::Sub wrapping.
        Model assembled = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("endpoint-configuration-cfn.smithy"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        ObjectNode result = OpenApiConverter.create()
                .config(config)
                .classLoader(getClass().getClassLoader())
                .convertToNode(assembled);

        ObjectNode server = result.expectArrayMember("servers")
                .get(0)
                .get()
                .expectObjectNode();
        ObjectNode extension = server.expectObjectMember(EXTENSION_NAME);
        ObjectNode firstId = extension.expectArrayMember("vpcEndpointIds")
                .get(0)
                .get()
                .expectObjectNode();

        assertThat(firstId.expectStringMember("Fn::Sub").getValue(),
                equalTo("${MyVpcEndpointId}"));
    }

    private OpenApi convert(String model) {
        Model assembled = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource(model))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        return OpenApiConverter.create()
                .config(config)
                .classLoader(getClass().getClassLoader())
                .convert(assembled);
    }
}
