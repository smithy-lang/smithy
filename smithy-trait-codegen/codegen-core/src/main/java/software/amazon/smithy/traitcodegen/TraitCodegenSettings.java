/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Settings for trait code generation. These can be set in the
 * {@code smithy-build.json} configuration for this plugin.
 */
@SmithyUnstableApi
public final class TraitCodegenSettings {
    private final String packageName;
    private final List<String> headerLines;
    private final List<String> excludeTags;

    /**
     * Settings for trait code generation. These can be set in the
     * {@code smithy-build.json} configuration for this plugin.
     *
     * @param packageName java package name to use for generated code. For example {@code com.example.traits}.
     * @param headerLines lines of text to included as a header in all generated code files. This might be a
     *                    license header or copyright header that should be included in all generated files.
     * @param excludeTags smithy tags to exclude from trait code generation. Traits with these tags will be
     *                    ignored when generating java classes.
     */
    TraitCodegenSettings(String packageName, List<String> headerLines, List<String> excludeTags) {
        this.packageName = Objects.requireNonNull(packageName);
        this.headerLines = Objects.requireNonNull(headerLines);
        this.excludeTags = Objects.requireNonNull(excludeTags);
    }

    /**
     * Loads settings from an {@link ObjectNode}.
     *
     * @param node object node to load settings from
     * @return settings loaded from given node
     */
    public static TraitCodegenSettings fromNode(ObjectNode node) {
        return new TraitCodegenSettings(
                node.expectStringMember("package").getValue(),
                node.expectArrayMember("header")
                        .getElementsAs(el -> el.expectStringNode().getValue()),
                node.getArrayMember("excludeTags")
                        .map(n -> n.getElementsAs(el -> el.expectStringNode().getValue()))
                        .orElse(new ArrayList<>())
        );
    }

    public String packageName() {
        return packageName;
    }

    public List<String> headerLines() {
        return headerLines;
    }

    public List<String> excludeTags() {
        return excludeTags;
    }
}
