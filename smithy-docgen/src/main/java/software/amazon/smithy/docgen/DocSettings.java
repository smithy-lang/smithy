/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Settings for documentation generation. These can be set in the
 * {@code smithy-build.json} configuration for this plugin.
 *
 * @param service The shape id of the service to generate documentation for.
 * @param format The format to generate documentation in. The default is markdown.
 * @param references A mapping of external resources to their documentation URIs, used
 *     when generating links for the
 *     <a href="https://smithy.io/2.0/spec/resource-traits.html#references-trait">references trait</a>
 *     for resources that are not contained within the model.
 */
@SmithyUnstableApi
public record DocSettings(ShapeId service, String format, Map<ShapeId, String> references) {

    /**
     * Settings for documentation generation. These can be set in the
     * {@code smithy-build.json} configuration for this plugin.
     *
     * @param service The shape id of the service to generate documentation for.
     * @param format The format to generate documentation in. The default is markdown.
     */
    public DocSettings {
        Objects.requireNonNull(service);
        Objects.requireNonNull(format);
    }

    /**
     * Load the settings from an {@code ObjectNode}.
     *
     * @param pluginSettings the {@code ObjectNode} to load settings from.
     * @return loaded settings based on the given node.
     */
    public static DocSettings fromNode(ObjectNode pluginSettings) {
        var references = pluginSettings.getObjectMember("references")
                .orElse(ObjectNode.objectNode())
                .getMembers()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> ShapeId.from(e.getKey().getValue()),
                        e -> e.getValue().expectStringNode().getValue()));
        return new DocSettings(
                pluginSettings.expectStringMember("service").expectShapeId(),
                pluginSettings.getStringMemberOrDefault("format", "sphinx-markdown"),
                references);
    }
}
