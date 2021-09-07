/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.MapUtils;

public class RemoveUriPrefixTest {
    @Test
    public void removesPrefixFromModeledOperations() {
        OpenApiConfig config = new OpenApiConfig();
        config.setRemoveUriPrefix("/v1");
        config.setService(ShapeId.from("smithy.example#Example"));

        Model model = Model.assembler()
                .addImport(RemoveUriPrefixTest.class.getResource("remove-uri-prefix.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApi result = OpenApiConverter.create().config(config).convert(model);

        assertThat(result.getPaths().keySet(), hasItem("/item/more-nesting"));
    }

    @Test
    public void doesNotRemovePrefixFromUnmodeledOperations() {
        OpenApiConfig config = new OpenApiConfig();
        config.setRemoveUriPrefix("/v1");
        config.setService(ShapeId.from("smithy.example#Example"));
        // Create a place holder operation to prove these aren't affected.
        config.setJsonAdd(MapUtils.of("/paths/~1v1~1foo/get", Node.objectNode()));

        Model model = Model.assembler()
                .addImport(RemoveUriPrefixTest.class.getResource("remove-uri-prefix.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        Node result = OpenApiConverter.create().config(config).convertToNode(model);

        Set<String> paths = result.expectObjectNode().expectObjectMember("paths").getStringMap().keySet();
        assertThat(paths, hasItem("/v1/foo"));
        assertThat(paths, hasItem("/item/more-nesting"));
    }
}
