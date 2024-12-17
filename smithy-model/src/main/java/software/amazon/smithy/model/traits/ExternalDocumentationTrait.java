/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Provides named links to external documentation.
 */
public final class ExternalDocumentationTrait extends AbstractTrait
        implements ToSmithyBuilder<ExternalDocumentationTrait> {

    public static final ShapeId ID = ShapeId.from("smithy.api#externalDocumentation");

    private final Map<String, String> urls;

    public ExternalDocumentationTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        this.urls = Collections.unmodifiableMap(builder.urls);

        urls.forEach((name, url) -> validateUrl(name, url, getSourceLocation()));
    }

    private static String validateUrl(String name, String url, SourceLocation location) {
        try {
            new URL(url);
            return url;
        } catch (MalformedURLException e) {
            throw new SourceException(String.format("Each externalDocumentation value must be a valid URL. "
                    + "Found \"%s\" for name \"%s\"", url, name), location);
        }
    }

    /**
     * Gets the external documentation names and links.
     *
     * @return returns the external documentation mapping.
     */
    public Map<String, String> getUrls() {
        return urls;
    }

    @Override
    protected Node createNode() {
        return ObjectNode.fromStringMap(urls).toBuilder().sourceLocation(getSourceLocation()).build();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder().sourceLocation(getSourceLocation());
        urls.forEach(builder::addUrl);
        return builder;
    }

    /**
     * @return Returns an external documentation trait builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create the external documentation trait.
     */
    public static final class Builder extends AbstractTraitBuilder<ExternalDocumentationTrait, Builder> {
        private final Map<String, String> urls = new LinkedHashMap<>();

        public Builder addUrl(String name, String url) {
            urls.put(Objects.requireNonNull(name), Objects.requireNonNull(url));
            return this;
        }

        public Builder removeUrl(String name) {
            urls.remove(name);
            return this;
        }

        public Builder clearUrls() {
            urls.clear();
            return this;
        }

        @Override
        public ExternalDocumentationTrait build() {
            return new ExternalDocumentationTrait(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public ExternalDocumentationTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            value.expectObjectNode().getMembers().forEach((k, v) -> {
                builder.addUrl(k.expectStringNode().getValue(), v.expectStringNode().getValue());
            });
            ExternalDocumentationTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }
}
