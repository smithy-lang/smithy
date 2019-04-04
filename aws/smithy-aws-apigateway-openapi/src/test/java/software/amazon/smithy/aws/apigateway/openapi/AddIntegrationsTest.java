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

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.LoaderUtils;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;

public class AddIntegrationsTest {
    @Test
    public void addsIntegrations() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("integrations.json"))
                .assemble()
                .unwrap();
        var result = OpenApiConverter.create().convert(model, ShapeId.from("smithy.example#Service"));
        var expectedNode = Node.parse(LoaderUtils.readInputStream(
                getClass().getResourceAsStream("integrations.openapi.json"), "UTF-8"));

        Node.assertEquals(result, expectedNode);
    }
}
