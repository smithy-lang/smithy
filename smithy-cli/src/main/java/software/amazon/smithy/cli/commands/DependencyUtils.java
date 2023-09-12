/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.cli.commands;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import software.amazon.smithy.build.model.MavenConfig;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.EnvironmentVariable;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.IoUtils;

public final class DependencyUtils {
    private static final Logger LOGGER = Logger.getLogger(DependencyUtils.class.getName());
    private static final Path LOCK_FILE = Paths.get("smithy-lock.json");
    private static final MavenRepository CENTRAL = MavenRepository.builder()
            .url("https://repo.maven.apache.org/maven2")
            .build();

    private DependencyUtils() {
        // Utility Class should not be instantiated
    }

    /**
     * Computes the sha1 digest of a file.
     *
     * @param path Path to file to compute hash for.
     * @return sha1 digest string.
     * @throws UncheckedIOException if the specified file could not be read.
     */
    public static String computeSha1(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            try (DigestInputStream din = new DigestInputStream(in, md)) {
                byte[] buf = new byte[1024 * 32];
                int n;
                do {
                    n = din.read(buf);
                } while (n > 0);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                int decimal = (int) b & 0xff;
                String hex = Integer.toHexString(decimal);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Gets the list of Maven Repos configured via smithy-build config and environment variables.
     * <p>
     * Environment variables take precedence over config file entries. If no repositories are
     * defined in either environment variables or config file entries, then the returned set
     * defaults to a singleton set of just the Maven Central repo.
     *
     * @param config Smithy build config to check for maven repository values.
     * @return set of configured repositories.
     */
    public static Set<MavenRepository> getConfiguredMavenRepos(SmithyBuildConfig config) {
        Set<MavenRepository> repositories = new LinkedHashSet<>();

        String envRepos = EnvironmentVariable.SMITHY_MAVEN_REPOS.get();
        if (envRepos != null) {
            for (String repo : envRepos.split("\\|")) {
                repositories.add(MavenRepository.builder().url(repo.trim()).build());
            }
        }

        Set<MavenRepository> configuredRepos = config.getMaven()
                .map(MavenConfig::getRepositories)
                .orElse(Collections.emptySet());

        if (!configuredRepos.isEmpty()) {
            repositories.addAll(configuredRepos);
        } else if (envRepos == null) {
            LOGGER.finest(() -> String.format("maven.repositories is not defined in smithy-build.json and the %s "
                            + "environment variable is not set. Defaulting to Maven Central.",
                    EnvironmentVariable.SMITHY_MAVEN_REPOS));
            repositories.add(CENTRAL);
        }

        return repositories;
    }

    /**
     * Computes the combined hash of the artifact set and repositories set.
     *
     * @param artifacts set of requested artifact coordinates
     * @param repositories set of repositories used for dependency resolution
     * @return combined hash
     */
    public static int configHash(Set<String> artifacts, Set<MavenRepository> repositories) {
        int result = 0;
        for (String artifact : artifacts) {
            result = 31 * result + artifact.hashCode();
        }
        for (MavenRepository repo : repositories) {
            result = 31 * result + repo.getUrl().hashCode();
        }
        return result;
    }

    public static Optional<LockFile> loadLockfile() {
        return loadLockfile(LOCK_FILE);
    }

    /**
     * Loads  lockfile if it exists.
     *
     * @param path path of file to load as a lockfile
     * @return Lockfile if it exists otherwise Optional.empty().
     */
    public static Optional<LockFile> loadLockfile(Path path) {
        if (Files.exists(path) && Files.isRegularFile(path)) {
            String contents = IoUtils.readUtf8File(path);
            Node result = Node.parseJsonWithComments(contents);
            return Optional.of(LockFile.fromNode(result));
        }
        return Optional.empty();
    }

    /**
     * Saves lockfile to current working directory.
     */
    public static void saveLockFile(LockFile lockFile) {
        try {
            Files.write(LOCK_FILE, Node.printJson(lockFile.toNode()).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
