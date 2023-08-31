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

package software.amazon.smithy.cli.dependencies;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.model.loader.ModelSyntaxException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * A resolver that loads and caches resolved artifacts to a JSON file if
 * the cache is fresh and resolved artifacts haven't been updated after a
 * given reference point in time.
 */
public final class FileCacheResolver implements DependencyResolver {
    private static final String CURRENT_CACHE_FILE_VERSION = "1.0";

    // This is hard-coded for now to 1 day, and it can become an environment variable in the future if needed.
    private static final Duration SMITHY_MAVEN_TTL = Duration.parse("P1D");

    private static final Logger LOGGER = Logger.getLogger(FileCacheResolver.class.getName());
    private final DependencyResolver delegate;
    private final File location;
    private final long referenceTimeInMillis;

    /**
     * @param location The location to the cache.
     * @param referenceTimeInMillis Invalidate cache items if this time is newer than the cache item time.
     * @param delegate Resolver to delegate to when dependencies aren't cached.
     */
    public FileCacheResolver(File location, long referenceTimeInMillis, DependencyResolver delegate) {
        this.location = location;
        this.referenceTimeInMillis = referenceTimeInMillis;
        this.delegate = delegate;
    }

    @Override
    public void addRepository(MavenRepository repository) {
        delegate.addRepository(repository);
    }

    @Override
    public void addDependency(String coordinates) {
        delegate.addDependency(coordinates);
    }

    @Override
    public List<ResolvedArtifact> resolve() {
        List<ResolvedArtifact> cachedResult = load();

        if (!cachedResult.isEmpty()) {
            LOGGER.fine(() -> "Classpath found in cache: " + cachedResult);
            return cachedResult;
        }

        List<ResolvedArtifact> result = delegate.resolve();
        save(result);
        return result;
    }

    private List<ResolvedArtifact> load() {
        // Invalidate the cache if:
        // 1. smithy-build.json was updated after the cache was written.
        // 2. the cache file is older than the allowed TTL.
        // 3. a cached artifact was deleted.
        // 4. a cached artifact is newer than the cache file.
        long cacheLastModifiedMillis = location.lastModified();
        long currentTimeMillis = new Date().getTime();
        long ttlMillis = SMITHY_MAVEN_TTL.toMillis();

        if (location.length() == 0) {
            return Collections.emptyList();
        } else if (!isCacheValid(cacheLastModifiedMillis)) {
            LOGGER.fine("Invalidating dependency cache: config is newer than the cache");
            invalidate();
            return Collections.emptyList();
        } else if (currentTimeMillis - cacheLastModifiedMillis > ttlMillis) {
            LOGGER.fine(() -> "Invalidating dependency cache: Cache exceeded TTL (TTL: " + ttlMillis + ")");
            invalidate();
            return Collections.emptyList();
        }

        ObjectNode node;
        try (InputStream stream = Files.newInputStream(location.toPath())) {
            node = Node.parse(stream, location.toString()).expectObjectNode();
        } catch (ModelSyntaxException | IOException e) {
            throw new DependencyResolverException("Error loading dependency cache file from " + location, e);
        }

        // If the version of the cache file does not match the current version or does not exist
        // invalidate it so we can replace it with a more recent version.
        if (!node.containsMember("version")
                || !CURRENT_CACHE_FILE_VERSION.equals(node.expectStringMember("version").getValue())
        ) {
            LOGGER.fine(() -> "Invalidating dependency cache: cache file uses old version");
            invalidate();
            return Collections.emptyList();
        }

        ObjectNode artifactNode = node.expectObjectMember("artifacts");
        List<ResolvedArtifact> result = new ArrayList<>(artifactNode.getStringMap().size());
        for (Map.Entry<String, Node> entry : artifactNode.getStringMap().entrySet()) {
            ResolvedArtifact artifact = ResolvedArtifact.fromNode(entry.getKey(), entry.getValue());
            long lastModifiedOfArtifact = artifact.getLastModified();
            // Invalidate the cache if the JAR file was updated since the cache was created.
            if (lastModifiedOfArtifact == 0 || lastModifiedOfArtifact > cacheLastModifiedMillis) {
                LOGGER.fine(() -> "Invalidating dependency cache: artifact is newer than cache: " + artifact.getPath());
                invalidate();
                return Collections.emptyList();
            }
            result.add(artifact);
        }

        return result;
    }

    private void save(List<ResolvedArtifact> result) {
        Path filePath = location.toPath();
        Path parent = filePath.getParent();
        if (parent == null) {
            throw new DependencyResolverException("Invalid classpath cache location: " + location);
        }

        try {
            Files.createDirectories(parent);
            ObjectNode.Builder builder = Node.objectNodeBuilder();
            builder.withMember("version", CURRENT_CACHE_FILE_VERSION);
            ObjectNode.Builder artifactNodeBuilder = Node.objectNodeBuilder();
            for (ResolvedArtifact artifact : result) {
                artifactNodeBuilder.withMember(artifact.getCoordinates(), artifact.toNode());
            }
            builder.withMember("artifacts", artifactNodeBuilder.build());
            Files.write(filePath, Node.printJson(builder.build()).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new DependencyResolverException("Unable to write classpath cache file: " + e.getMessage(), e);
        }
    }

    private boolean isCacheValid(long cacheLastModifiedMillis) {
        return referenceTimeInMillis <= cacheLastModifiedMillis;
    }

    private void invalidate() {
        if (location.exists() && !location.delete()) {
            LOGGER.warning("Unable to invalidate dependency cache file: " + location);
        }
    }
}
