/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.SetUtils;
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
    private static final String SMITHY_KEYWORD = "smithy";
    private static final Set<String> RESERVED_NAMESPACES = SetUtils.of("smithy.api",
            "smithy.framework",
            "smithy.mqtt",
            "smithy.openapi",
            "smithy.protocols",
            "smithy.rules",
            "smithy.test",
            "smithy.waiters");

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
        if (isReservedNamespace(smithyNamespace)) {
            throw new IllegalArgumentException("The `smithy` namespace and its sub-namespaces are reserved.");
        }
        this.smithyNamespace = Objects.requireNonNull(smithyNamespace);
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

    /**
     * Checks if a namespace is reserved. A namespace is considered reserved if it is
     * "smithy", one of the predefined reserved namespaces or if it starts with one of the reserved namespaces followed by a dot.
     *
     * @param namespace the namespace to check
     * @return true if the namespace is reserved, false otherwise
     */
    private static boolean isReservedNamespace(String namespace) {
        String namespaceLowerCase = namespace.toLowerCase(Locale.ROOT);
        if (namespaceLowerCase.equals(SMITHY_KEYWORD) || RESERVED_NAMESPACES.contains(namespaceLowerCase)) {
            return true;
        }

        for (String reserved : RESERVED_NAMESPACES) {
            if (namespaceLowerCase.startsWith(reserved + ".")) {
                return true;
            }
        }

        return false;
    }
}
