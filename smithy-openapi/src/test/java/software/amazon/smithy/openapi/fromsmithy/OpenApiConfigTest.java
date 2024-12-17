/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.jsonschema.JsonSchemaVersion;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.OpenApiVersion;

public class OpenApiConfigTest {
    @Test
    public void throwsOnDisableProperties() {
        Node disableTest = Node.objectNode().withMember("disable.additionalProperties", Node.from(true));

        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            OpenApiConfig.fromNode(disableTest);
        });

        assertThat(thrown.getMessage(), containsString("disableFeatures"));
    }

    @Test
    public void throwsOnOpenApiUseProperties() {
        Node openApiUseTest = Node.objectNode().withMember("openapi.use.xml", Node.from(true));

        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            OpenApiConfig.fromNode(openApiUseTest);
        });

        assertThat(thrown.getMessage(), containsString("disableFeatures"));
    }

    @Test
    public void convertsExplicitlyMappedProperties() {
        Node mappedTest = Node.objectNode()
                .withMember("openapi.tags", Node.from(true))
                .withMember("openapi.ignoreUnsupportedTraits", Node.from(true));
        OpenApiConfig config = OpenApiConfig.fromNode(mappedTest);

        assertThat(config.getTags(), equalTo(true));
        assertThat(config.getIgnoreUnsupportedTraits(), equalTo(true));
    }

    @Test
    public void putsAdditionalPropertiesInExtensions() {
        Node mappedTest = Node.objectNode()
                .withMember("tags", true)
                .withMember("apiGatewayType", "REST");
        OpenApiConfig config = OpenApiConfig.fromNode(mappedTest);

        assertThat(config.getTags(), equalTo(true));
        assertThat(config.getExtensions().getStringMap(), hasKey("apiGatewayType"));
    }

    @Test
    public void correctlySetsJsonSchemaVersionDefault() {
        Node mappedTest = Node.objectNode();
        OpenApiConfig config = OpenApiConfig.fromNode(mappedTest);

        assertThat(config.getVersion(), equalTo(OpenApiVersion.VERSION_3_0_2));
        assertThat(config.getJsonSchemaVersion(), equalTo(JsonSchemaVersion.DRAFT07));
    }

    @Test
    public void correctlySetsJsonSchemaVersion() {
        Node mappedTest = Node.objectNode().withMember("version", "3.1.0");

        OpenApiConfig config = OpenApiConfig.fromNode(mappedTest);

        assertThat(config.getVersion(), equalTo(OpenApiVersion.VERSION_3_1_0));
        assertThat(config.getJsonSchemaVersion(), equalTo(JsonSchemaVersion.DRAFT2020_12));
    }

}
