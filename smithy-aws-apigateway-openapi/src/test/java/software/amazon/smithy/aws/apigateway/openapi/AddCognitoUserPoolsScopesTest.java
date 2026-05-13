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

public class AddCognitoUserPoolsScopesTest {
    private static final String SCHEME_NAME = "aws.auth.cognitoUserPools";
    private static OpenApi result;

    @BeforeAll
    public static void setup() {
        Model model = Model.assembler()
                .discoverModels(AddCognitoUserPoolsScopesTest.class.getClassLoader())
                .addImport(AddCognitoUserPoolsScopesTest.class.getResource("cognito-user-pools-scopes.smithy"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        result = OpenApiConverter.create()
                .config(config)
                .classLoader(AddCognitoUserPoolsScopesTest.class.getClassLoader())
                .convert(model);
    }

    @Test
    public void scopedOperationIncludesScopes() {
        PathItem path = result.getPaths().get("/scoped");
        OperationObject operation = path.getGet().get();
        List<Map<String, List<String>>> security = operation.getSecurity().get();

        Map<String, List<String>> authReq = security.stream()
                .filter(s -> s.containsKey(SCHEME_NAME))
                .findFirst()
                .get();

        assertThat(authReq, hasKey(SCHEME_NAME));
        assertThat(authReq.get(SCHEME_NAME), contains("email", "profile"));
    }

    @Test
    public void unscopedOperationInheritsServiceSecurity() {
        PathItem path = result.getPaths().get("/unscoped");
        OperationObject operation = path.getGet().get();

        // Unscoped operation has no scopes, so no per-operation security
        // is added. The operation inherits security from the service level.
        assertThat(operation.getSecurity().isPresent(), is(false));
    }

    @Test
    public void noAuthOperationRespectsAuthOptOut() {
        PathItem path = result.getPaths().get("/noauth");
        OperationObject operation = path.getGet().get();

        // Operation uses @auth([]) to opt out of authentication. Even though
        // the @cognitoUserPoolsScopes trait is applied, the mapper must not
        // add a Cognito security requirement because Cognito is not an
        // effective auth scheme for this operation.
        List<Map<String, List<String>>> security = operation.getSecurity().get();
        assertThat(security, is(List.of()));
    }
}
