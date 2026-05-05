/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.utils.ListUtils;

public class AddApiKeyRequiredTest {
    private static OpenApi result;

    @BeforeAll
    public static void setup() {
        Model model = Model.assembler()
                .discoverModels(AddApiKeyRequiredTest.class.getClassLoader())
                .addImport(AddApiKeyRequiredTest.class.getResource("api-key-required.smithy"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        result = OpenApiConverter.create()
                .config(config)
                .classLoader(AddApiKeyRequiredTest.class.getClassLoader())
                .convert(model);
    }

    @Test
    public void addsApiKeySecurityScheme() {
        SecurityScheme scheme = result.getComponents().getSecuritySchemes().get("api_key");
        assertThat(scheme.getType(), equalTo("apiKey"));
        assertThat(scheme.getName().get(), equalTo("x-api-key"));
        assertThat(scheme.getIn().get(), equalTo("header"));
    }

    @Test
    public void keyedOperationHasApiKeySecurity() {
        OperationObject operation = result.getPaths().get("/items").getGet().get();
        List<Map<String, List<String>>> security = operation.getSecurity().get();

        Map<String, List<String>> apiKeyReq = security.stream()
                .filter(s -> s.containsKey("api_key"))
                .findFirst()
                .get();

        assertThat(apiKeyReq, hasKey("api_key"));
        assertThat(apiKeyReq.get("api_key"), is(ListUtils.of()));
    }

    @Test
    public void openOperationHasNoApiKeySecurity() {
        OperationObject operation = result.getPaths().get("/health").getGet().get();
        // Open operation should not have per-operation security with api_key
        if (operation.getSecurity().isPresent()) {
            boolean hasApiKey = operation.getSecurity()
                    .get()
                    .stream()
                    .anyMatch(s -> s.containsKey("api_key"));
            assertFalse(hasApiKey);
        }
    }
}
