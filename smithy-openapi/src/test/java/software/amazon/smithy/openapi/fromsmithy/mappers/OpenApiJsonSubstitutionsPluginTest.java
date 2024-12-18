/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.mappers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.utils.MapUtils;

public class OpenApiJsonSubstitutionsPluginTest {
    @Test
    public void removesBySubstitution() {
        Model model = Model.assembler()
                .addImport(RemoveUnusedComponentsTest.class.getResource("substitutions.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();

        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        config.setSubstitutions(MapUtils.of("SUB_HELLO", Node.from("hello")));
        ObjectNode openApi = OpenApiConverter.create()
                .config(config)
                .convertToNode(model);
        String description = openApi.getObjectMember("info").get().getStringMember("description").get().getValue();

        Assertions.assertEquals("hello", description);
    }
}
