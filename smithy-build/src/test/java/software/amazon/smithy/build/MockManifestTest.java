/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MockManifestTest {
    @Test
    public void pathsMustBeWithinBaseDir() {
        Exception thrown = Assertions.assertThrows(SmithyBuildException.class, () -> {
            FileManifest a = new MockManifest(Paths.get("/foo"));
            a.addFile(Paths.get("/not/within/parent"));
        });

        assertThat(thrown.getMessage(), containsString("must be relative to the base directory"));
    }

    @Test
    public void mergesRelativeWithBasePath() {
        MockManifest a = new MockManifest();
        a.writeFile("foo/file.txt", "The contents");

        assertTrue(a.hasFile("foo/file.txt"));
        assertThat(a.getFiles(), hasSize(1));
    }

    @Test
    public void writesFromInputStream() {
        MockManifest a = new MockManifest();
        a.writeFile("foo/file.txt", new ByteArrayInputStream("The contents".getBytes()));

        assertThat(a.expectFileBytes("foo/file.txt"), equalTo("The contents".getBytes()));
    }

    @Test
    public void writesFromReader() {
        MockManifest a = new MockManifest();
        a.writeFile("foo/file.txt", new StringReader("The contents"));

        assertThat(a.expectFileString("foo/file.txt"), equalTo("The contents"));
    }

    @Test
    public void findsFilesUnderPrefix() {
        MockManifest a = new MockManifest();
        a.writeFile("foo/bar/a", "hello");
        a.writeFile("foo/bar/b", "hello");
        a.writeFile("foo/bam/qux", "hello");

        assertThat(a.getFilesIn("foo"),
                contains(
                        Paths.get("/foo/bam/qux"),
                        Paths.get("/foo/bar/a"),
                        Paths.get("/foo/bar/b")));
        assertThat(a.getFilesIn("foo/bam"), contains(Paths.get("/foo/bam/qux")));
        assertThat(a.getFilesIn("foo/bar"), contains(Paths.get("/foo/bar/a"), Paths.get("/foo/bar/b")));
        assertThat(a.getFilesIn("foo/bam"), contains(Paths.get("/foo/bam/qux")));
    }

    @Test
    public void canStoreOnlyPaths() {
        MockManifest a = new MockManifest(Paths.get("/foo"), MockManifest.StoreMode.PATH_ONLY);
        a.writeFile("foo/bar/a", "hello");

        assertThat(a.getFiles(), contains(Paths.get("/foo/foo/bar/a")));
        assertThat(a.expectFileString("foo/bar/a"), equalTo(""));
        assertTrue(a.expectFileBytes("foo/bar/a").length == 0);
    }
}
