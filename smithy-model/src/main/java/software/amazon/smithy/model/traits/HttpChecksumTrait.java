/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.traits;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that an operation supports checksum validation.
 * Contains request and response members that define the checksum behavior
 * for operations HTTP Request and HTTP Response respectively.
 */
public final class HttpChecksumTrait extends AbstractTrait implements ToSmithyBuilder<HttpChecksumTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#httpChecksum");

    private static final String REQUEST_PROPERTY = "request";
    private static final String RESPONSE_PROPERTY = "response";

    private final HttpChecksumProperties requestProperty;
    private final HttpChecksumProperties responseProperty;

    private HttpChecksumTrait(HttpChecksumTrait.Builder builder) {
        super(ID, builder.sourceLocation);
        this.requestProperty = builder.requestProperty;
        this.responseProperty = builder.responseProperty;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public SmithyBuilder<HttpChecksumTrait> toBuilder() {
        return new Builder()
                .sourceLocation(getSourceLocation())
                .requestProperty(requestProperty)
                .responseProperty(responseProperty);
    }

    /**
     * Gets request property defined within the HttpChecksum trait.
     *
     * @return checksum properties for request.
     */
    public HttpChecksumProperties getRequestProperty() {
        return requestProperty;
    }

    /**
     * Gets response property defined within the HttpChecksum trait.
     *
     * @return checksum properties for response.
     */
    public HttpChecksumProperties getResponseProperty() {
        return responseProperty;
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = ObjectNode.objectNodeBuilder();
        builder.sourceLocation(getSourceLocation());

        if (requestProperty != null) {
            builder.withMember(REQUEST_PROPERTY, requestProperty.toNode());
        }
        if (responseProperty != null) {
            builder.withMember(RESPONSE_PROPERTY, responseProperty.toNode());
        }
        return builder.build();
    }

    public static final class Builder extends AbstractTraitBuilder<HttpChecksumTrait, Builder> {
        private HttpChecksumProperties requestProperty;
        private HttpChecksumProperties responseProperty;

        private Builder() {
        }

        @Override
        public HttpChecksumTrait build() {
            return new HttpChecksumTrait(this);
        }

        public Builder requestProperty(HttpChecksumProperties requestProperty) {
            this.requestProperty = requestProperty;
            return this;
        }

        public Builder responseProperty(HttpChecksumProperties responseProperty) {
            this.responseProperty = responseProperty;
            return this;
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        public Trait createTrait(ShapeId target, Node value) {
            ObjectNode node = value.expectObjectNode();
            Builder builder = builder().sourceLocation(value);

            Optional<ObjectNode> requestNode = node.getObjectMember(REQUEST_PROPERTY);
            if (requestNode.isPresent()) {
                builder.requestProperty = HttpChecksumProperties.fromNode(requestNode.get());
            }

            Optional<ObjectNode> responseNode = node.getObjectMember(RESPONSE_PROPERTY);
            if (responseNode.isPresent()) {
                builder.responseProperty = HttpChecksumProperties.fromNode(responseNode.get());
            }

            return builder.build();
        }
    }
}
