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
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;

public class AddEndpointConfigurationTest {
    private static final String EXTENSION_NAME = "x-amazon-apigateway-endpoint-configuration";

    @Test
    public void addsExtensionWithVpcEndpointIdsAndDisableFlag() {
        OpenApi result = convert("endpoint-configuration.smithy");

        assertThat(result.getExtension(EXTENSION_NAME).isPresent(), is(true));
        ObjectNode extension = result.getExtension(EXTENSION_NAME).get().expectObjectNode();

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
    public void omitsExtensionWhenOnlyTypesIsSet() {
        OpenApi result = convert("endpoint-configuration-minimal.smithy");

        // Only types is set on the trait, and types is not part of the
        // extension. The mapper must not emit an empty extension.
        assertThat(result.getExtension(EXTENSION_NAME).isPresent(), is(false));
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

        ObjectNode extension = result.expectObjectMember(EXTENSION_NAME);
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
