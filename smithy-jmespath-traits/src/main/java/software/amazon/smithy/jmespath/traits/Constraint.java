/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.jmespath.traits;

import java.util.Optional;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyGenerated;
import software.amazon.smithy.utils.ToSmithyBuilder;

@SmithyGenerated
public final class Constraint implements ToNode, ToSmithyBuilder<Constraint> {
    private final String path;
    private final String description;

    private Constraint(Builder builder) {
        this.path = SmithyBuilder.requiredState("path", builder.path);
        this.description = builder.description;
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
            .withMember("path", Node.from(path))
            .withOptionalMember("description", getDescription().map(m -> Node.from(m)))
            .build();
    }

    /**
     * Creates a {@link Constraint} from a {@link Node}.
     *
     * @param node Node to create the Constraint from.
     * @return Returns the created Constraint.
     * @throws ExpectationNotMetException if the given Node is invalid.
     */
    public static Constraint fromNode(Node node) {
        Builder builder = builder();
        node.expectObjectNode()
            .expectStringMember("path", builder::path)
            .getStringMember("description", builder::description);

        return builder.build();
    }

    /**
     * JMESPath expression that must evaluate to true.
     */
    public String getPath() {
        return path;
    }

    /**
     * Description of the constraint. Used in error messages when violated.
     */
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    /**
     * Creates a builder used to build a {@link Constraint}.
     */
    public SmithyBuilder<Constraint> toBuilder() {
        return builder()
            .path(path)
            .description(description);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Constraint}.
     */
    public static final class Builder implements SmithyBuilder<Constraint> {
        private String path;
        private String description;

        private Builder() {}

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        @Override
        public Constraint build() {
            return new Constraint(this);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (!(other instanceof Constraint)) {
            return false;
        } else {
            Constraint b = (Constraint) other;
            return toNode().equals(b.toNode());
        }
    }

    @Override
    public int hashCode() {
        return toNode().hashCode();
    }
}
