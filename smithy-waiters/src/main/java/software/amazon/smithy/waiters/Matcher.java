/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.waiters;

import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;

/**
 * Determines if an acceptor matches the current state of a resource.
 */
public abstract class Matcher<T> implements ToNode {

    // A sealed constructor.
    private Matcher() {}

    /**
     * Visits the variants of the Matcher union type.
     *
     * @param <T> Type of value to return from the visitor.
     */
    public interface Visitor<T> {
        T visitOutput(OutputMember outputPath);

        T visitInputOutput(InputOutputMember inputOutputPath);

        T visitSuccess(SuccessMember success);

        T visitErrorType(ErrorTypeMember errorType);

        T visitUnknown(UnknownMember unknown);
    }

    /**
     * Gets the value of the set matcher variant.
     *
     * @return Returns the set variant's value.
     */
    public abstract T getValue();

    /**
     * Gets the member name of the matcher.
     *
     * @return Returns the set member name.
     */
    public abstract String getMemberName();

    /**
     * Visits the Matcher union type.
     *
     * @param visitor Visitor to apply.
     * @param <U> The type returned by the visitor.
     * @return Returns the return value of the visitor.
     */
    public abstract <U> U accept(Visitor<U> visitor);

    @Override
    public final int hashCode() {
        return Objects.hash(getMemberName(), getValue());
    }

    @Override
    public final boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Matcher)) {
            return false;
        } else {
            Matcher<?> other = (Matcher<?>) o;
            return getMemberName().equals(other.getMemberName()) && getValue().equals(other.getValue());
        }
    }

    /**
     * Creates a {@code Matcher} from a {@link Node}.
     *
     * @param node {@code Node} to create a {@code Matcher} from.
     * @return Returns the create {@code Matcher}.
     * @throws ExpectationNotMetException if the given {@code node} is invalid.
     */
    public static Matcher<?> fromNode(Node node) {
        ObjectNode value = node.expectObjectNode();
        if (value.size() != 1) {
            throw new ExpectationNotMetException("Union value must have exactly one value set", node);
        }

        Map.Entry<StringNode, Node> entry = value.getMembers().entrySet().iterator().next();
        String entryKey = entry.getKey().getValue();
        Node entryValue = entry.getValue();

        switch (entryKey) {
            case "output":
                return new OutputMember(PathMatcher.fromNode(entryValue));
            case "inputOutput":
                return new InputOutputMember(PathMatcher.fromNode(entryValue));
            case "success":
                return new SuccessMember(entryValue.expectBooleanNode().getValue());
            case "errorType":
                return new ErrorTypeMember(entryValue.expectStringNode().getValue());
            default:
                return new UnknownMember(entryKey, entryValue);
        }
    }

    private abstract static class PathMatcherMember extends Matcher<PathMatcher> {
        private final String memberName;
        private final PathMatcher value;

        private PathMatcherMember(String memberName, PathMatcher value) {
            this.memberName = memberName;
            this.value = value;
        }

        @Override
        public final String getMemberName() {
            return memberName;
        }

        @Override
        public final PathMatcher getValue() {
            return value;
        }

        @Override
        public final Node toNode() {
            return Node.objectNode().withMember(getMemberName(), value.toNode());
        }
    }

    public static final class OutputMember extends PathMatcherMember {
        public OutputMember(PathMatcher value) {
            super("output", value);
        }

        @Override
        public <U> U accept(Visitor<U> visitor) {
            return visitor.visitOutput(this);
        }
    }

    public static final class InputOutputMember extends PathMatcherMember {
        public InputOutputMember(PathMatcher value) {
            super("inputOutput", value);
        }

        @Override
        public <U> U accept(Visitor<U> visitor) {
            return visitor.visitInputOutput(this);
        }
    }

    /**
     * Matches if an operation returns an error, and the error matches the
     * expected error type.
     */
    public static final class ErrorTypeMember extends Matcher<String> {
        private final String value;

        public ErrorTypeMember(String value) {
            this.value = value;
        }

        @Override
        public String getMemberName() {
            return "errorType";
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public Node toNode() {
            return Node.objectNode().withMember(getMemberName(), Node.from(value));
        }

        @Override
        public <U> U accept(Visitor<U> visitor) {
            return visitor.visitErrorType(this);
        }
    }

    /**
     * When set to true, matches when a call returns a success response.
     * When set to false, matches when a call fails with any error.
     */
    public static final class SuccessMember extends Matcher<Boolean> {
        private final boolean value;

        public SuccessMember(boolean value) {
            this.value = value;
        }

        @Override
        public String getMemberName() {
            return "success";
        }

        @Override
        public Boolean getValue() {
            return value;
        }

        @Override
        public Node toNode() {
            return Node.objectNode().withMember(getMemberName(), Node.from(value));
        }

        @Override
        public <U> U accept(Visitor<U> visitor) {
            return visitor.visitSuccess(this);
        }
    }

    /**
     * Represents an union value.
     */
    public static final class UnknownMember extends Matcher<Node> {
        private final String key;
        private final Node value;

        public UnknownMember(String key, Node value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String getMemberName() {
            return key;
        }

        @Override
        public Node getValue() {
            return value;
        }

        @Override
        public Node toNode() {
            return Node.objectNode().withMember(getMemberName(), getValue());
        }

        @Override
        public <U> U accept(Visitor<U> visitor) {
            return visitor.visitUnknown(this);
        }
    }
}
