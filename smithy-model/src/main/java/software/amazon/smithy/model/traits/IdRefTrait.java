/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that a string value must contain a valid shape ID.
 *
 * <p>{@code failWhenMissing} can be set to true to fail when the
 * shape ID contained in a string shape cannot be found in the result
 * set of the {@code selector} property. {@code failWhenMissing} defaults to
 * {@code false} when not set.
 *
 * <p>The {@code selector} property is used to query the model for all shapes
 * that match a shape selector. The value contained within the string shape
 * targeted by this trait MUST match one of the shapes returns by the shape
 * selector if {@code failWhenMissing} is set to true. {@code selector}
 * defaults to "*" when not set.
 */
public final class IdRefTrait extends AbstractTrait implements ToSmithyBuilder<IdRefTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#idRef");

    private static final String SELECTOR_MEMBER_ID = "selector";
    private static final String FAIL_WHEN_MISSING_MEMBER = "failWhenMissing";
    private static final String ERROR_MESSAGE = "errorMessage";

    private final Selector selector;
    private final boolean failWhenMissing;
    private final String errorMessage;

    private IdRefTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        selector = builder.selector;
        failWhenMissing = builder.failWhenMissing;
        errorMessage = builder.errorMessage;
    }

    public Selector getSelector() {
        return selector == null ? Selector.IDENTITY : selector;
    }

    public boolean failWhenMissing() {
        return failWhenMissing;
    }

    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation())
                .failWhenMissing(failWhenMissing)
                .selector(selector)
                .errorMessage(errorMessage);
    }

    @Override
    protected Node createNode() {
        ObjectNode result = Node.objectNode()
                .withOptionalMember(
                        SELECTOR_MEMBER_ID,
                        Optional.ofNullable(selector).map(Selector::toString).map(Node::from))
                .withOptionalMember(ERROR_MESSAGE, getErrorMessage().map(Node::from));
        if (failWhenMissing) {
            result = result.withMember(FAIL_WHEN_MISSING_MEMBER, Node.from(true));
        }
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractTraitBuilder<IdRefTrait, Builder> {
        private Selector selector;
        private boolean failWhenMissing;
        private String errorMessage;

        private Builder() {}

        public Builder failWhenMissing(boolean failWhenMissing) {
            this.failWhenMissing = failWhenMissing;
            return this;
        }

        public Builder selector(Selector selector) {
            this.selector = selector;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        @Override
        public IdRefTrait build() {
            return new IdRefTrait(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public IdRefTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            ObjectNode objectNode = value.expectObjectNode();
            objectNode.getStringMember(SELECTOR_MEMBER_ID)
                    .map(Selector::fromNode)
                    .ifPresent(builder::selector);
            objectNode.getBooleanMember(FAIL_WHEN_MISSING_MEMBER)
                    .map(BooleanNode::getValue)
                    .ifPresent(builder::failWhenMissing);
            objectNode.getStringMember(ERROR_MESSAGE)
                    .map(StringNode::getValue)
                    .ifPresent(builder::errorMessage);
            return builder.build();
        }
    }
}
