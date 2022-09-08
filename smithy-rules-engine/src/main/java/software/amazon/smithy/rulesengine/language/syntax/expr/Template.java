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

package software.amazon.smithy.rulesengine.language.syntax.expr;

import static software.amazon.smithy.rulesengine.language.error.RuleError.ctx;
import static software.amazon.smithy.rulesengine.language.syntax.expr.Expr.parseShortform;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Typecheck;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.util.MandatorySourceLocation;
import software.amazon.smithy.rulesengine.language.visit.TemplateVisitor;
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
public final class Template extends MandatorySourceLocation implements ToNode {

    private final List<Part> parts;

    Template(StringNode template) {
        super(template);
        this.parts =
                ctx("when parsing template", template, () -> parseTemplate(template.getValue(), template));
    }

    public static Template fromString(String s) {
        return new Template(StringNode.from(s));
    }

    public <T> Stream<T> accept(TemplateVisitor<T> visitor) {
        if (isStatic()) {
            return Stream.of(visitor.visitStaticTemplate(expectLiteral()));
        }
        if (parts.size() == 1) {
            // must be dynamic because previous branch handled single-element static template
            return Stream.of(visitor.visitSingleDynamicTemplate(((Dynamic) parts.get(0)).expr));
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
        return this.parts.stream().allMatch(it -> it instanceof Template.Literal);
    }

    public String expectLiteral() {
        assert isStatic();
        return this.parts.stream().map(Part::toString).collect(Collectors.joining());
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
        return String.format("\"%s\"", this.parts.stream().map(Part::toString).collect(Collectors.joining()));
    }

    Type typecheck(Scope<Type> scope) {
        return ctx(
                String.format("while typechecking the template `%s`", this),
                this,
                () -> {
                    this.parts.forEach(
                            part -> ctx("while checking " + part, () -> part.typecheck(scope).expectString()));
                    return Type.str();
                });
    }

    @Override
    public Node toNode() {
        StringBuilder sb = new StringBuilder();
        parts.forEach(
                p -> {
                    if (p instanceof Literal) {
                        sb.append(((Literal) p).value);
                    } else if (p instanceof Dynamic) {
                        sb.append('{').append(((Dynamic) p).raw).append('}');
                    }
                });
        return Node.from(sb.toString());
    }

    public Value eval(Scope<Value> scope) {
        return Value.str(parts.stream().map(part -> part.eval(scope)).collect(Collectors.joining()));
    }

    private List<Part> parseTemplate(String template, FromSourceLocation context)
            throws InnerParseError {
        ArrayList<Part> out = new ArrayList<>();
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

    public abstract static class Part implements Typecheck {
        abstract String eval(Scope<Value> scope);

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
        String eval(Scope<Value> scope) {
            return this.value;
        }

        @Override
        <T> T accept(TemplateVisitor<T> visitor) {
            return visitor.visitStaticElement(this.value);
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
            return this.value;
        }

        @Override
        public Type typecheck(Scope<Type> scope) {
            return Type.str();
        }
    }

    public static final class Dynamic extends Part {
        private final String raw;
        private final Expr expr;

        private Dynamic(String raw, Expr expr) {
            this.raw = raw;
            this.expr = expr;
        }

        public static Dynamic parse(String value, FromSourceLocation context) {
            return new Dynamic(value, parseShortform(value, context));
        }

        @Override
        String eval(Scope<Value> scope) {
            return ctx("while evaluating " + this, () -> expr.eval(scope).expectString());
        }

        @Override
        <T> T accept(TemplateVisitor<T> visitor) {
            return visitor.visitDynamicElement(this.expr);
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
            return expr.equals(dynamic.expr);
        }

        public Expr getExpr() {
            return expr;
        }

        @Override
        public int hashCode() {
            return Objects.hash(expr);
        }

        @Override
        public String toString() {
            return String.format("{%s}", this.raw);
        }

        @Override
        public Type typecheck(Scope<Type> scope) {
            return expr.typecheck(scope);
        }
    }
}
