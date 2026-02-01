/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.docs;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.DefaultBuilderRef;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a file containing generated snippets.
 *
 * <p>Snippets are generated code based on some trait or other shared definition,
 * such as the {@link software.amazon.smithy.model.traits.ExamplesTrait}. These are
 * created by code generators and consumed by documentation tools, such as
 * smithy-docgen.
 *
 * <p>These are differentiated from typical code gen artifacts in that they are
 * intended to be shared. These may be distributed and aggregated in any manner,
 * but they SHOULD be written to the {@literal snippets} directory of the shared
 * plugin space by Smithy build plugins. smithy-docgen will discover and include
 * any snippet files in that directory. Smithy build plugins MUST declare a
 * {@code runBefore} on "docgen" for their generated snippets to be included.
 */
@SmithyUnstableApi
public final class SnippetConfig implements ToSmithyBuilder<SnippetConfig>, ToNode {
    public static final String DEFAULT_VERSION = "1.0";

    private final String version;
    private final Map<ShapeId, Map<ShapeId, List<Snippet>>> snippets;

    private SnippetConfig(Builder builder) {
        this.version = SmithyBuilder.requiredState("version", builder.version);
        this.snippets = builder.snippets.copy();
    }

    /**
     * @return Returns the version of the snippet config.
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return Returns all the snippets in the config.
     */
    public Map<ShapeId, Map<ShapeId, List<Snippet>>> getSnippets() {
        return snippets;
    }

    /**
     * Gets all the snippets for a particular service.
     *
     * @param id The id of the service to get snippets for.
     * @return Returns the snippets for shapes bound to the service.
     */
    public Map<ShapeId, List<Snippet>> getServiceSnippets(ShapeId id) {
        return snippets.getOrDefault(id, Collections.emptyMap());
    }

    /**
     * Gets the snippets for a particular shape in a service.
     *
     * @param serviceId The service to search for snippets.
     * @param shapeId The shape to get snippets for.
     * @return Returns the snippets for a shape from a service.
     */
    public List<Snippet> getShapeSnippets(ShapeId serviceId, ShapeId shapeId) {
        return getServiceSnippets(serviceId).getOrDefault(shapeId, Collections.emptyList());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SnippetConfig)) {
            return false;
        }
        SnippetConfig config = (SnippetConfig) o;
        return Objects.equals(version, config.version) && Objects.equals(snippets, config.snippets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, snippets);
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .version(version)
                .snippets(snippets);
    }

    /**
     * @return Returns a new SnippetConfig builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Node toNode() {
        return ObjectNode.objectNodeBuilder()
                .withMember("version", version)
                .withMember("snippets", new NodeMapper().serialize(snippets))
                .build();
    }

    /**
     * @param node A node representing a SnippetConfig.
     * @return Returns a SnippetConfig based on the given node.
     */
    public static SnippetConfig fromNode(Node node) {
        Builder builder = builder();
        new NodeMapper().deserializeInto(node, builder);
        return builder.build();
    }

    /**
     * Loads a SnippetConfig from a JSON file.
     *
     * @param path The path to the JSON snippet config.
     * @return Returns a SnippetConfig based on the given file.
     */
    public static SnippetConfig load(Path path) {
        return fromNode(Node.parse(IoUtils.readUtf8File(path)));
    }

    public final static class Builder implements SmithyBuilder<SnippetConfig> {
        private String version = DEFAULT_VERSION;
        private final BuilderRef<Map<ShapeId, Map<ShapeId, List<Snippet>>>> snippets = new DefaultBuilderRef<>(
                LinkedHashMap::new,
                Builder::copySnippetMap,
                Builder::immutableWrapSnippetMap,
                Collections::emptyMap);

        private static Map<ShapeId, Map<ShapeId, List<Snippet>>> copySnippetMap(
                Map<ShapeId, Map<ShapeId, List<Snippet>>> snippets
        ) {
            return copySnippetMap(snippets, ArrayList::new, Function.identity());
        }

        private static Map<ShapeId, Map<ShapeId, List<Snippet>>> immutableWrapSnippetMap(
                Map<ShapeId, Map<ShapeId, List<Snippet>>> snippets
        ) {
            return MapUtils.orderedCopyOf(copySnippetMap(snippets, ListUtils::copyOf, MapUtils::orderedCopyOf));
        }

        private static Map<ShapeId, Map<ShapeId, List<Snippet>>> copySnippetMap(
                Map<ShapeId, Map<ShapeId, List<Snippet>>> snippets,
                Function<List<Snippet>, List<Snippet>> copySnippetList,
                Function<Map<ShapeId, List<Snippet>>, Map<ShapeId, List<Snippet>>> finalizeServiceCopy
        ) {
            LinkedHashMap<ShapeId, Map<ShapeId, List<Snippet>>> copy = new LinkedHashMap<>();
            for (Map.Entry<ShapeId, Map<ShapeId, List<Snippet>>> entry : snippets.entrySet()) {
                LinkedHashMap<ShapeId, List<Snippet>> serviceCopy = new LinkedHashMap<>();
                for (Map.Entry<ShapeId, List<Snippet>> serviceEntry : entry.getValue().entrySet()) {
                    serviceCopy.put(serviceEntry.getKey(), copySnippetList.apply(serviceEntry.getValue()));
                }
                copy.put(entry.getKey(), finalizeServiceCopy.apply(serviceCopy));
            }
            return copy;
        }

        @Override
        public SnippetConfig build() {
            return new SnippetConfig(this);
        }

        /**
         * Sets the version of the config schema.
         *
         * @param version The version to set.
         * @return Returns the builder.
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the snippet map, erasing any existing values.
         *
         * @param snippets The snippets to set.
         * @return Returns the builder.
         */
        public Builder snippets(Map<ShapeId, Map<ShapeId, List<Snippet>>> snippets) {
            this.snippets.clear();
            for (Map.Entry<ShapeId, Map<ShapeId, List<Snippet>>> entry : snippets.entrySet()) {
                putServiceSnippets(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /**
         * Merges new snippets into the snippet map.
         *
         * @param snippets The snippets to merge in.
         * @return Returns the builder.
         */
        public Builder mergeSnippets(Map<ShapeId, Map<ShapeId, List<Snippet>>> snippets) {
            for (Map.Entry<ShapeId, Map<ShapeId, List<Snippet>>> entry : snippets.entrySet()) {
                mergeServiceSnippets(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public Builder mergeSnippets(SnippetConfig snippetConfig) {
            if (!snippetConfig.getVersion().equals(version)) {
                throw new IllegalArgumentException(String.format(
                        "Tried to merge snippets from incompatible version. Expected: %s but was %s",
                        version,
                        snippetConfig.getVersion()));
            }
            return mergeSnippets(snippetConfig.getSnippets());
        }

        /**
         * Sets the snippets for a particular service, erasing any existing values.
         *
         * @param serviceId The service to set the snippets for.
         * @param serviceSnippets The snippets to set in the service.
         * @return Returns the builder.
         */
        public Builder putServiceSnippets(ShapeId serviceId, Map<ShapeId, List<Snippet>> serviceSnippets) {
            Map<ShapeId, List<Snippet>> mutableCopy = new LinkedHashMap<>(serviceSnippets.size());
            for (Map.Entry<ShapeId, List<Snippet>> entry : serviceSnippets.entrySet()) {
                mutableCopy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            snippets.get().put(serviceId, mutableCopy);
            return this;
        }

        /**
         * Merges in the snippets for a particular service.
         *
         * @param serviceId The service to merge snippets for.
         * @param serviceSnippets The snippets to merge in.
         * @return Returns the builder.
         */
        public Builder mergeServiceSnippets(ShapeId serviceId, Map<ShapeId, List<Snippet>> serviceSnippets) {
            Map<ShapeId, List<Snippet>> old = snippets.get().computeIfAbsent(serviceId, k -> new LinkedHashMap<>());
            for (Map.Entry<ShapeId, List<Snippet>> entry : serviceSnippets.entrySet()) {
                old.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
            }
            return this;
        }
    }
}
