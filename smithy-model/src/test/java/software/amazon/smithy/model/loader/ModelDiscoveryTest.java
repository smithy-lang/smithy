/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ModelDiscoveryTest {
    @Test
    public void discoversModelsInManifests() throws MalformedURLException {
        URL manifest = getClass().getResource("manifest-valid");
        String prefix = manifest.toString().substring(0, manifest.toString().length() - "manifest".length());
        List<URL> models = ModelDiscovery.findModels(manifest);

        assertThat(models,
                contains(
                        new URL(prefix + "foo.smithy"),
                        new URL(prefix + "baz/bar/example.json"),
                        new URL(prefix + "test"),
                        new URL(prefix + "test2")));
    }

    @Test
    public void discoversModelsFromClasspath() {
        URL[] urls = new URL[] {getClass().getResource("jar-import.jar")};
        URLClassLoader classLoader = new URLClassLoader(urls);
        List<URL> models = ModelDiscovery.findModels(classLoader);
        List<String> names = models.stream()
                .map(ModelDiscovery::getSmithyModelPathFromJarUrl)
                .collect(Collectors.toList());

        assertThat(names, contains("a.smithy", "b/b.smithy", "b/c/c.json"));
    }

    @Test
    public void parsesEmptyManifest() {
        URL manifest = getClass().getResource("manifest-empty");
        List<URL> models = ModelDiscovery.findModels(manifest);

        assertThat(models, empty());
    }

    @Test
    public void skipsCommentLines() throws IOException {
        URL manifest = getClass().getResource("manifest-valid-with-comments");
        String prefix = manifest.toString().substring(0, manifest.toString().length() - "manifest".length());
        List<URL> models = ModelDiscovery.findModels(manifest);

        assertThat(models,
                contains(
                        new URL(prefix + "foo.smithy"),
                        new URL(prefix + "baz/bar/example.json"),
                        new URL(prefix + "test"),
                        new URL(prefix + "test2")));
    }

    @Test
    public void prohibitsLeadingSlash() {
        Assertions.assertThrows(ModelManifestException.class, () -> {
            URL manifest = getClass().getResource("manifest-prohibits-leading-slash");
            ModelDiscovery.findModels(manifest);
        });
    }

    @Test
    public void prohibitsTrailingSlash() {
        Assertions.assertThrows(ModelManifestException.class, () -> {
            URL manifest = getClass().getResource("manifest-prohibits-trailing-slash");
            ModelDiscovery.findModels(manifest);
        });
    }

    @Test
    public void prohibitsEmptySegments() {
        Assertions.assertThrows(ModelManifestException.class, () -> {
            URL manifest = getClass().getResource("manifest-prohibits-empty-segments");
            ModelDiscovery.findModels(manifest);
        });
    }

    @Test
    public void prohibitsDotSegments() {
        Assertions.assertThrows(ModelManifestException.class, () -> {
            URL manifest = getClass().getResource("manifest-prohibits-dot-segments");
            ModelDiscovery.findModels(manifest);
        });
    }

    @Test
    public void prohibitsDotDotSegments() {
        Assertions.assertThrows(ModelManifestException.class, () -> {
            URL manifest = getClass().getResource("manifest-prohibits-dot-dot-segments");
            ModelDiscovery.findModels(manifest);
        });
    }

    @Test
    public void prohibitsSpecialCharacters() {
        Assertions.assertThrows(ModelManifestException.class, () -> {
            URL manifest = getClass().getResource("manifest-prohibits-special-characters");
            ModelDiscovery.findModels(manifest);
        });
    }

    @Test
    public void extractsOutModelNameFromJarURL() throws IOException {
        assertThat(ModelDiscovery.getSmithyModelPathFromJarUrl(new URL("jar:file:/a.jar!/META-INF/smithy/a/b.json")),
                equalTo("a/b.json"));
        assertThat(ModelDiscovery.getSmithyModelPathFromJarUrl(new URL("jar:file:/a.jar!/META-INF/smithy/b.json")),
                equalTo("b.json"));
    }

    @Test
    public void requiresModelNameToBeValidWhenParsing() throws IOException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ModelDiscovery.getSmithyModelPathFromJarUrl(new URL("jar:file:/a.jar!/blerg.json"));
        });
    }

    @Test
    public void createSmithyManifestUrlFromPath() throws IOException {
        assertThat(ModelDiscovery.createSmithyJarManifestUrl("/foo.jar"),
                equalTo(new URL("jar:file:/foo.jar!/META-INF/smithy/manifest")));
    }

    @Test
    public void createSmithyManifestUrlFromFileUrl() throws IOException {
        assertThat(ModelDiscovery.createSmithyJarManifestUrl("file:/foo.jar"),
                equalTo(new URL("jar:file:/foo.jar!/META-INF/smithy/manifest")));
    }

    @Test
    public void createSmithyManifestUrlFromJarUrl() throws IOException {
        assertThat(ModelDiscovery.createSmithyJarManifestUrl("jar:file:/foo.jar"),
                equalTo(new URL("jar:file:/foo.jar!/META-INF/smithy/manifest")));
    }
}
