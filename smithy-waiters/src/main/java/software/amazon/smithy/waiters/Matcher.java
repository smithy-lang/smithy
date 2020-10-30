/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.waiters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import software.amazon.smithy.model.node.ArrayNode;
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

        T visitInput(InputMember inputPath);

        T visitSuccess(SuccessMember success);

        T visitErrorType(ErrorTypeMember errorType);

        T visitAnd(AndMember and);

        T visitOr(OrMember or);

        T visitNot(NotMember not);

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
            case "input":
                return new InputMember(PathMatcher.fromNode(entryValue));
            case "output":
                return new OutputMember(PathMatcher.fromNode(entryValue));
            case "success":
                return new SuccessMember(entryValue.expectBooleanNode().getValue());
            case "errorType":
                return new ErrorTypeMember(entryValue.expectStringNode().getValue());
            case "and":
                return MatcherList.fromNode(entryValue, AndMember::new);
            case "or":
                return MatcherList.fromNode(entryValue, OrMember::new);
            case "not":
                return new NotMember(fromNode(entryValue));
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

    public static final class InputMember extends PathMatcherMember {
        public InputMember(PathMatcher value) {
            super("input", value);
        }

        @Override
        public <U> U accept(Visitor<U> visitor) {
            return visitor.visitInput(this);
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
     * Represents an union union value.
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

    private abstract static class MatcherList extends Matcher<List<Matcher<?>>> {
        private final List<Matcher<?>> values;
        private final String memberName;

        private MatcherList(String memberName, List<Matcher<?>> values) {
            this.memberName = memberName;
            this.values = values;
        }

        private static <T> T fromNode(Node node, Function<List<Matcher<?>>, T> constructor) {
            ArrayNode values = node.expectArrayNode();
            List<Matcher<?>> result = new ArrayList<>();
            for (ObjectNode element : values.getElementsAs(ObjectNode.class)) {
                result.add(Matcher.fromNode(element));
            }
            return constructor.apply(result);
        }

        @Override
        public String getMemberName() {
            return memberName;
        }

        public List<Matcher<?>> getValue() {
            return values;
        }

        @Override
        public final Node toNode() {
            return Node.objectNode()
                    .withMember(getMemberName(), values.stream().map(Matcher::toNode).collect(ArrayNode.collect()));
        }
    }

    /**
     * Matches if all matchers in the list are matches.
     */
    public static final class AndMember extends MatcherList {
        public AndMember(List<Matcher<?>> matchers) {
            super("and", matchers);
        }

        @Override
        public <U> U accept(Visitor<U> visitor) {
            return visitor.visitAnd(this);
        }
    }

    /**
     * Matches if any matchers in the list are matches.
     */
    public static final class OrMember extends MatcherList {
        public OrMember(List<Matcher<?>> matchers) {
            super("or", matchers);
        }

        @Override
        public <U> U accept(Visitor<U> visitor) {
            return visitor.visitOr(this);
        }
    }

    /**
     * Matches if the given matcher is not a match.
     */
    public static final class NotMember extends Matcher<Matcher<?>> {
        private final Matcher<?> value;

        public NotMember(Matcher<?> value) {
            this.value = value;
        }

        @Override
        public String getMemberName() {
            return "not";
        }

        @Override
        public Matcher<?> getValue() {
            return value;
        }

        @Override
        public Node toNode() {
            return Node.objectNode().withMember(getMemberName(), getValue());
        }

        @Override
        public <U> U accept(Visitor<U> visitor) {
            return visitor.visitNot(this);
        }
    }
}
