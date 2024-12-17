/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;

@Isolated
public class CleanCommandTest {
    private static final String PROJECT_NAME = "simple-config-sources";
    @Test
    public void exitNormallyIfBuildDirMissing() {
        IntegUtils.run(PROJECT_NAME, ListUtils.of("clean"), result -> {
            assertThat(result.getExitCode(), equalTo(0));
            assertThat(result.getOutput(), hasLength(0));
        });
    }

    @Test
    public void deletesContentsOfBuildDir() {
        IntegUtils.withProject(PROJECT_NAME, root -> {
            try {
                Path created = Files.createDirectories(root.resolve("build").resolve("smithy").resolve("foo"));
                assertThat(Files.exists(created), is(true));
                RunResult result = IntegUtils.run(root, ListUtils.of("clean"));
                assertThat(Files.exists(created), is(false));
                assertThat(result.getExitCode(), is(0));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Test
    public void cleanRemovesAllCacheDirectories() throws IOException {
        IntegUtils.clearCacheDirIfExists();
        try {
            Files.createDirectories(IntegUtils.SMITHY_TEMPLATE_CACHE_PATH);
            assertTrue(Files.exists(IntegUtils.SMITHY_TEMPLATE_CACHE_PATH)
                    && Files.isDirectory(IntegUtils.SMITHY_ROOT_CACHE_PATH));
            IntegUtils.run(PROJECT_NAME, ListUtils.of("clean"), result -> {
                assertThat(result.getExitCode(), equalTo(0));
                assertThat(result.getOutput(), hasLength(0));
                assertFalse(Files.exists(IntegUtils.SMITHY_ROOT_CACHE_PATH));
            });
        } finally {
            IntegUtils.clearCacheDirIfExists();
        }
    }

    @Test
    public void cleanWithTemplateOptionRemovesOnlyTemplateDir() throws IOException {
        IntegUtils.clearCacheDirIfExists();
        try {
            Files.createDirectories(IntegUtils.SMITHY_TEMPLATE_CACHE_PATH);
            assertTrue(Files.exists(IntegUtils.SMITHY_TEMPLATE_CACHE_PATH)
                    && Files.isDirectory(IntegUtils.SMITHY_ROOT_CACHE_PATH));

            IntegUtils.withProject(PROJECT_NAME, root -> {
                Path created = null;
                try {
                    created = Files.createDirectories(root.resolve("build").resolve("smithy").resolve("foo"));
                    assertThat(Files.exists(created), is(true));
                    RunResult result = IntegUtils.run(root, ListUtils.of("clean", "--templates"));
                    assertThat(Files.exists(created), is(true));
                    assertThat(result.getExitCode(), is(0));
                    assertTrue(Files.exists(IntegUtils.SMITHY_ROOT_CACHE_PATH));
                    assertFalse(Files.exists(IntegUtils.SMITHY_TEMPLATE_CACHE_PATH));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    if (created != null) {
                        IoUtils.rmdir(created);
                    }
                }
            });
        } finally {
            IntegUtils.clearCacheDirIfExists();
        }
    }
}
