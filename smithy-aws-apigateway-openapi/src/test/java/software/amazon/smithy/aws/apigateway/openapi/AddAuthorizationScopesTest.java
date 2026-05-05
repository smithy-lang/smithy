/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

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
import software.amazon.smithy.openapi.model.PathItem;

public class AddAuthorizationScopesTest {
    private static OpenApi result;

    @BeforeAll
    public static void setup() {
        Model model = Model.assembler()
                .discoverModels(AddAuthorizationScopesTest.class.getClassLoader())
                .addImport(AddAuthorizationScopesTest.class.getResource("authorization-scopes.smithy"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        result = OpenApiConverter.create()
                .config(config)
                .classLoader(AddAuthorizationScopesTest.class.getClassLoader())
                .convert(model);
    }

    @Test
    public void scopedOperationIncludesScopes() {
        PathItem path = result.getPaths().get("/scoped");
        OperationObject operation = path.getGet().get();
        List<Map<String, List<String>>> security = operation.getSecurity().get();

        // Find the security requirement for our authorizer
        Map<String, List<String>> authReq = security.stream()
                .filter(s -> s.containsKey("my-cognito-auth"))
                .findFirst()
                .get();

        assertThat(authReq, hasKey("my-cognito-auth"));
        assertThat(authReq.get("my-cognito-auth"), contains("email", "profile"));
    }

    @Test
    public void unscopedOperationInheritsServiceSecurity() {
        PathItem path = result.getPaths().get("/unscoped");
        OperationObject operation = path.getGet().get();

        // Unscoped operation has the same authorizer as the service and no
        // scopes, so no per-operation security is added. The operation
        // inherits security from the service level.
        assertThat(operation.getSecurity().isPresent(), is(false));
    }
}
