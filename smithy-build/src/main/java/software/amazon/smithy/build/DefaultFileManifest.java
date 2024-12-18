/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @see FileManifest#create
 */
final class DefaultFileManifest implements FileManifest {
    private final Set<Path> files = new ConcurrentSkipListSet<>(Comparator.comparing(Path::toString));
    private final Path baseDir;

    DefaultFileManifest(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public Path getBaseDir() {
        return baseDir;
    }

    @Override
    public Set<Path> getFiles() {
        return new LinkedHashSet<>(files);
    }

    @Override
    public Path addFile(Path path) {
        Objects.requireNonNull(path);
        if (!path.startsWith(baseDir) || !path.isAbsolute()) {
            path = resolvePath(path);
        }

        Path parent = path.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new SmithyBuildException(String.format(
                        "Error create directory `%s`: %s",
                        parent,
                        e.getMessage()));
            }
        }

        files.add(path);
        return path;
    }

    @Override
    public Path writeFile(Path path, Reader fileContentsReader) {
        path = addFile(path);

        try (BufferedReader bufferedReader = new BufferedReader(fileContentsReader);
                BufferedWriter writer = Files.newBufferedWriter(path)) {
            int len;
            char[] buffer = new char[4096];
            while ((len = bufferedReader.read(buffer)) != -1) {
                writer.write(buffer, 0, len);
            }
            return path;
        } catch (IOException e) {
            throw new SmithyBuildException("Unable to write contents of file `" + path + "`: " + e.getMessage(), e);
        }
    }

    @Override
    public Path writeFile(Path path, InputStream fileContentsInputStream) {
        path = addFile(path);

        try {
            Files.copy(fileContentsInputStream, path, StandardCopyOption.REPLACE_EXISTING);
            return path;
        } catch (IOException e) {
            throw new SmithyBuildException("Unable to write contents of file `" + path + "`: " + e.getMessage(), e);
        }
    }
}
