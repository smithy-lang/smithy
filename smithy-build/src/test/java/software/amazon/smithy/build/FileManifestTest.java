/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;

public class FileManifestTest {
    private Path outputDirectory;

    @BeforeEach
    public void before() throws IOException {
        outputDirectory = Files.createTempDirectory(getClass().getName());
    }

    @AfterEach
    public void after() throws IOException {
        Files.walk(outputDirectory).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

    @Test
    public void mergesManifests() {
        FileManifest a = FileManifest.create(outputDirectory);
        a.addFile(outputDirectory.resolve("a"));
        FileManifest b = FileManifest.create(outputDirectory);
        b.addFile(outputDirectory.resolve("b"));
        FileManifest c = FileManifest.create(outputDirectory);
        c.addAllFiles(a);
        c.addAllFiles(b);

        assertThat(c.getFiles(), contains(outputDirectory.resolve("a"), outputDirectory.resolve("b")));
    }

    @Test
    public void pathsMustBeWithinBaseDir() {
        Exception thrown = Assertions.assertThrows(SmithyBuildException.class, () -> {
            FileManifest a = FileManifest.create(outputDirectory);
            a.addFile(Paths.get("/not/within/parent"));
        });

        assertThat(thrown.getMessage(), containsString("must be relative to the base directory"));
    }

    @Test
    public void mergesRelativeWithBasePath() throws IOException {
        FileManifest a = FileManifest.create(outputDirectory);
        a.writeFile("foo/file.txt", "The contents");

        assertThat(Files.isDirectory(outputDirectory.resolve("foo")), is(true));
        assertThat(Files.isRegularFile(outputDirectory.resolve("foo/file.txt")), is(true));
        assertThat(new String(Files.readAllBytes(outputDirectory.resolve("foo/file.txt"))), equalTo("The contents"));
    }

    @Test
    public void writesJsonFiles() throws IOException {
        FileManifest a = FileManifest.create(outputDirectory);
        a.writeJson("foo/file.json", Node.objectNode());

        assertThat(Files.isDirectory(outputDirectory.resolve("foo")), is(true));
        assertThat(Files.isRegularFile(outputDirectory.resolve("foo/file.json")), is(true));
        assertThat(new String(Files.readAllBytes(outputDirectory.resolve("foo/file.json"))), equalTo("{}\n"));
    }

    @Test
    public void writesFromInputStream() throws IOException {
        FileManifest a = FileManifest.create(outputDirectory);
        a.writeFile("foo/file.txt", new ByteArrayInputStream("The contents".getBytes()));

        assertThat(Files.isDirectory(outputDirectory.resolve("foo")), is(true));
        assertThat(Files.isRegularFile(outputDirectory.resolve("foo/file.txt")), is(true));
        assertThat(new String(Files.readAllBytes(outputDirectory.resolve("foo/file.txt"))), equalTo("The contents"));
    }

    @Test
    public void writesFromReader() throws IOException {
        FileManifest a = FileManifest.create(outputDirectory);
        a.writeFile("foo/file.txt", new StringReader("The contents"));

        assertThat(Files.isDirectory(outputDirectory.resolve("foo")), is(true));
        assertThat(Files.isRegularFile(outputDirectory.resolve("foo/file.txt")), is(true));
        assertThat(new String(Files.readAllBytes(outputDirectory.resolve("foo/file.txt"))), equalTo("The contents"));
    }

    @Test
    public void writesClassResources() {
        FileManifest a = FileManifest.create(outputDirectory);
        a.writeFile("test.txt", getClass(), "simple-config.json");

        assertThat(Files.isRegularFile(outputDirectory.resolve("test.txt")), is(true));
    }
}
