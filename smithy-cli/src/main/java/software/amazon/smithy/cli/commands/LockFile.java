/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.cli.commands;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.dependencies.DependencyResolverException;
import software.amazon.smithy.cli.dependencies.ResolvedArtifact;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

final class LockFile implements ToSmithyBuilder<LockFile>, ToNode {
    private static final String DEFAULT_VERSION = "1.0";
    private static final String WARNING_STR = "AUTOGENERATED_DO_NOT_MODIFY";
    private static final String VERSION_NAME = "version";
    private static final String CONFIG_HASH_NAME = "configHash";
    private static final String ARTIFACT_LIST_NAME = "artifacts";
    private static final String REPOSITORIES_LIST_NAME = "repositories";
    private static final List<String> EXPECTED_PROPERTIES = ListUtils.of(
            WARNING_STR,
            VERSION_NAME,
            CONFIG_HASH_NAME,
            ARTIFACT_LIST_NAME,
            REPOSITORIES_LIST_NAME
    );

    private final Map<String, PinnedArtifact> artifacts;
    private final Set<String> repositories;
    private final int configHash;
    private final String version;

    private LockFile(Builder builder) {
        this.artifacts = builder.artifacts.copy();
        this.repositories =  builder.repositories.copy();
        this.version = builder.version;
        this.configHash = builder.configHash;
    }

    public static LockFile fromNode(Node node) {
        LockFile.Builder builder = builder();
        node.expectObjectNode()
                .warnIfAdditionalProperties(EXPECTED_PROPERTIES)
                .getStringMember(VERSION_NAME, builder::version)
                .getNumberMember(CONFIG_HASH_NAME, n -> builder.configHash(n.intValue()))
                .getObjectMember(ARTIFACT_LIST_NAME, artifactList -> {
                    for (Map.Entry<String, Node> entry : artifactList.getStringMap().entrySet()) {
                        builder.artifact(PinnedArtifact.fromCoordinateNode(entry.getKey(), entry.getValue()));
                    }
                })
                .getArrayMember(REPOSITORIES_LIST_NAME, StringNode::getValue, builder::repositories);
        return builder.build();
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withMember(WARNING_STR, "")
                .withMember(VERSION_NAME, version)
                .withMember(CONFIG_HASH_NAME, configHash)
                .withMember(REPOSITORIES_LIST_NAME, ArrayNode.fromStrings(repositories));

        ObjectNode.Builder artifactNodeBuilder = Node.objectNodeBuilder();
        for (PinnedArtifact artifact : artifacts.values()) {
            artifactNodeBuilder.withMember(artifact.coordinates, artifact.toNode());
        }
        builder.withMember(ARTIFACT_LIST_NAME, artifactNodeBuilder.build());

        return builder.build();
    }

    public void validateArtifacts(List<ResolvedArtifact> resolvedArtifacts) {
        for (ResolvedArtifact artifact : resolvedArtifacts) {
            PinnedArtifact pin = artifacts.get(artifact.getCoordinates());
            if (pin == null) {
                throw new CliError("Resolved artifact `" + artifact + "` does not match pinned artifacts.");
            }
            if (!pin.matchesResolved(artifact)) {
                throw new CliError("Resolved artifact `" + artifact + "` does not match pinned artifact `" + pin + "`");
            }
        }
    }

    public static LockFile.Builder builder() {
        return new LockFile.Builder();
    }

    /**
     * @return Gets the configuration hash for the lockfile
     */
    public int getConfigHash() {
        return configHash;
    }

    /**
     * @return Gets the list of pinned artifacts.
     */
    public Set<String> getDependencyCoordinateSet() {
        return artifacts.keySet();
    }

    /**
     * @return Gets the version of lockfile.
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return Gets the pinned repositories in the lockfile.
     */
    public Set<String> getRepositories() {
        return repositories;
    }


    @Override
    public LockFile.Builder toBuilder() {
        return builder().repositories(repositories)
                .artifacts(artifacts)
                .version(version)
                .configHash(configHash);
    }

    public static final class Builder implements SmithyBuilder<LockFile> {
        private final BuilderRef<Map<String, PinnedArtifact>> artifacts = BuilderRef.forOrderedMap();
        private final BuilderRef<Set<String>> repositories = BuilderRef.forOrderedSet();
        private String version = DEFAULT_VERSION;
        private int configHash;

        public LockFile.Builder artifacts(Map<String, PinnedArtifact> artifacts) {
            this.artifacts.clear();
            this.artifacts.get().putAll(artifacts);
            return this;
        }

        public LockFile.Builder artifacts(Collection<ResolvedArtifact> artifacts) {
            this.artifacts.clear();
            for (ResolvedArtifact artifact : artifacts) {
                this.artifacts.get().put(artifact.getCoordinates(), PinnedArtifact.from(artifact));
            }
            return this;
        }

        public LockFile.Builder artifact(ResolvedArtifact artifact) {
            this.artifacts.get().put(artifact.getCoordinates(), PinnedArtifact.from(artifact));
            return this;
        }

        public LockFile.Builder artifact(PinnedArtifact artifact) {
            this.artifacts.get().put(artifact.coordinates, artifact);
            return this;
        }

        public LockFile.Builder repositories(Set<MavenRepository> repositories) {
            this.repositories.clear();
            for (MavenRepository repository : repositories) {
                this.repositories.get().add(repository.getUrl());
            }
            return this;
        }

        public LockFile.Builder repositories(Collection<String> repositories) {
            this.repositories.clear();
            this.repositories.get().addAll(repositories);
            return this;
        }

        public LockFile.Builder version(String version) {
            this.version = version;
            return this;
        }

        public LockFile.Builder configHash(int configHash) {
            this.configHash = configHash;
            return this;
        }

        @Override
        public LockFile build() {
            return new LockFile(this);
        }
    }

    private static final class PinnedArtifact implements ToNode {
        private static final String SHA_SUM_MEMBER_NAME = "sha1";
        private final String coordinates;
        private final String shaSum;

        private PinnedArtifact(String coordinates, String shaSum) {
            this.coordinates = coordinates;
            this.shaSum = shaSum;
        }

        private static PinnedArtifact from(ResolvedArtifact resolvedArtifact) {
            return new PinnedArtifact(resolvedArtifact.getCoordinates(), resolvedArtifact.getShaSum());
        }
        private static PinnedArtifact fromCoordinateNode(String coordinates, Node node) {
            validateCoordinates(coordinates);
            ObjectNode objectNode = node.expectObjectNode();
            String shaSum = objectNode.expectMember(SHA_SUM_MEMBER_NAME).expectStringNode().getValue();

            return new PinnedArtifact(coordinates, shaSum);
        }

        @Override
        public Node toNode() {
            return Node.objectNodeBuilder()
                    .withMember(SHA_SUM_MEMBER_NAME, shaSum)
                    .build();
        }

        private static void validateCoordinates(String coords) {
            String[] parts = coords.split(":");
            if (parts.length != 3) {
                throw new DependencyResolverException("Invalid Pinned coordinates: " + coords);
            }
        }

        boolean matchesResolved(ResolvedArtifact resolved) {
            return coordinates.equals(resolved.getCoordinates()) && shaSum.equals(resolved.getShaSum());
        }
    }
}

