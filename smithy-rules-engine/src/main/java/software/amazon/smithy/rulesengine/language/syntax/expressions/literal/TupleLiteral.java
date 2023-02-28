/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.language.syntax.expressions.literal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;

public final class TupleLiteral extends Literal {
    private final List<Literal> members;

    TupleLiteral(List<Literal> members, FromSourceLocation sourceLocation) {
        super(sourceLocation);
        this.members = members;
    }

    public List<Literal> members() {
        return members;
    }

    @Override
    public <T> T accept(LiteralVisitor<T> visitor) {
        return visitor.visitTuple(members);
    }

    @Override
    public Optional<List<Literal>> asTuple() {
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
