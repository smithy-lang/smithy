/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that an operation supports compressing requests from clients to services.
 */
public final class RequestCompressionTrait extends AbstractTrait implements ToSmithyBuilder<RequestCompressionTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#requestCompression");
    /**
     * Set of supported compression algorithm encoding values.
     */
    public static final Set<String> SUPPORTED_COMPRESSION_ALGORITHMS = SetUtils.of(
            // NOTE: When a new encoding is supported, add the encoding to
            //       `multiple-encodings.smithy`
            // TODO: When the next encoding value after "gzip" is added, remove the
            //       duplicate "gzip" value in `multiple-encodings.smithy`. Also, add
            //       validation to make sure encoding values are unique. Then remove
            //       this TODO item.
            "gzip");

    public static final String ENCODINGS = "encodings";

    private final List<String> encodings;

    private RequestCompressionTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.encodings = builder.encodings.copy();
    }

    public List<String> getEncodings() {
        return encodings;
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            Builder builder = builder().sourceLocation(value);
            objectNode.expectArrayMember(ENCODINGS, StringNode::getValue, builder::encodings);
            RequestCompressionTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }

    @Override
    protected Node createNode() {
        return ObjectNode.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withMember(ENCODINGS, Node.fromStrings(getEncodings()))
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder()
                .sourceLocation(getSourceLocation())
                .encodings(encodings);
    }

    public static final class Builder
            extends AbstractTraitBuilder<RequestCompressionTrait, RequestCompressionTrait.Builder> {
        private final BuilderRef<List<String>> encodings = BuilderRef.forList();

        private Builder() {}

        @Override
        public RequestCompressionTrait build() {
            return new RequestCompressionTrait(this);
        }

        public Builder encodings(List<String> encodings) {
            this.encodings.clear();
            this.encodings.get().addAll(encodings);
            return this;
        }

        public Builder addEncoding(String encoding) {
            this.encodings.get().add(encoding);
            return this;
        }

        public Builder clearEncodings() {
            this.encodings.clear();
            return this;
        }
    }
}
