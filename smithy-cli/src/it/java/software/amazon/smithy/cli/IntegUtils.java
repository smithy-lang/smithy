/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.MapUtils;

public final class IntegUtils {

    private static final Logger LOGGER = Logger.getLogger(IntegUtils.class.getName());

    private IntegUtils() {}

    public static void withProject(String projectName, Consumer<Path> consumer) {
        withTempDir(projectName, path -> {
            copyProject(projectName, path);
            consumer.accept(path);
        });
    }

    public static void run(String projectName, List<String> args, Consumer<RunResult> consumer) {
        run(projectName, args, MapUtils.of(EnvironmentVariable.NO_COLOR.toString(), "true"), consumer);
    }

    public static void run(String projectName, List<String> args, Map<String, String> env,
            Consumer<RunResult> consumer) {
        withProject(projectName, path -> consumer.accept(run(path, args, env)));
    }

    public static void runWithEmptyCache(String projectName, List<String> args, Map<String, String> env,
            Consumer<RunResult> consumer) {
        try {
            String cacheDir = Files.createTempDirectory("foo").toString();
            Map<String, String> actualEnv = new HashMap<>(env);
            actualEnv.put(EnvironmentVariable.SMITHY_MAVEN_CACHE.toString(), cacheDir);
            withProject(projectName, path -> consumer.accept(run(path, args, actualEnv)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static RunResult run(Path root, List<String> args) {
        return run(root, args, Collections.emptyMap());
    }

    public static RunResult run(Path root, List<String> args, Map<String, String> env) {
        List<String> smithyCommand = createSmithyCommand(args);
        StringBuilder output = new StringBuilder();
        int exitCode = IoUtils.runCommand(smithyCommand, root, output, env);
        return new RunResult(smithyCommand, exitCode, output.toString(), root);
    }

    private static List<String> createSmithyCommand(List<String> args) {
        String smithyBinary = System.getProperty("SMITHY_BINARY");
        if (smithyBinary != null) {
            List<String> result = new ArrayList<>(args.size() + 1);
            result.add(smithyBinary);
            result.addAll(args);
            return result;
        }

        throw new RuntimeException("No SMITHY_BINARY location was set. Did you build the Smithy jlink CLI?");
    }

    static void withTempDir(String name, Consumer<Path> consumer) {
        try {
            Path path = Files.createTempDirectory(name.replace("/", "_"));
            try {
                consumer.accept(path);
            } finally {
                try {
                    IoUtils.rmdir(path);
                } catch (Exception e) {
                    LOGGER.log(Level.INFO, "Unable to delete temp directory", e);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void copyProject(String name, Path dest) {
        try {
            URL url = IntegUtils.class.getResource("projects/" + name);
            if (url == null) {
                throw new IllegalArgumentException("Invalid project name: " + name);
            }
            Path source = Paths.get(url.toURI());
            copyDirectory(source, dest);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static void copyDirectory(Path from, Path to) throws IOException {
        Files.walkFileTree(from, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // Create parent directories if they don't exist.
                Path targetDir = to.resolve(from.relativize(dir));
                try {
                    Files.copy(dir, targetDir);
                } catch (FileAlreadyExistsException e) {
                    // do nothing
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, to.resolve(from.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
