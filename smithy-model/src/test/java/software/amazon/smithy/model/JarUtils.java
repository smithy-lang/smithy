/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class JarUtils {
    /**
     * Creates a JAR in a temp directory on demand for test cases based on a directory.
     *
     * <p>This method is preferred over embedding JARs directly as resources when possible, because generated JARs
     * don't need to be manually recreated if their contents need to change, and we don't need to commit blobs to VCS.
     *
     * <p>TODO: migrate other test cases to use this.
     *
     * @param source Where the files for the JAR are stored, including the required "META-INF/MANIFEST.MF" file.
     * @return Returns the path to the temporary JAR file.
     */
    public static Path createJarFromDir(Path source) {
        try {
            Path target = Files.createTempFile("temp-jar", ".jar");

            Path relativeManifestLocation = Paths.get("META-INF").resolve("MANIFEST.MF");
            Manifest manifest;

            // Requires a manifest to be provided.
            Path manifestLocation = target.resolve(relativeManifestLocation);
            if (Files.isRegularFile(manifestLocation)) {
                manifest = new Manifest(Files.newInputStream(manifestLocation));
            } else {
                manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            }

            try (JarOutputStream stream = new JarOutputStream(Files.newOutputStream(target), manifest)) {
                Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path relative = source.relativize(file);
                        // The manifest is added through the constructor.
                        if (!relative.equals(relativeManifestLocation)) {
                            JarEntry entry = new JarEntry(relative.toString().replace("\\", "/"));
                            entry.setTime(file.toFile().lastModified());
                            stream.putNextEntry(entry);
                            Files.copy(file, stream);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
