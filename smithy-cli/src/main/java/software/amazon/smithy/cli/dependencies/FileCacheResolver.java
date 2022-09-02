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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
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

    private static final Logger LOGGER = Logger.getLogger(FileCacheResolver.class.getName());
    private final DependencyResolver delegate;
    private final File location;
    private final long referenceTimeInMillis;

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

    List<ResolvedArtifact> load() {
        // Invalidate the cache if smithy-build.json was updated after the cache was written.
        Path filePath = location.toPath();
        if (!Files.exists(filePath)) {
            return Collections.emptyList();
        } else if (!isCacheValid(location)) {
            invalidate(filePath);
            return Collections.emptyList();
        }

        ObjectNode node;
        try (InputStream stream = Files.newInputStream(filePath)) {
            node = Node.parse(stream, location.toString()).expectObjectNode();
        } catch (ModelSyntaxException | IOException e) {
            throw new DependencyResolverException("Error loading dependency cache file from " + filePath, e);
        }

        List<ResolvedArtifact> result = new ArrayList<>(node.getStringMap().size());
        for (Map.Entry<String, Node> entry : node.getStringMap().entrySet()) {
            Path location = Paths.get(entry.getValue().expectStringNode().getValue());
            // Invalidate the cache if the JAR file was updated after the cache was written.
            if (isArtifactUpdatedSinceReferenceTime(location)) {
                invalidate(filePath);
                return Collections.emptyList();
            }
            result.add(ResolvedArtifact.fromCoordinates(location, entry.getKey()));
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
            for (ResolvedArtifact artifact : result) {
                builder.withMember(artifact.getCoordinates(), artifact.getPath().toString());
            }
            ObjectNode objectNode = builder.build();
            Files.write(filePath, Node.printJson(objectNode).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new DependencyResolverException("Unable to write classpath cache file: " + e.getMessage(), e);
        }
    }

    private boolean isCacheValid(File file) {
        return referenceTimeInMillis <= file.lastModified() && file.length() > 0;
    }

    private boolean isArtifactUpdatedSinceReferenceTime(Path path) {
        File file = path.toFile();
        return !file.exists() || (referenceTimeInMillis > 0 && file.lastModified() > referenceTimeInMillis);
    }

    private void invalidate(Path filePath) {
        try {
            if (Files.exists(filePath)) {
                LOGGER.fine("Invalidating dependency cache file: " + location);
                Files.delete(filePath);
            }
        } catch (IOException e) {
            throw new DependencyResolverException("Unable to delete cache file: " + e.getMessage(), e);
        }
    }
}
