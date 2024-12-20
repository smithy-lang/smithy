/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * A {@link FileManifest} that doesn't actually store files on disk.
 *
 * <p>This manifest is useful for testing SmithyBuildPlugin implementations.
 */
public final class MockManifest implements FileManifest {
    private final Path baseDir;
    private final StoreMode storeMode;
    private final Map<Path, byte[]> files = new ConcurrentSkipListMap<>(Comparator.comparing(Path::toString));

    /**
     * The way in which files are stored.
     */
    public enum StoreMode {
        /** Store all saved files in memory. */
        IN_MEMORY,

        /** Store only the path of each saved file. */
        PATH_ONLY
    }

    /**
     * @param baseDir Base directory of the manifest.
     * @param storeMode How files are stored in the mock.
     */
    public MockManifest(Path baseDir, MockManifest.StoreMode storeMode) {
        this.baseDir = baseDir;
        this.storeMode = storeMode;
    }

    /**
     * Creates a mock manifest that stores files in memory.
     *
     * @param baseDir Base directory of the manifest.
     */
    public MockManifest(Path baseDir) {
        this(baseDir, MockManifest.StoreMode.IN_MEMORY);
    }

    /**
     * Creates a mock manifest that stores files in memory and uses a
     * base directory of "/".
     */
    public MockManifest() {
        this(Paths.get("/"));
    }

    @Override
    public Path getBaseDir() {
        return baseDir;
    }

    @Override
    public Set<Path> getFiles() {
        return new LinkedHashSet<>(files.keySet());
    }

    @Override
    public Path addFile(Path path) {
        if (!path.startsWith(baseDir) || !path.isAbsolute()) {
            path = resolvePath(path);
        }

        files.put(path, new byte[0]);
        return path;
    }

    @Override
    public Path writeFile(Path path, Reader fileContentsReader) {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[8 * 1024];
        int numCharsRead;
        try {
            while ((numCharsRead = fileContentsReader.read(buffer, 0, buffer.length)) != -1) {
                builder.append(buffer, 0, numCharsRead);
            }
        } catch (IOException e) {
            throw new SmithyBuildException(e);
        }

        return storeFile(path, builder.toString().getBytes(Charset.forName("UTF-8")));
    }

    private Path storeFile(Path path, byte[] bytes) {
        path = resolvePath(path);
        if (storeMode == StoreMode.IN_MEMORY) {
            files.put(path, bytes);
        } else {
            files.put(path, new byte[0]);
        }
        return path;
    }

    @Override
    public Path writeFile(Path path, InputStream fileContentsInputStream) {
        try {
            ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8 * 1024];
            int numCharsRead;
            while ((numCharsRead = fileContentsInputStream.read(buffer, 0, buffer.length)) != -1) {
                byteArrayStream.write(buffer, 0, numCharsRead);
            }
            return storeFile(path, byteArrayStream.toByteArray());
        } catch (IOException e) {
            throw new SmithyBuildException("Unable to write contents of file `" + path + "`: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the contents of a stored file as a String.
     *
     * @param file Relative or absolute path to the file to retrieve.
     * @return Returns the optionally found file string.
     * @throws SmithyBuildException if the file cannot be read as UTF-8.
     */
    public Optional<String> getFileString(Path file) {
        return getFileBytes(file).map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }

    /**
     * Gets the contents of a stored file as a String.
     *
     * @param file Relative or absolute path to the file to retrieve.
     * @return Returns the optionally found file string.
     * @throws SmithyBuildException if the file cannot be read as UTF-8.
     */
    public Optional<String> getFileString(String file) {
        return getFileString(Paths.get(file));
    }

    /**
     * Expects that the given file was stored and returns a UTF-8 string.
     *
     * @param file File to retrieve.
     * @return Returns the bytes of the file if found.
     * @throws InvalidPathException if the file cannot be found.
     */
    public String expectFileString(Path file) {
        return getFileString(file).orElseThrow(() -> new InvalidPathException(file.toString(), file + " not found"));
    }

    /**
     * Expects that the given file was stored and returns a UTF-8 string.
     *
     * @param file File to retrieve.
     * @return Returns the bytes of the file if found.
     * @throws InvalidPathException if the file cannot be found.
     */
    public String expectFileString(String file) {
        return expectFileString(Paths.get(file));
    }

    /**
     * Gets the bytes of a stored file.
     *
     * @param file Relative or absolute path to the file to retrieve.
     * @return Returns the optionally found file by.es,
     */
    public Optional<byte[]> getFileBytes(Path file) {
        return Optional.ofNullable(files.get(resolvePath(file)));
    }

    /**
     * Gets the bytes of a stored file.
     *
     * @param file Relative or absolute path to the file to retrieve.
     * @return Returns the optionally found file by.es,
     */
    public Optional<byte[]> getFileBytes(String file) {
        return getFileBytes(Paths.get(file));
    }

    /**
     * Expects that the given file was stored and returns the bytes.
     *
     * @param file File to retrieve.
     * @return Returns the bytes of the file if found.
     * @throws InvalidPathException if the file cannot be found.
     */
    public byte[] expectFileBytes(Path file) {
        return getFileBytes(file).orElseThrow(() -> new InvalidPathException(file.toString(), file + " not found"));
    }

    /**
     * Expects that the given file was stored and returns the bytes.
     *
     * @param file File to retrieve.
     * @return Returns the bytes of the file if found.
     * @throws InvalidPathException if the file cannot be found.
     */
    public byte[] expectFileBytes(String file) {
        return expectFileBytes(Paths.get(file));
    }
}
