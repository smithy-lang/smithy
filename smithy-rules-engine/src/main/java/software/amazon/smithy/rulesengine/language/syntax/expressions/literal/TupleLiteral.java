/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.expressions.literal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;

/**
 * A tuple value, containing a list of other literals.
 */
public final class TupleLiteral extends Literal {
    private final List<Literal> members;

    TupleLiteral(List<Literal> members, FromSourceLocation sourceLocation) {
        super(sourceLocation);
        this.members = members;
    }

    /**
     * Gets the list of literals contained within this tuple.
     *
     * @return the list of literals.
     */
    public List<Literal> members() {
        return members;
    }

    @Override
    public <T> T accept(LiteralVisitor<T> visitor) {
        return visitor.visitTuple(members);
    }

    @Override
    public Optional<List<Literal>> asTupleLiteral() {
        return Optional.of(members);
    }

    @Override
    public int hashCode() {
        return Objects.hash(members);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        TupleLiteral that = (TupleLiteral) obj;
        return Objects.equals(this.members, that.members);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        List<String> memberStrings = new ArrayList<>();
        for (Literal member : members) {
            memberStrings.add(member.toString());
        }
        builder.append(String.join(", ", memberStrings));
        return builder.append("]").toString();
    }

    @Override
    public Node toNode() {
        ArrayNode.Builder builder = ArrayNode.builder();
        for (Literal member : members) {
            builder.withValue(member.toNode());
        }
        return builder.build();
    }
}
