/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.plugins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.model.ProjectionConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.utils.ListUtils;

public class BuildInfoPluginTest {

    @Test
    public void testFiltersSyntheticEnumTrait() throws URISyntaxException {

        String testPath = "build-info/example.smithy";

        Model model = Model.assembler()
                .addImport(getClass().getResource(testPath))
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        PluginContext context = PluginContext.builder()
                .fileManifest(manifest)
                .model(model)
                .originalModel(model)
                .projection("TEST", ProjectionConfig.builder().build())
                .sources(ListUtils.of(Paths.get(getClass().getResource(testPath).toURI()).getParent()))
                .build();
        new BuildInfoPlugin().execute(context);

        String buildInfo = manifest.getFileString(BuildInfoPlugin.BUILD_INFO_PATH).get();

        assertThat(buildInfo, not(containsString("smithy.synthetic#enum")));
    }

}
