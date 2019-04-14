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

package software.amazon.smithy.openapi.fromsmithy.mappers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;

public class JsonSubstitutionsPluginTest {
    @Test
    public void removesBySubstitution() {
        Model model = Model.assembler()
                .addImport(RemoveUnusedComponentsTest.class.getResource("substitutions.smithy"))
                .assemble()
                .unwrap();

        ObjectNode openApi = OpenApiConverter.create()
                .putSetting(OpenApiConstants.SUBSTITUTIONS, Node.objectNode()
                        .withMember("SUB_HELLO", Node.from("hello")))
                .convertToNode(model, ShapeId.from("smithy.example#Service"));
        String description = openApi.getObjectMember("info").get().getStringMember("description").get().getValue();

        Assertions.assertEquals("hello", description);
    }
}
