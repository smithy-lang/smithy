/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax;

import static software.amazon.smithy.rulesengine.language.RulesComponentBuilder.javaLocation;

import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A name used to identify a component of a rule-set.
 */
@SmithyUnstableApi
public final class Identifier implements FromSourceLocation, ToNode {
    private final StringNode name;
    private final SourceLocation sourceLocation;

    private Identifier(StringNode name) {
        this.name = name;
        sourceLocation = name.getSourceLocation();
    }

    /**
     * Creates an {@link Identifier} from the given name.
     *
     * @param name the name of the identifier to create.
     * @return the created Identifier.
     */
    public static Identifier of(String name) {
        return new Identifier(new StringNode(name, javaLocation()));
    }

    /**
     * Creates an {@link Identifier} from the given name.
     *
     * @param name the name of the identifier to create.
     * @return the created Identifier.
     */
    public static Identifier of(StringNode name) {
        return new Identifier(name);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    /**
     * Gets the name of this identifier.
     *
     * @return a node containing the name of the identifier.
     */
    public StringNode getName() {
        return name;
    }

    @Override
    public Node toNode() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        Identifier that = (Identifier) obj;
        return Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name.getValue();
    }
}
