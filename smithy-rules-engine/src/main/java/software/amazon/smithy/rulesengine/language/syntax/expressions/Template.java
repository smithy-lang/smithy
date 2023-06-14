/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.expressions;

import static software.amazon.smithy.rulesengine.language.error.RuleError.context;
import static software.amazon.smithy.rulesengine.language.syntax.expressions.Expression.parseShortform;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.TypeCheck;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Template represents a "Template Literal". This is a literal string within the rules language. A template
 * can contain 0 or more dynamic sections. The dynamic sections use getAttr short-form:
 * <p>
 * `https://{Region}.{partition#dnsSuffix}`
 * --------          ------------
 * |                   |
 * Dynamic            getAttr short form
 */
@SmithyUnstableApi
public final class Template implements FromSourceLocation, ToNode {
    private final SourceLocation sourceLocation;
    private final List<Part> parts;
    private final String value;

    public Template(StringNode template) {
        sourceLocation = SmithyBuilder.requiredState("source", template.getSourceLocation());
        value = template.getValue();
        parts = context("when parsing template", template, () -> parseTemplate(template.getValue(), template));
    }

    public static Template fromString(String s) {
        return new Template(StringNode.from(s));
    }

    private List<Part> parseTemplate(String template, FromSourceLocation context) throws InnerParseError {
        List<Part> out = new ArrayList<>();
        Optional<Integer> templateStart = Optional.empty();
        int depth = 0;
        int templateEnd = 0;
        for (int i = 0; i < template.length(); i++) {
            if (template.substring(i).startsWith("{{")) {
                i++;
                continue;
            }
            if (template.substring(i).startsWith("}}")) {
                i++;
                continue;
            }

            if (template.charAt(i) == '{') {
                if (depth == 0) {
                    if (templateEnd != i) {
                        out.add(Literal.unescape(template.substring(templateEnd, i)));
                    }
                    templateStart = Optional.of(i + 1);
                }
                depth++;
            }

            if (template.charAt(i) == '}') {
                depth--;
                if (depth < 0) {
                    throw new InnerParseError("unmatched `}` in template");
                }
                if (depth == 0) {
                    out.add(Dynamic.parse(template.substring(templateStart.get(), i), context));
                    templateStart = Optional.empty();
                }
                templateEnd = i + 1;
            }
        }
        if (depth != 0) {
            throw new InnerParseError("unmatched `{` in template");
        }
        if (templateEnd < template.length()) {
            out.add(Literal.unescape(template.substring(templateEnd)));
        }
        return out;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public <T> Stream<T> accept(TemplateVisitor<T> visitor) {
        if (isStatic()) {
            return Stream.of(visitor.visitStaticTemplate(expectLiteral()));
        }
        if (parts.size() == 1) {
            // must be dynamic because previous branch handled single-element static template
            return Stream.of(visitor.visitSingleDynamicTemplate(((Dynamic) parts.get(0)).expression));
        }
        Stream<T> start = Stream.of(visitor.startMultipartTemplate());
        Stream<T> components = parts.stream().map(part -> part.accept(visitor));
        Stream<T> end = Stream.of(visitor.finishMultipartTemplate());
        return Stream.concat(start, Stream.concat(components, end));
    }

    public List<Part> getParts() {
        return parts;
    }

    public boolean isStatic() {
        for (Part part : parts) {
            if (!(part instanceof Template.Literal)) {
                return false;
            }
        }
        return true;
    }

    public String expectLiteral() {
        if (!isStatic()) {
            throw new RuntimeException("Expected a literal when not all parts are literals.");
        }
        return value;
    }

    public Type typeCheck(Scope<Type> scope) {
        return context(String.format("while typechecking the template `%s`", this), this, () -> {
            for (Part part : parts) {
                context("while checking " + part, () -> part.typeCheck(scope).expectStringType());
            }
            return Type.stringType();
        });
    }

    @Override
    public Node toNode() {
        StringBuilder builder = new StringBuilder();
        for (Part part : parts) {
            if (part instanceof Literal) {
                builder.append(((Literal) part).value);
            } else if (part instanceof Dynamic) {
                builder.append('{').append(((Dynamic) part).raw).append('}');
            }
        }
        return Node.from(builder.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(parts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Template template = (Template) o;
        return parts.equals(template.parts);
    }

    @Override
    public String toString() {
        return String.format("\"%s\"", value);
    }

    public abstract static class Part implements TypeCheck {

        abstract <T> T accept(TemplateVisitor<T> visitor);

        @Override
        public abstract boolean equals(Object obj);
    }

    public static class Literal extends Part {
        private final String value;

        public Literal(String value) {
            if (value.isEmpty()) {
                throw new RuntimeException("value cannot blank");
            }
            this.value = value;
        }

        public static Literal unescape(String value) {
            return new Literal(value.replace("{{", "{").replace("}}", "}"));
        }

        public String getValue() {
            return value;
        }

        @Override
        <T> T accept(TemplateVisitor<T> visitor) {
            return visitor.visitStaticElement(this.value);
        }

        @Override
        public Type typeCheck(Scope<Type> scope) {
            return Type.stringType();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Literal literal = (Literal) o;
            return Objects.equals(value, literal.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public static final class Dynamic extends Part {
        private final String raw;
        private final Expression expression;

        private Dynamic(String raw, Expression expression) {
            this.raw = raw;
            this.expression = expression;
        }

        public static Dynamic parse(String value, FromSourceLocation context) {
            return new Dynamic(value, parseShortform(value, context));
        }

        @Override
        <T> T accept(TemplateVisitor<T> visitor) {
            return visitor.visitDynamicElement(this.expression);
        }

        @Override
        public Type typeCheck(Scope<Type> scope) {
            return expression.typeCheck(scope);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Dynamic dynamic = (Dynamic) o;
            return expression.equals(dynamic.expression);
        }

        public Expression getExpr() {
            return expression;
        }

        @Override
        public int hashCode() {
            return Objects.hash(expression);
        }

        @Override
        public String toString() {
            return String.format("{%s}", raw);
        }
    }
}
