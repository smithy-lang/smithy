/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.waiters;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.Tagged;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines an individual operation waiter.
 */
public final class Waiter implements Tagged, ToNode, ToSmithyBuilder<Waiter> {

    private static final String DOCUMENTATION = "documentation";
    private static final String ACCEPTORS = "acceptors";
    private static final String MIN_DELAY = "minDelay";
    private static final String MAX_DELAY = "maxDelay";
    private static final String DEPRECATED = "deprecated";
    private static final String TAGS = "tags";
    private static final int DEFAULT_MIN_DELAY = 2;
    private static final int DEFAULT_MAX_DELAY = 120;
    private static final Set<String> KEYS = SetUtils.of(
            DOCUMENTATION,
            ACCEPTORS,
            MIN_DELAY,
            MAX_DELAY,
            TAGS,
            DEPRECATED);

    private final String documentation;
    private final List<Acceptor> acceptors;
    private final int minDelay;
    private final int maxDelay;
    private final boolean deprecated;
    private final List<String> tags;

    private Waiter(Builder builder) {
        this.documentation = builder.documentation;
        this.acceptors = builder.acceptors.copy();
        this.minDelay = builder.minDelay;
        this.maxDelay = builder.maxDelay;
        this.deprecated = builder.deprecated;
        this.tags = builder.tags.copy();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .documentation(getDocumentation().orElse(null))
                .acceptors(getAcceptors())
                .minDelay(getMinDelay())
                .maxDelay(getMaxDelay())
                .tags(tags)
                .deprecated(deprecated);
    }

    /**
     * Create a {@code Waiter} from a {@link Node}.
     *
     * @param node {@code Node} to create the {@code Waiter} from.
     * @return Returns the created {@code Waiter}.
     * @throws ExpectationNotMetException if the given {@code node} is invalid.
     */
    public static Waiter fromNode(Node node) {
        ObjectNode value = node.expectObjectNode().warnIfAdditionalProperties(KEYS);
        Builder builder = builder();
        value.getStringMember(DOCUMENTATION).map(StringNode::getValue).ifPresent(builder::documentation);

        for (Node entry : value.expectArrayMember(ACCEPTORS).getElements()) {
            builder.addAcceptor(Acceptor.fromNode(entry));
        }

        value.getNumberMember(MIN_DELAY).map(NumberNode::getValue).map(Number::intValue).ifPresent(builder::minDelay);
        value.getNumberMember(MAX_DELAY).map(NumberNode::getValue).map(Number::intValue).ifPresent(builder::maxDelay);

        builder.deprecated(value.getBooleanMemberOrDefault(DEPRECATED));
        value.getMember(TAGS).ifPresent(tags -> builder.tags(Node.loadArrayOfString(TAGS, tags)));

        return builder.build();
    }

    /**
     * Gets the documentation of the waiter.
     *
     * @return Return the optional documentation.
     */
    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    /**
     * Gets the list of {@link Acceptor}s.
     *
     * @return Returns the acceptors of the waiter.
     */
    public List<Acceptor> getAcceptors() {
        return acceptors;
    }

    /**
     * Gets the minimum amount of time to wait between retries
     * in seconds.
     *
     * @return Gets the minimum retry wait time in seconds.
     */
    public int getMinDelay() {
        return minDelay;
    }

    /**
     * Gets the maximum amount of time allowed to wait between
     * retries in seconds.
     *
     * @return Gets the maximum retry wait time in seconds.
     */
    public int getMaxDelay() {
        return maxDelay;
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    /**
     * Checks if the waiter is deprecated.
     *
     * @return Returns true if the waiter is deprecated.
     */
    public boolean isDeprecated() {
        return deprecated;
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withOptionalMember(DOCUMENTATION, getDocumentation().map(Node::from))
                .withMember(ACCEPTORS, getAcceptors().stream().map(Acceptor::toNode).collect(ArrayNode.collect()));

        // Don't serialize default values for minDelay and maxDelay.
        if (minDelay != DEFAULT_MIN_DELAY) {
            builder.withMember(MIN_DELAY, minDelay);
        }
        if (maxDelay != DEFAULT_MAX_DELAY) {
            builder.withMember(MAX_DELAY, maxDelay);
        }

        if (isDeprecated()) {
            builder.withMember(DEPRECATED, true);
        }

        if (!tags.isEmpty()) {
            builder.withMember(TAGS, Node.fromStrings(tags));
        }

        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Waiter)) {
            return false;
        }

        Waiter waiter = (Waiter) o;
        return minDelay == waiter.minDelay
                && maxDelay == waiter.maxDelay
                && Objects.equals(documentation, waiter.documentation)
                && acceptors.equals(waiter.acceptors)
                && tags.equals(waiter.tags)
                && deprecated == waiter.deprecated;
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentation, acceptors, minDelay, maxDelay, deprecated, tags);
    }

    public static final class Builder implements SmithyBuilder<Waiter> {

        private String documentation;
        private final BuilderRef<List<Acceptor>> acceptors = BuilderRef.forList();
        private int minDelay = DEFAULT_MIN_DELAY;
        private int maxDelay = DEFAULT_MAX_DELAY;
        private boolean deprecated = false;
        private final BuilderRef<List<String>> tags = BuilderRef.forList();

        private Builder() {}

        @Override
        public Waiter build() {
            return new Waiter(this);
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder clearAcceptors() {
            this.acceptors.clear();
            return this;
        }

        public Builder acceptors(List<Acceptor> acceptors) {
            clearAcceptors();
            acceptors.forEach(this::addAcceptor);
            return this;
        }

        public Builder addAcceptor(Acceptor acceptor) {
            this.acceptors.get().add(Objects.requireNonNull(acceptor));
            return this;
        }

        public Builder minDelay(int minDelay) {
            this.minDelay = minDelay;
            return this;
        }

        public Builder maxDelay(int maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }

        public Builder clearTags() {
            this.tags.clear();
            return this;
        }

        public Builder tags(List<String> tags) {
            clearTags();
            tags.forEach(this::addTag);
            return this;
        }

        public Builder addTag(String tag) {
            this.tags.get().add(Objects.requireNonNull(tag));
            return this;
        }

        public Builder deprecated(boolean deprecated) {
            this.deprecated = deprecated;
            return this;
        }
    }
}
