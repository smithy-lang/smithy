/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Settings for trait code generation. These can be set in the
 * {@code smithy-build.json} configuration for this plugin.
 *
 * <p>The following options are provided:
 * <dl>
 *     <dt>"packageName" ({@code String})</dt>
 *     <dd>Sets the package namespace to use for generated Java classes.</dd>
 *     <dt>"headerLines" ({@code List<String>}) </dt>
 *     <dd>Defines the header comment to include in all output files. Use
 *     this setting to add license and/or author information to all generated files. Each entry in the list
 *     is generated as a new line in the generated comment.</dd>
 *     <dt>"excludeTags" ({@code List<String>})</dt>
 *     <dd>List of Smithy tags to use for filtering out trait shapes
 *     from the trait code generation process.</dd>
 * </dl>
 */
@SmithyUnstableApi
public final class TraitCodegenSettings {
    private static final String SMITHY_MODEL_NAMESPACE = "software.amazon.smithy";
    private static final String SMITHY_API_NAMESPACE = "smithy";

    private final String packageName;
    private final String smithyNamespace;
    private final List<String> headerLines;
    private final List<String> excludeTags;

    /**
     * Settings for trait code generation. These can be set in the
     * {@code smithy-build.json} configuration for this plugin.
     *
     * @param packageName java package name to use for generated code. For example {@code com.example.traits}.
     * @param smithyNamespace smithy namespace to search for traits in.
     * @param headerLines lines of text to included as a header in all generated code files. This might be a
     *                    license header or copyright header that should be included in all generated files.
     * @param excludeTags smithy tags to exclude from trait code generation. Traits with these tags will be
     *                    ignored when generating java classes.
     */
    TraitCodegenSettings(
            String packageName,
            String smithyNamespace,
            List<String> headerLines,
            List<String> excludeTags
    ) {
        this.packageName = Objects.requireNonNull(packageName);
        if (packageName.startsWith(SMITHY_MODEL_NAMESPACE)) {
            throw new IllegalArgumentException("The `software.amazon.smithy` package namespace is reserved.");
        }
        this.smithyNamespace = Objects.requireNonNull(smithyNamespace);
        if (smithyNamespace.startsWith(SMITHY_API_NAMESPACE)) {
            throw new IllegalArgumentException("The `smithy` namespace is reserved.");
        }
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
                node.expectStringMember("namespace").getValue(),
                node.expectArrayMember("header")
                        .getElementsAs(el -> el.expectStringNode().getValue()),
                node.getArrayMember("excludeTags")
                        .map(n -> n.getElementsAs(el -> el.expectStringNode().getValue()))
                        .orElse(Collections.emptyList()));
    }

    /**
     * Java package name to generate traits into.
     *
     * @return package name
     */
    public String packageName() {
        return packageName;
    }

    /**
     * Smithy namespace to search for traits.
     *
     * @return namespace
     */
    public String smithyNamespace() {
        return smithyNamespace;
    }

    /**
     * List of lines added to the top of every generated file as a header.
     *
     * @return header lines as a list
     */
    public List<String> headerLines() {
        return headerLines;
    }

    /**
     * List of tags to exclude from shape code generation.
     *
     * @return tag list
     */
    public List<String> excludeTags() {
        return excludeTags;
    }
}
