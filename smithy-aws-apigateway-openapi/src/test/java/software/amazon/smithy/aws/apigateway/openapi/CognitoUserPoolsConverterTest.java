/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.ValidatedResultException;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.IoUtils;

public class CognitoUserPoolsConverterTest {
    @Test
    public void addsAwsV4() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cognito-user-pools-security.json"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create().config(config).convert(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("cognito-user-pools-security.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void requiresProviderArns() {
        Assertions.assertThrows(ValidatedResultException.class, () -> {
            Model model = Model.assembler(getClass().getClassLoader())
                    .discoverModels(getClass().getClassLoader())
                    .addImport(getClass().getResource("invalid-cognito-user-pools-security.json"))
                    .assemble()
                    .unwrap();
            OpenApiConfig config = new OpenApiConfig();
            config.setService(ShapeId.from("smithy.example#Service"));
            OpenApiConverter.create().config(config).convert(model);
        });
    }
}
