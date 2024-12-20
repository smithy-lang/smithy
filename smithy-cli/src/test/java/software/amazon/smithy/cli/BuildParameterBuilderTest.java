/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;

public class BuildParameterBuilderTest {
    @Test
    public void cannotSetTagsUnlessProjecting() {
        Assertions.assertThrows(SmithyBuildException.class, () -> {
            new BuildParameterBuilder()
                    .projectionSourceTags(ListUtils.of("foo", "baz"))
                    .build();
        });
    }

    @Test
    public void skipsMissingConfigFiles() {
        BuildParameterBuilder.Result result = new BuildParameterBuilder()
                .addConfigIfExists("/does/not/exist.json")
                .build();

        assertThat(result.args, not(hasItem("--config")));
    }

    @Test
    public void addsConfigFilesWhenFound() throws URISyntaxException {
        String configFile = Paths.get(getClass().getResource("smithy-build-a.json").toURI()).toString();
        BuildParameterBuilder.Result result = new BuildParameterBuilder()
                .addConfigIfExists(configFile)
                .build();

        assertThat(result.args, hasItem("--config"));
        assertThat(result.args, hasItem(configFile));
    }

    @Test
    public void allowsUnknownTraits() {
        BuildParameterBuilder.Result result = new BuildParameterBuilder()
                .allowUnknownTraits(true)
                .build();

        assertThat(result.args, hasItem("--allow-unknown-traits"));
    }

    @Test
    public void canSetOutput() {
        BuildParameterBuilder.Result result = new BuildParameterBuilder()
                .output("/path/to/foo")
                .build();

        assertThat(result.args, hasItem("--output"));
        assertThat(result.args, hasItem("/path/to/foo"));
    }

    @Test
    public void canSetProjection() {
        BuildParameterBuilder.Result result = new BuildParameterBuilder()
                .projection("testing")
                .build();

        assertThat(result.args, hasItem("--projection"));
        assertThat(result.args, hasItem("testing"));
    }

    @Test
    public void canSetPlugin() {
        BuildParameterBuilder.Result result = new BuildParameterBuilder()
                .plugin("testing")
                .build();

        assertThat(result.args, hasItem("--plugin"));
        assertThat(result.args, hasItem("testing"));
    }

    @Test
    public void canEnableDiscoveryWithNoClasspaths() {
        BuildParameterBuilder.Result result = new BuildParameterBuilder()
                .discover(true)
                .build();

        assertThat(result.args, hasItem("--discover"));
        assertThat(result.args, not(hasItem("--discover-classpath")));
    }

    @Test
    public void canAddExtraArguments() {
        BuildParameterBuilder.Result result = new BuildParameterBuilder()
                .addExtraArgs("--foo", "baz", "--bar")
                .build();

        assertThat(result.args, hasItem("--foo"));
        assertThat(result.args, hasItem("baz"));
        assertThat(result.args, hasItem("--bar"));
    }

    @Test
    public void ignoresNullAndEmptyValues() {
        BuildParameterBuilder.Result result = new BuildParameterBuilder()
                .sources(null)
                .projectionSource(null)
                .projectionSource("")
                .projectionSourceTags((String) null)
                .projectionSourceTags((Collection<String>) null)
                .addConfig(null)
                .addConfig("")
                .addConfigIfExists(null)
                .addConfigIfExists("")
                .buildClasspath(null)
                .buildClasspath("")
                .libClasspath(null)
                .libClasspath("")
                .build();

        assertThat(result.args, contains("build"));
        assertThat(result.classpath.length(), is(0));
        assertThat(result.discoveryClasspath.length(), is(0));
        assertThat(result.sources, empty());
    }

    private static String cp(String... paths) {
        StringJoiner joiner = new StringJoiner(System.getProperty("path.separator"));
        for (String path : paths) {
            joiner.add(path);
        }
        return joiner.toString();
    }

    @Test
    public void sourceBuildUsesCorrectClasspaths() {
        BuildParameterBuilder.Result result = new BuildParameterBuilder()
                .sources(ListUtils.of("foo.smithy", "bar.smithy"))
                // Note that duplicates are removed when necessary.
                .libClasspath(cp("foo.jar", "baz.jar", "bar.jar"))
                .buildClasspath(cp("foo.jar", "/abc/123.jar"))
                .discover(true)
                .build();

        assertThat(result.args,
                contains(
                        "build",
                        "--discover-classpath",
                        cp("foo.jar", "baz.jar", "bar.jar"),
                        "foo.smithy",
                        "bar.smithy"));
        assertThat(result.classpath, equalTo(cp("foo.jar", "baz.jar", "bar.jar", "/abc/123.jar")));
        assertThat(result.discoveryClasspath, equalTo(cp("foo.jar", "baz.jar", "bar.jar")));
        assertThat(result.sources, contains("foo.smithy", "bar.smithy"));
    }

    @Test
    public void skipsSourcesThatDoNotExsit() {
        BuildParameterBuilder.Result result = new BuildParameterBuilder()
                .addSourcesIfExists(ListUtils.of("foo.smithy", "bar.smithy"))
                .build();

        assertThat(result.args, contains("build"));
    }

    @Test
    public void projectionBuildUsesCorrectClasspaths() {
        BuildParameterBuilder.Result result = new BuildParameterBuilder()
                .projectionSource("foo")
                .sources(ListUtils.of("foo.smithy", "bar.smithy"))
                .libClasspath(cp("foo.jar", "baz.jar", "bar.jar"))
                .buildClasspath(cp("foo.jar", "/abc/123.jar"))
                .discover(true)
                .build();

        assertThat(result.args,
                contains(
                        "build",
                        "--discover-classpath",
                        cp("foo.jar", "/abc/123.jar"),
                        "foo.smithy",
                        "bar.smithy"));
        assertThat(result.classpath, equalTo(cp("foo.jar", "/abc/123.jar")));
        assertThat(result.discoveryClasspath, equalTo(cp("foo.jar", "/abc/123.jar")));
        assertThat(result.sources, contains("foo.smithy", "bar.smithy"));
    }

    @Test
    public void projectionBuildSkipsMissingJars() {
        BuildParameterBuilder.Result result = new BuildParameterBuilder()
                .projectionSource("foo")
                .projectionSourceTags("abc, def")
                .libClasspath(cp("foo.jar", "baz.jar", "bar.jar"))
                .buildClasspath(cp("foo.jar", "/abc/123.jar"))
                .discover(true)
                .build();

        assertThat(result.classpath, equalTo(cp("foo.jar", "/abc/123.jar")));
        assertThat(result.discoveryClasspath, equalTo(cp("foo.jar", "/abc/123.jar")));
        assertThat(result.sources, empty());
    }

    @Test
    public void projectionBuildTaggedSourcesRemovedFromModelDiscovery() {
        Set<String> parsedTags = new HashSet<>();

        BuildParameterBuilder.Result result = new BuildParameterBuilder()
                .projectionSource("foo")
                .projectionSourceTags("abc, def")
                .libClasspath(cp("abc.jar"))
                .buildClasspath(cp("foo.jar", "baz.jar", "bar.jar"))
                .discover(true)
                .tagMatcher((cp, tags) -> {
                    parsedTags.addAll(tags);
                    return SetUtils.of("baz.jar");
                })
                .build();

        assertThat(parsedTags, containsInAnyOrder("abc", "def"));
        assertThat(result.classpath, equalTo(cp("foo.jar", "baz.jar", "bar.jar")));
        assertThat(result.discoveryClasspath, equalTo(cp("foo.jar", "bar.jar")));
        assertThat(result.sources, contains("baz.jar"));
    }

    @Test
    public void findsProjectionJarsWithSourceTags() throws URISyntaxException {
        String a = Paths.get(getClass().getResource("jars/a/a.jar").toURI()).toString();
        String b = Paths.get(getClass().getResource("jars/b/b.jar").toURI()).toString();
        String c = Paths.get(getClass().getResource("jars/c/c.jar").toURI()).toString();
        String separator = System.getProperty("path.separator");
        String buildCp = a + separator + b + separator + c;

        BuildParameterBuilder.Result result = new BuildParameterBuilder()
                .projectionSource("foo")
                .projectionSourceTags("X, Blah")
                .libClasspath("abc.jar")
                .buildClasspath(buildCp)
                .discover(true)
                .build();

        // The classpath keeps all of the JARs.
        assertThat(result.classpath, equalTo(buildCp));
        // The discovery classpath removes a because it's JAR matched the source tag.
        assertThat(result.discoveryClasspath, equalTo(b + separator + c));
        // The sources now contains a because it matched a source tag.
        assertThat(result.sources, contains(a));
    }

    @Test
    public void usesCustomSeparator() throws URISyntaxException {
        String currentSeparator = System.getProperty("path.separator");

        try {
            System.setProperty("path.separator", "|");
            String a = Paths.get(getClass().getResource("jars/a/a.jar").toURI()).toString();
            String b = Paths.get(getClass().getResource("jars/b/b.jar").toURI()).toString();
            String c = Paths.get(getClass().getResource("jars/c/c.jar").toURI()).toString();
            String buildCp = a + "|" + b + "|" + c;

            BuildParameterBuilder.Result result = new BuildParameterBuilder()
                    .projectionSource("foo")
                    .projectionSourceTags("X, Blah")
                    .libClasspath("abc.jar")
                    .buildClasspath(buildCp)
                    .discover(true)
                    .build();

            assertThat(result.classpath, equalTo(buildCp));
            assertThat(result.discoveryClasspath, equalTo(b + "|" + c));
            assertThat(result.sources, contains(a));
        } finally {
            System.setProperty("path.separator", currentSeparator);
        }
    }
}
