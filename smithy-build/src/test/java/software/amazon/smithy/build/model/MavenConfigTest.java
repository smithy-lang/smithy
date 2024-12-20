/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;

public class MavenConfigTest {
    @Test
    public void hasNoDefaultsBuiltInToThePojo() {
        MavenConfig config = MavenConfig.builder().build();

        assertThat(config.getRepositories(), empty());
    }

    @Test
    public void loadsEmptyConfig() {
        MavenConfig config = MavenConfig.fromNode(Node.objectNode());

        assertThat(config.getDependencies(), empty());
        assertThat(config.getRepositories(), empty());
    }

    @Test
    public void loadsFromNodeAndOverridesMavenCentral() {
        MavenConfig config = MavenConfig.fromNode(Node.objectNodeBuilder()
                .withMember("dependencies", Node.fromStrings("g:a:v"))
                .withMember("repositories", Node.fromNodes(Node.objectNode().withMember("url", "https://example.com")))
                .build());

        assertThat(config.getDependencies(), contains("g:a:v"));
        assertThat(config.getRepositories(), hasSize(1));
        assertThat(config.getRepositories().iterator().next().getUrl(), equalTo("https://example.com"));
    }

    @Test
    public void convertToBuilder() {
        MavenConfig config1 = MavenConfig.fromNode(Node.objectNodeBuilder()
                .withMember("dependencies", Node.fromStrings("g:a:v"))
                .withMember("repositories", Node.fromNodes(Node.objectNode().withMember("url", "https://example.com")))
                .build());
        MavenConfig config2 = config1.toBuilder().build();

        assertThat(config1, equalTo(config1));
        assertThat(config1, equalTo(config2));
    }

    @Test
    public void mergesConfigs() {
        MavenConfig config1 = MavenConfig.fromNode(Node.objectNodeBuilder()
                .withMember("dependencies", Node.fromStrings("g:a:v"))
                .withMember("repositories", Node.fromNodes(Node.objectNode().withMember("url", "https://example.com")))
                .build());
        MavenConfig config2 = MavenConfig.fromNode(Node.objectNodeBuilder()
                .withMember("dependencies", Node.fromStrings("g:a:v", "a:a:a"))
                .withMember("repositories",
                        Node.fromNodes(
                                Node.objectNode().withMember("url", "https://example.com"),
                                Node.objectNode().withMember("url", "https://m2.example.com")))
                .build());
        MavenConfig merged = config1.merge(config2);

        List<MavenRepository> repos = new ArrayList<>(config1.getRepositories());
        repos.add(MavenRepository.builder().url("https://m2.example.com").build());

        List<String> dependencies = new ArrayList<>();
        dependencies.add("g:a:v");
        dependencies.add("a:a:a");

        MavenConfig expectedMerge = MavenConfig.builder()
                .repositories(repos)
                .dependencies(dependencies)
                .build();

        assertThat(merged, equalTo(expectedMerge));
    }
}
