/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
* Defines the XML Namespace prefix and URI.
*/
public final class XmlNamespaceTrait extends AbstractTrait implements ToSmithyBuilder<XmlNamespaceTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#xmlNamespace");

    private final String prefix;
    private final String uri;

    private XmlNamespaceTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        uri = SmithyBuilder.requiredState("uri", builder.uri);
        prefix = builder.prefix;
    }

    public String getUri() {
        return uri;
    }

    public Optional<String> getPrefix() {
        return Optional.ofNullable(prefix);
    }

    @Override
    protected Node createNode() {
        return new ObjectNode(MapUtils.of(), getSourceLocation())
                .withMember("uri", Node.from(uri))
                .withOptionalMember("prefix", getPrefix().map(Node::from));
    }

    /**
    * @return Returns a builder used to create an XmlNamespace trait.
    */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .uri(uri)
                .prefix(prefix);
    }

    /**
    * Builder used to create an XmlNamespace trait.
    */
    public static final class Builder extends AbstractTraitBuilder<XmlNamespaceTrait, Builder> {
        private String uri;
        private String prefix;

        private Builder() {}

        public Builder uri(String uri) {
            this.uri = Objects.requireNonNull(uri);
            return this;
        }

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        @Override
        public XmlNamespaceTrait build() {
            return new XmlNamespaceTrait(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public XmlNamespaceTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            value.expectObjectNode()
                    .expectStringMember("uri", builder::uri)
                    .getStringMember("prefix", builder::prefix);
            XmlNamespaceTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }
}
