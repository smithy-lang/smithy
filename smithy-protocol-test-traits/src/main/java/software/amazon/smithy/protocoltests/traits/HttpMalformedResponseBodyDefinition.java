/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines the response expected by an HttpMalformedRequest test case.
 */
@SmithyUnstableApi
public final class HttpMalformedResponseBodyDefinition
        implements ToNode, ToSmithyBuilder<HttpMalformedResponseBodyDefinition> {

    private static final String ASSERTION = "assertion";
    private static final String CONTENTS = "contents";
    private static final String MEDIA_TYPE = "mediaType";
    private static final String MESSAGE_REGEX = "messageRegex";

    private final String contents;
    private final String mediaType;
    private final String messageRegex;

    private HttpMalformedResponseBodyDefinition(Builder builder) {
        contents = builder.contents;
        mediaType = SmithyBuilder.requiredState(MEDIA_TYPE, builder.mediaType);
        messageRegex = builder.messageRegex;
    }

    public Optional<String> getContents() {
        return Optional.ofNullable(contents);
    }

    public String getMediaType() {
        return mediaType;
    }

    public Optional<String> getMessageRegex() {
        return Optional.ofNullable(messageRegex);
    }

    public static HttpMalformedResponseBodyDefinition fromNode(Node node) {
        HttpMalformedResponseBodyDefinition.Builder builder = builder();
        ObjectNode o = node.expectObjectNode();
        builder.mediaType(o.expectStringMember(MEDIA_TYPE).getValue());
        ObjectNode assertion = o.expectObjectMember(ASSERTION);
        assertion.getStringMember(CONTENTS).ifPresent(stringNode -> builder.contents(stringNode.getValue()));
        assertion.getStringMember(MESSAGE_REGEX).ifPresent(stringNode -> builder.messageRegex(stringNode.getValue()));
        return builder.build();
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withMember(MEDIA_TYPE, getMediaType())
                .withMember(ASSERTION,
                        ObjectNode.objectNodeBuilder()
                                .withOptionalMember(CONTENTS, getContents().map(StringNode::from))
                                .withOptionalMember(MESSAGE_REGEX, getMessageRegex().map(StringNode::from))
                                .build())
                .build();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder()
                .mediaType(getMediaType());
        getContents().ifPresent(builder::mediaType);
        getMessageRegex().ifPresent(builder::messageRegex);
        return builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create a HttpMalformedResponseBodyDefinition.
     */
    public static final class Builder implements SmithyBuilder<HttpMalformedResponseBodyDefinition> {

        private String contents;
        private String mediaType;
        private String messageRegex;

        private Builder() {}

        public Builder contents(String contents) {
            this.contents = contents;
            return this;
        }

        public Builder mediaType(String mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public Builder messageRegex(String messageRegex) {
            this.messageRegex = messageRegex;
            return this;
        }

        @Override
        public HttpMalformedResponseBodyDefinition build() {
            return new HttpMalformedResponseBodyDefinition(this);
        }
    }
}
