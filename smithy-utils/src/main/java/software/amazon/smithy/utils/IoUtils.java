/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for IO operations.
 */
public final class IoUtils {

    private static final Logger LOGGER = Logger.getLogger(IoUtils.class.getName());
    private static final int BUFFER_SIZE = 1024 * 8;

    private IoUtils() {}

    /**
     * Reads and returns the rest of the given input stream as a byte array.
     * Caller is responsible for closing the given input stream.
     *
     * @param is The input stream to convert.
     * @return The converted bytes.
     */
    public static byte[] toByteArray(InputStream is) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] b = new byte[BUFFER_SIZE];
            int n;
            while ((n = is.read(b)) != -1) {
                output.write(b, 0, n);
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads and returns the rest of the given input stream as a string.
     * Caller is responsible for closing the given input stream.
     *
     * @param is The input stream to convert.
     * @return The converted string.
     */
    public static String toUtf8String(InputStream is) {
        return new String(toByteArray(is), StandardCharsets.UTF_8);
    }

    /**
     * Reads a file into a UTF-8 encoded string.
     *
     * @param path Path to the file to read.
     * @return Returns the contents of the file.
     * @throws RuntimeException if the file can't be read or encoded.
     */
    public static String readUtf8File(String path) {
        return readUtf8File(Paths.get(path));
    }

    /**
     * Reads a file into a UTF-8 encoded string.
     *
     * @param path Path to the file to read.
     * @return Returns the contents of the file.
     * @throws RuntimeException if the file can't be read or encoded.
     */
    public static String readUtf8File(Path path) {
        try {
            return new String(Files.readAllBytes(path.toRealPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads a class loader resource into a UTF-8 string.
     *
     * <p>This is equivalent to reading the contents of an {@link InputStream}
     * from {@link ClassLoader#getResourceAsStream}.
     *
     * @param classLoader Class loader to load from.
     * @param resourcePath Path to the resource to load.
     * @return Returns the loaded resource.
     * @throws UncheckedIOException if the resource cannot be loaded.
     */
    public static String readUtf8Resource(ClassLoader classLoader, String resourcePath) {
        return readUtf8Url(classLoader.getResource(resourcePath));
    }

    /**
     * Reads a class resource into a UTF-8 string.
     *
     * <p>This is equivalent to reading the contents of an {@link InputStream}
     * from {@link Class#getResourceAsStream(String)}.
     *
     * @param clazz Class to load from.
     * @param resourcePath Path to the resource to load.
     * @return Returns the loaded resource.
     * @throws UncheckedIOException if the resource cannot be loaded.
     */
    public static String readUtf8Resource(Class<?> clazz, String resourcePath) {
        return readUtf8Url(clazz.getResource(resourcePath));
    }

    /**
     * Reads a URL resource into a UTF-8 string.
     *
     * @param url URL to load from.
     * @return Returns the loaded resource.
     * @throws UncheckedIOException if the resource cannot be loaded.
     */
    public static String readUtf8Url(URL url) {
        try (InputStream is = url.openStream()) {
            return toUtf8String(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Helper to run a process.
     *
     * <p>stderr is redirected to stdout in the return value of this method.
     *
     * @param command String that contains the command and arguments of the command.
     * @return        Returns the combined stdout and stderr of the process.
     * @throws RuntimeException if the process returns a non-zero exit code or fails.
     */
    public static String runCommand(String command) {
        return runCommand(command, Paths.get(System.getProperty("user.dir")));
    }

    /**
     * Helper to run a process.
     *
     * <p>stderr is redirected to stdout in the return value of this method.
     *
     * @param command   String that contains the command and arguments of the command.
     * @param directory Directory to use as the working directory.
     * @return          Returns the combined stdout and stderr of the process.
     * @throws RuntimeException if the process returns a non-zero exit code or fails.
     */
    public static String runCommand(String command, Path directory) {
        StringBuilder sb = new StringBuilder();
        int exitValue = runCommand(command, directory, sb);

        if (exitValue != 0) {
            throw new RuntimeException(String.format(
                    "Command `%s` failed with exit code %d and output:%n%n%s", command, exitValue, sb));
        }

        return sb.toString();
    }

    /**
     * Helper to run a process.
     *
     * <p>stderr is redirected to stdout when writing to {@code output}.
     * This method <em>does not</em> throw when a non-zero exit code is
     * encountered. For any more complex use cases, use {@link ProcessBuilder}
     * directly.
     *
     * @param command   String that contains the command and arguments of the command.
     * @param directory Directory to use as the working directory.
     * @param output    Sink destination for lines from stdout and stderr of the process.
     * @return          Returns the exit code of the process.
     */
    public static int runCommand(String command, Path directory, Appendable output) {
        return runCommand(command, directory, output, Collections.emptyMap());
    }

    /**
     * Helper to run a process.
     *
     * <p>stderr is redirected to stdout when writing to {@code output}.
     * This method <em>does not</em> throw when a non-zero exit code is
     * encountered. For any more complex use cases, use {@link ProcessBuilder}
     * directly.
     *
     * @param command   String that contains the command and arguments of the command.
     * @param directory Directory to use as the working directory.
     * @param output    Sink destination for lines from stdout and stderr of the process.
     * @param env       Environment variables to set, possibly overriding specific inherited variables.
     * @return          Returns the exit code of the process.
     */
    public static int runCommand(String command, Path directory, Appendable output, Map<String, String> env) {
        List<String> finalizedCommand;
        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).startsWith("windows")) {
            finalizedCommand = Arrays.asList("cmd.exe", "/c", command);
        } else {
            finalizedCommand = Arrays.asList("sh", "-c", command);
        }
        return runCommand(finalizedCommand, directory, output, env);
    }

    /**
     * Helper to run a process.
     *
     * <p>stderr is redirected to stdout when writing to {@code output}.
     * This method <em>does not</em> throw when a non-zero exit code is
     * encountered. For any more complex use cases, use {@link ProcessBuilder}
     * directly.
     *
     * @param args      List containing the program and the arguments.
     * @param directory Directory to use as the working directory.
     * @param output    Sink destination for lines from stdout and stderr of the process.
     * @param env       Environment variables to set, possibly overriding specific inherited variables.
     * @return          Returns the exit code of the process.
     */
    public static int runCommand(List<String> args, Path directory, Appendable output, Map<String, String> env) {
        return runCommand(args, directory, null, output, env);
    }

    /**
     * Helper to run a process.
     *
     * <p>stderr is redirected to stdout when writing to {@code output} This method <em>does not</em> throw when a
     * non-zero exit code is encountered. For any more complex use cases, use {@link ProcessBuilder} directly.
     *
     * @param args      List containing the program and the arguments.
     * @param directory Directory to use as the working directory.
     * @param input     Data to write to the stdin of the process. Use {@code null} to send no input.
     * @param output    Sink destination for lines from stdout and stderr of the process.
     * @param env       Environment variables to set, possibly overriding specific inherited variables.
     * @return          Returns the exit code of the process.
     */
    public static int runCommand(
            List<String> args,
            Path directory,
            InputStream input,
            Appendable output,
            Map<String, String> env
    ) {
        ProcessBuilder processBuilder = new ProcessBuilder(args)
                .directory(Objects.requireNonNull(directory.toFile(), "Process directory cannot be null"))
                .redirectErrorStream(true);
        processBuilder.environment().putAll(env);

        try {
            Process process = processBuilder.start();

            if (input != null) {
                OutputStream outputStream = process.getOutputStream();
                copyInputToOutput(input, outputStream);
                quietlyCloseStream(args, outputStream);
            }

            try (BufferedReader bufferedStdoutReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = bufferedStdoutReader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            return process.waitFor();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Can be replaced with InputStream#transferTo in Java 9+.
    private static void copyInputToOutput(InputStream source, OutputStream sink) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = source.read(buffer, 0, BUFFER_SIZE)) >= 0) {
            sink.write(buffer, 0, read);
        }
    }

    private static void quietlyCloseStream(List<String> args, OutputStream outputStream) {
        try {
            outputStream.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to close outputStream for " + args, e);
        }
    }

    /**
     * Delete a directory and all files within.
     *
     * <p>Any found symlink is deleted, but the contents of a symlink are not deleted.
     *
     * @param dir Directory to delete.
     * @return Returns true if the directory was deleted, or false if the directory does not exist.
     * @throws IllegalArgumentException if the given path is not a directory.
     * @throws RuntimeException if unable to delete a file or directory.
     */
    public static boolean rmdir(Path dir) {
        if (!Files.exists(dir)) {
            return false;
        }

        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException(dir + " is not a directory");
        }

        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return Files.isSymbolicLink(dir)
                           // Don't delete symlink files, just delete the symlink.
                           ? FileVisitResult.SKIP_SUBTREE
                           : FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                    if (e != null) {
                        throw e;
                    }
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Error deleting directory: " + dir + ": " + e.getMessage(), e);
        }

        return true;
    }
}
