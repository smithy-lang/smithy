/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import java.util.Map;
import java.util.Set;
import software.amazon.smithy.jmespath.ast.AndExpression;
import software.amazon.smithy.jmespath.ast.BinaryExpression;
import software.amazon.smithy.jmespath.ast.ComparatorExpression;
import software.amazon.smithy.jmespath.ast.CurrentExpression;
import software.amazon.smithy.jmespath.ast.ExpressionTypeExpression;
import software.amazon.smithy.jmespath.ast.FieldExpression;
import software.amazon.smithy.jmespath.ast.FilterProjectionExpression;
import software.amazon.smithy.jmespath.ast.FlattenExpression;
import software.amazon.smithy.jmespath.ast.FunctionExpression;
import software.amazon.smithy.jmespath.ast.IndexExpression;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.jmespath.ast.MultiSelectHashExpression;
import software.amazon.smithy.jmespath.ast.MultiSelectListExpression;
import software.amazon.smithy.jmespath.ast.NotExpression;
import software.amazon.smithy.jmespath.ast.ObjectProjectionExpression;
import software.amazon.smithy.jmespath.ast.OrExpression;
import software.amazon.smithy.jmespath.ast.ProjectionExpression;
import software.amazon.smithy.jmespath.ast.SliceExpression;
import software.amazon.smithy.jmespath.ast.Subexpression;

/**
 * Serializes the JMESPath expression AST back to a JMESPath expression.
 */
public final class ExpressionSerializer {

    /**
     * Serialize the given JMESPath expression to a string.
     *
     * @param expression JMESPath expression to serialize.
     * @return Returns the serialized expression.
     */
    public String serialize(JmespathExpression expression) {
        StringBuilder builder = new StringBuilder();
        Visitor visitor = new Visitor(builder);
        expression.accept(visitor);
        return builder.toString();
    }

    private static final class Visitor implements ExpressionVisitor<Void> {
        private final StringBuilder builder;

        Visitor(StringBuilder builder) {
            this.builder = builder;
        }

        @Override
        public Void visitComparator(ComparatorExpression expression) {
            // e.g.: foo > bar
            expression.getLeft().accept(this);
            builder.append(' ');
            builder.append(expression.getComparator());
            builder.append(' ');
            expression.getRight().accept(this);
            return null;
        }

        @Override
        public Void visitCurrentNode(CurrentExpression expression) {
            builder.append('@');
            return null;
        }

        @Override
        public Void visitExpressionType(ExpressionTypeExpression expression) {
            // e.g.: &(foo.bar)
            builder.append("&(");
            expression.getExpression().accept(this);
            builder.append(')');
            return null;
        }

        @Override
        public Void visitFlatten(FlattenExpression expression) {
            // e.g.: foo[]
            expression.getExpression().accept(this);
            builder.append("[]");
            return null;
        }

        @Override
        public Void visitFunction(FunctionExpression expression) {
            // e.g.: some_function(@, foo)
            builder.append(expression.getName());
            builder.append('(');
            for (int i = 0; i < expression.getArguments().size(); i++) {
                expression.getArguments().get(i).accept(this);
                if (i < expression.getArguments().size() - 1) {
                    builder.append(", ");
                }
            }
            builder.append(')');
            return null;
        }

        @Override
        public Void visitField(FieldExpression expression) {
            // Always quote fields: "foo"
            builder.append('"');
            builder.append(sanitizeString(expression.getName(), false));
            builder.append('"');
            return null;
        }

        private String sanitizeString(String str, boolean escapeBackticks) {
            String result = str.replace("\\", "\\\\").replace("\"", "\\\"");
            if (escapeBackticks) {
                result = result.replace("`", "\\`");
            }
            return result;
        }

        @Override
        public Void visitIndex(IndexExpression expression) {
            // e.g.: [1]
            builder.append('[').append(expression.getIndex()).append(']');
            return null;
        }

        @Override
        public Void visitLiteral(LiteralExpression expression) {
            // e.g.: `[true]`
            visitLiteral(expression, false);
            return null;
        }

        private void visitLiteral(LiteralExpression expression, boolean nestedInsideLiteral) {
            if (!nestedInsideLiteral) {
                builder.append('`');
            }

            switch (expression.getType()) {
                case NUMBER:
                    builder.append(expression.expectNumberValue());
                    break;
                case NULL:
                case ANY: // treat "any" like null
                    builder.append("null");
                    break;
                case ARRAY:
                    builder.append('[');
                    int ai = 0;
                    for (Object value : expression.expectArrayValue()) {
                        LiteralExpression exp = LiteralExpression.from(value);
                        visitLiteral(exp, true);
                        if (ai++ < expression.expectArrayValue().size() - 1) {
                            builder.append(", ");
                        }
                    }
                    builder.append(']');
                    break;
                case OBJECT:
                    builder.append('{');
                    int oi = 0;
                    Set<Map.Entry<String, Object>> objectEntries = expression.expectObjectValue().entrySet();
                    for (Map.Entry<String, Object> objectEntry : objectEntries) {
                        builder.append('"')
                                .append(sanitizeString(objectEntry.getKey(), true))
                                .append("\": ");
                        LiteralExpression exp = LiteralExpression.from(objectEntry.getValue());
                        visitLiteral(exp, true);
                        if (oi++ < objectEntries.size() - 1) {
                            builder.append(", ");
                        }
                    }
                    builder.append('}');
                    break;
                case STRING:
                    builder.append('"')
                            .append(sanitizeString(expression.expectStringValue(), true))
                            .append('"');
                    break;
                case BOOLEAN:
                    builder.append(expression.expectBooleanValue());
                    break;
                case EXPRESSION:
                    // fall-through
                default:
                    throw new JmespathException("Unable to serialize literal runtime value: " + expression);
            }

            if (!nestedInsideLiteral) {
                builder.append('`');
            }
        }

        @Override
        public Void visitMultiSelectList(MultiSelectListExpression expression) {
            // e.g.: [foo, bar[].baz]
            builder.append('[');
            for (int i = 0; i < expression.getExpressions().size(); i++) {
                expression.getExpressions().get(i).accept(this);
                if (i < expression.getExpressions().size() - 1) {
                    builder.append(", ");
                }
            }
            builder.append(']');
            return null;
        }

        @Override
        public Void visitMultiSelectHash(MultiSelectHashExpression expression) {
            // e.g.: {foo: `true`, bar: bar}
            builder.append('{');
            int i = 0;
            for (Map.Entry<String, JmespathExpression> entry : expression.getExpressions().entrySet()) {
                builder.append('"').append(sanitizeString(entry.getKey(), false)).append("\": ");
                entry.getValue().accept(this);
                if (i < expression.getExpressions().entrySet().size() - 1) {
                    builder.append(", ");
                }
                i++;
            }
            builder.append('}');
            return null;
        }

        @Override
        public Void visitAnd(AndExpression expression) {
            // e.g.: (a && b)
            builder.append('(');
            expression.getLeft().accept(this);
            builder.append(" && ");
            expression.getRight().accept(this);
            builder.append(')');
            return null;
        }

        @Override
        public Void visitOr(OrExpression expression) {
            // e.g.: (a || b)
            builder.append('(');
            expression.getLeft().accept(this);
            builder.append(" || ");
            expression.getRight().accept(this);
            builder.append(')');
            return null;
        }

        @Override
        public Void visitNot(NotExpression expression) {
            // e.g.: !(foo.bar)
            builder.append("!(");
            expression.getExpression().accept(this);
            builder.append(')');
            return null;
        }

        @Override
        public Void visitProjection(ProjectionExpression expression) {
            if (!(expression.getLeft() instanceof CurrentExpression)) {
                expression.getLeft().accept(this);
            }

            // Without this check, [::1].a would be reserialized into [::-1][*]."a",
            // which is equivalent, but convoluted.
            if (!(expression.getLeft() instanceof SliceExpression)) {
                // Flatten expressions, when parsed, inherently create a projection. Unroll
                // that here.
                if (!(expression.getLeft() instanceof FlattenExpression)) {
                    builder.append("[*]");
                }
            }

            // Avoid unnecessary addition of extracting the current node.
            // We add the current node in projections if there's no RHS.
            if (!(expression.getRight() instanceof CurrentExpression)) {
                // Add a "." if the right hand side needs it.
                if (rhsNeedsDot(expression.getRight())) {
                    builder.append('.');
                }
                expression.getRight().accept(this);
            }

            return null;
        }

        @Override
        public Void visitFilterProjection(FilterProjectionExpression expression) {
            // e.g.: foo[?bar == `10`].baz
            // Don't emit a pointless the current node select.
            if (!(expression.getLeft() instanceof CurrentExpression)) {
                expression.getLeft().accept(this);
            }

            builder.append("[?");
            expression.getComparison().accept(this);
            builder.append(']');

            // Avoid unnecessary addition of extracting the current node.
            if (!(expression.getRight() instanceof CurrentExpression)) {
                // Add a "." if the right hand side needs it.
                if (rhsNeedsDot(expression.getRight())) {
                    builder.append('.');
                }
                expression.getRight().accept(this);
            }

            return null;
        }

        @Override
        public Void visitObjectProjection(ObjectProjectionExpression expression) {
            // Avoid the unnecessary "@.*" by just emitting "*".
            if (!(expression.getLeft() instanceof CurrentExpression)) {
                expression.getLeft().accept(this);
                builder.append(".*");
            } else {
                builder.append('*');
            }

            // Avoid unnecessary addition of extracting the current node.
            // We add the current node in projections if there's no RHS.
            if (!(expression.getRight() instanceof CurrentExpression)) {
                // Add a "." if the right hand side needs it.
                if (rhsNeedsDot(expression.getRight())) {
                    builder.append('.');
                }
                expression.getRight().accept(this);
            }

            return null;
        }

        @Override
        public Void visitSlice(SliceExpression expression) {
            // e.g.: [0::1], [0:1:2], etc
            builder.append('[');
            expression.getStart().ifPresent(builder::append);
            builder.append(':');
            expression.getStop().ifPresent(builder::append);
            builder.append(':');
            builder.append(expression.getStep());
            builder.append(']');
            return null;
        }

        @Override
        public Void visitSubexpression(Subexpression expression) {
            // e.g.: "foo"."bar", "foo" | "bar"
            expression.getLeft().accept(this);

            if (expression.isPipe()) {
                // pipe has a different precedence than dot, so this is important.
                builder.append(" | ");
            } else if (rhsNeedsDot(expression.getRight())) {
                builder.append('.');
            }

            expression.getRight().accept(this);
            return null;
        }

        // These expression need to be preceded by a "." in a binary expression.
        private boolean rhsNeedsDot(JmespathExpression expression) {
            return expression instanceof FieldExpression
                    || expression instanceof MultiSelectHashExpression
                    || expression instanceof MultiSelectListExpression
                    || expression instanceof ObjectProjectionExpression
                    || expression instanceof FunctionExpression
                    || (expression instanceof BinaryExpression
                            && rhsNeedsDot(((BinaryExpression) expression).getLeft()));
        }
    }
}
