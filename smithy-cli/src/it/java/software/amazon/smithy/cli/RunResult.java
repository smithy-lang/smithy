/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.utils.IoUtils;

final class RunResult {
    private final List<String> args;
    private final int exitCode;
    private final String output;
    private final Path root;
    private Path buildDir;

    RunResult(List<String> args, int exitCode, String output, Path root) {
        this.args = args;
        this.exitCode = exitCode;
        this.output = output;
        this.root = root;
    }

    List<String> getArgs() {
        return args;
    }

    int getExitCode() {
        return exitCode;
    }

    String getOutput() {
        return output;
    }

    Path getRoot() {
        return root;
    }

    Path withBuildDir(String... paths) {
        Path result = resolve(root, paths);
        if (!Files.isDirectory(result)) {
            throw new RuntimeException("Smithy build directory does not exist: " + result);
        }
        this.buildDir = result;
        return result;
    }

    Path getBuildDir() {
        if (buildDir == null) {
            buildDir = resolve(root, "build", "smithy");
        }
        return buildDir;
    }

    boolean hasFile(String... paths) {
        return Files.exists(resolve(root, paths));
    }

    String getFile(String... paths) {
        Path resolved = resolve(root, paths);
        if (!Files.isRegularFile(resolved)) {
            throw new IllegalArgumentException("File not found: " + resolved);
        }
        return IoUtils.readUtf8File(resolved);
    }

    boolean hasProjection(String projection) {
        return Files.isDirectory(getBuildDir().resolve(projection));
    }

    boolean hasPlugin(String projection, String plugin) {
        return Files.isDirectory(getArtifactPath(projection, plugin));
    }

    boolean hasArtifact(String projection, String plugin, String... paths) {
        return Files.exists(getArtifactPath(projection, plugin, paths));
    }

    Path getArtifactPath(String projection, String plugin, String... paths) {
        return resolve(getBuildDir().resolve(projection).resolve(plugin), paths);
    }

    String getArtifact(String projection, String plugin, String... paths) {
        return IoUtils.readUtf8File(getArtifactPath(projection, plugin, paths));
    }

    Path resolve(Path result, String... paths) {
        for (String path : paths) {
            result = result.resolve(path);
        }
        return result;
    }

    Set<Path> getFiles() {
        return getFiles(root);
    }

    Set<Path> getFiles(Path inDir) {
        try (Stream<Path> files = Files.find(inDir, 999, (p, a) -> Files.isRegularFile(p))) {
            return files.collect(Collectors.toCollection(TreeSet::new));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Set<Path> getDirectories() {
        try (Stream<Path> files = Files.find(root, 999, (p, a) -> Files.isDirectory(p))) {
            return files.collect(Collectors.toCollection(TreeSet::new));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String toString() {
        return "RunResult{"
                + "args='" + args + '\''
                + ", exitCode=" + exitCode
                + ", output='" + output + '\''
                + ", root=" + root + '}';
    }
}
