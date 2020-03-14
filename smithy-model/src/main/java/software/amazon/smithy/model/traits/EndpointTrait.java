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

import static java.lang.String.format;
import static software.amazon.smithy.model.pattern.SmithyPattern.Segment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.pattern.InvalidPatternException;
import software.amazon.smithy.model.pattern.SmithyPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines custom endpoint bindings of an operation.
 */
public final class EndpointTrait extends AbstractTrait implements ToSmithyBuilder<EndpointTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#endpoint");

    private final SmithyPattern hostPrefix;

    private EndpointTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        String hostPrefix = Objects.requireNonNull(builder.hostPrefix, "hostPrefix not set");

        StringTokenizer tokenizer = new StringTokenizer(hostPrefix, "{}", true);
        List<Segment> segments = new ArrayList<>();
        int position = 0;
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.equals("{")) {
                if (!segments.isEmpty() && segments.get(segments.size() - 1).isLabel()) {
                    throw new InvalidPatternException(
                            format("Host labels must not be adjacent in a pattern. Found %s", hostPrefix));
                }
                // If we found a label, grab the content and the close brace
                for (int i = 0; i < 2; i++) {
                    if (tokenizer.hasMoreTokens()) {
                        token += tokenizer.nextToken();
                    } else {
                        throw new InvalidPatternException("Unclosed label found in pattern at: ." + position);
                    }
                }
            } else if (token.equals("}")) {
                throw new InvalidPatternException("Literal segments must not contain `}`. Found at: " + position);
            }
            segments.add(Segment.parse(token, position));
            position += token.length();
        }

        this.hostPrefix = SmithyPattern.builder()
                .allowsGreedyLabels(false)
                .segments(segments)
                .pattern(hostPrefix)
                .build();
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            EndpointTrait.Builder builder = builder().sourceLocation(value);
            ObjectNode objectNode = value.expectObjectNode();
            builder.hostPrefix(objectNode.expectStringMember("hostPrefix").getValue());
            return builder.build();
        }
    }

    public SmithyPattern getHostPrefix() {
        return hostPrefix;
    }

    /**
     * @return Returns a builder used to create an Endpoint trait.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder().hostPrefix(hostPrefix.toString());
    }

    @Override
    protected Node createNode() {
        return new ObjectNode(Collections.emptyMap(), getSourceLocation())
                .withMember("hostPrefix", Node.from(hostPrefix.toString()));
    }

    public static final class Builder extends AbstractTraitBuilder<EndpointTrait, Builder> {
        private String hostPrefix;

        private Builder() {}

        public Builder hostPrefix(String hostPrefix) {
            this.hostPrefix = hostPrefix;
            return this;
        }

        @Override
        public EndpointTrait build() {
            return new EndpointTrait(this);
        }
    }
}
