/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.expressions.literal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;

/**
 * A record literal value, containing a map of identifiers to other literals.
 */
public final class RecordLiteral extends Literal {
    private final Map<Identifier, Literal> members;

    RecordLiteral(Map<Identifier, Literal> members, FromSourceLocation sourceLocation) {
        super(sourceLocation);
        this.members = Collections.unmodifiableMap(new LinkedHashMap<>(members));
    }

    /**
     * Gets the map of identifiers to literals contained within this record.
     *
     * @return the map of identifiers to literals.
     */
    public Map<Identifier, Literal> members() {
        return members;
    }

    @Override
    public <T> T accept(LiteralVisitor<T> visitor) {
        return visitor.visitRecord(members);
    }

    @Override
    public Optional<Map<Identifier, Literal>> asRecordLiteral() {
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
        RecordLiteral that = (RecordLiteral) obj;
        return Objects.equals(this.members, that.members);
    }

    @Override
    public String toString() {
        return members.toString();
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = ObjectNode.builder();
        members.forEach((k, v) -> builder.withMember(k.toString(), v.toNode()));
        return builder.build();
    }
}
