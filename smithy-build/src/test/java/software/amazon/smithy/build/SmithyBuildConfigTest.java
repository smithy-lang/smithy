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

package software.amazon.smithy.build;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;

public class SmithyBuildConfigTest {
    @Test
    public void loadsFromJson() throws Exception {
        SmithyBuildConfig.load(Paths.get(getClass().getResource("special-syntax.json").toURI()));
    }

    @Test
    public void throwsWithCorrectSyntaxErrorMessage() throws Exception {
        Exception thrown = Assertions.assertThrows(SmithyBuildException.class, () -> {
            SmithyBuildConfig.load(Paths.get(getClass().getResource("bad-syntax.json").toURI()));
        });

        assertThat(thrown.getMessage(), containsString("Error parsing file"));
    }

    @Test
    public void requiresVersion() throws Exception {
        Exception thrown = Assertions.assertThrows(SourceException.class, () -> {
            SmithyBuildConfig.load(Paths.get(getClass().getResource("missing-version.json").toURI()));
        });

        assertThat(thrown.getMessage(), containsString("version"));
    }

    @Test
    public void canBeAbstract() throws Exception {
        SmithyBuildConfig config = SmithyBuildConfig.load(
                Paths.get(getClass().getResource("config-with-abstract.json").toURI()));

        assertThat(config.getProjection("abstract").get().isAbstract(), is(true));
    }

    @Test
    public void canLoadJsonDataIntoBuilder() throws Exception {
        Path outputDir = Paths.get("/foo/baz");
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("config-with-abstract.json").toURI()))
                .load(Paths.get(getClass().getResource("simple-config.json").toURI()))
                .outputDirectory(outputDir.toString())
                .build();

        assertThat(config.getProjection("a").get(), notNullValue());
        assertThat(config.getProjection("b").get(), notNullValue());
        assertThat(config.getProjection("abstract").get().isAbstract(), is(true));
        assertThat(config.getOutputDirectory(), equalTo(Optional.of(outputDir)));
    }

    @Test
    public void canAddImports() throws Exception {
        String importPath = Paths.get(getClass().getResource("simple-model.json").toURI()).toString();
        SmithyBuildConfig config = SmithyBuildConfig.builder().addImport(importPath).build();

        assertThat(config.getImports(), containsInAnyOrder(importPath));
    }

    @Test
    public void canAddImportsAndOutputDirViaJson() throws Exception {
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("config-with-imports-and-output-dir.json").toURI()))
                .build();

        assertThat(config.getOutputDirectory(), not(Optional.empty()));
        assertThat(config.getImports(), not(empty()));
    }

    @Test
    public void pluginsMustHaveValidNames() {
        Assertions.assertThrows(SmithyBuildException.class, () -> {
            SmithyBuildConfig.builder().addPlugin("!invalid", Node.objectNode()).build();
        });
    }
}
