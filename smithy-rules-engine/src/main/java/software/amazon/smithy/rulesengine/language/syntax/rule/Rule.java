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

package software.amazon.smithy.rulesengine.language.syntax.rule;

import static software.amazon.smithy.rulesengine.language.error.RuleError.ctx;
import static software.amazon.smithy.rulesengine.language.util.StringUtils.indent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.Into;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Typecheck;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expr;
import software.amazon.smithy.rulesengine.language.syntax.expr.Literal;
import software.amazon.smithy.rulesengine.language.util.SourceLocationHelpers;
import software.amazon.smithy.rulesengine.language.visit.RuleValueVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public abstract class Rule implements Typecheck, ToNode, FromSourceLocation {
    public static final String DOCUMENTATION = "documentation";
    public static final String ENDPOINT = "endpoint";
    public static final String ERROR = "error";
    public static final String TREE = "tree";
    public static final String RULES = "rules";
    public static final String TYPE = "type";
    private static final String CONDITIONS = "conditions";
    private final SourceLocation sourceLocation;
    private final List<Condition> conditions;
    private final String documentation;

    protected Rule(Builder builder) {
        this.conditions = builder.conditions;
        this.documentation = builder.documentation;
        this.sourceLocation = builder.sourceLocation;
    }

    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    public static Builder builder(FromSourceLocation sourceLocation) {
        return new Builder(sourceLocation);
    }

    public static Rule fromNode(Node node) {
        ObjectNode on = node.expectObjectNode();

        Builder builder = new Builder(node);

        ArrayNode conditionsNode = on.expectArrayMember(CONDITIONS);

        List<Node> fnNodes = new ArrayList<>(conditionsNode.getElements());
        List<Condition> conditions = fnNodes.stream().map(Condition::fromNode).collect(Collectors.toList());
        builder.conditions(conditions);
        Optional<String> description = on.getStringMember(DOCUMENTATION).map(StringNode::getValue);
        description.ifPresent(builder::description);

        String type = on.expectStringMember(TYPE).getValue();
        switch (type) {
            case ENDPOINT:
                return builder.endpoint(Endpoint.fromNode(on.expectMember(ENDPOINT)));
            case ERROR:
                return builder.error(on.expectMember(ERROR));
            case TREE:
                return builder.treeRule(on.expectArrayMember(RULES)
                        .getElements()
                        .stream()
                        .map(Rule::fromNode)
                        .collect(Collectors.toList()));
            default:
                throw new IllegalStateException("Unexpected rule type: " + type);
        }
    }

    public abstract <T> T accept(RuleValueVisitor<T> visitor);

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    @Override
    public Type typecheck(Scope<Type> scope) {
        // ensure that we don't leak scope
        return scope.inScope(() -> {
            for (Condition condition : this.conditions) {
                ctx(String.format("while typechecking %s", condition.getFn()), condition,
                        () -> condition.typecheck(scope));
            }
            return ctx(String.format("while typechecking%s", this
                            .getDocumentation()
                            .map(doc -> String.format(" `%s`", doc))
                            .orElse("")),
                    this, () -> typecheckValue(scope));
        });
    }

    protected abstract Type typecheckValue(Scope<Type> scope);

    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder ruleNode = ObjectNode.builder();
        ArrayNode conditionsNode = conditions.stream().map(ToNode::toNode).collect(ArrayNode.collect());
        ruleNode.withMember(CONDITIONS, conditionsNode);
        if (documentation != null) {
            ruleNode.withMember(DOCUMENTATION, documentation);
        }
        withValueNode(ruleNode);
        return ruleNode.build();
    }

    abstract void withValueNode(ObjectNode.Builder builder);

    @Override
    public int hashCode() {
        return Objects.hash(conditions, documentation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Rule rule = (Rule) o;
        return conditions.equals(rule.conditions) && Objects.equals(documentation, rule.documentation);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (documentation != null) {
            sb.append("# ").append(documentation).append("\n");
        }
        if (conditions.isEmpty()) {
            sb.append("always:\n");
        } else {
            sb.append("when:\n");
            for (Condition condition : conditions) {
                sb.append(indent(condition.toString(), 2));
            }
            sb.append("then:\n");
        }
        return sb.toString();
    }

    public static final class Builder {

        private final List<Condition> conditions = new ArrayList<>();
        private final SourceLocation sourceLocation;
        private Function<Rule, Rule> onBuild = Function.identity();
        private String documentation;

        private Builder(FromSourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation.getSourceLocation();
        }

        @SafeVarargs
        public final Builder conditions(Into<Condition>... conditions) {
            this.conditions.addAll(Arrays.stream(conditions).map(Into::into).collect(Collectors.toList()));
            return this;
        }

        public Builder conditions(List<Condition> conditions) {
            this.conditions.addAll(conditions);
            return this;
        }

        public Builder condition(Into<Condition> condition) {
            this.conditions.add(condition.into());
            return this;
        }

        public Rule endpoint(Endpoint endpoint) {
            return this.onBuild.apply(new EndpointRule(this, endpoint));
        }

        public Rule error(Node error) {
            return this.onBuild.apply(new ErrorRule(this, Expr.fromNode(error)));
        }

        public Rule error(String error) {
            return this.onBuild.apply(new ErrorRule(this, Literal.of(error)));
        }

        public Rule treeRule(Rule... rules) {
            return this.treeRule(Arrays.stream(rules).collect(Collectors.toList()));
        }

        @SafeVarargs
        public final Rule treeRule(List<Rule>... rules) {
            List<Rule> out = new ArrayList<>();
            for (List<Rule> ruleList : rules) {
                out.addAll(ruleList);
            }
            return this.onBuild.apply(new TreeRule(this, out));
        }

        public Builder description(String description) {
            this.documentation = description;
            return this;
        }

        /**
         * If `condition` IS met, return an error. Otherwise, proceed with the rules generated by the returned builder
         */
        @SafeVarargs
        public final Builder errorOrElse(String error, Into<Condition>... condition) {
            Builder next = new Builder(SourceLocationHelpers.javaLocation());
            next.onBuild = (Rule r) -> this.treeRule(
                    Rule.builder().conditions(condition).error(error),
                    r
            );
            return next;

        }

        /**
         * If `condition` is not met, return an error. Otherwise, proceed with the rules generated by
         * the returned builder.
         * <p>
         * This method returns a new builder that must be used!
         *
         * @param condition a coercible {@link Condition}
         * @param error     an error description if the condition is not matched
         * @return a new builder to attach subsequent rules to
         */
        public Builder validateOrElse(Into<Condition> condition, String error) {
            Builder next = new Builder(SourceLocationHelpers.javaLocation());
            next.onBuild = (Rule r) -> this.treeRule(
                    Rule.builder().conditions(condition).treeRule(r),
                    Rule.builder().error(error)
            );
            return next;
        }


        /**
         * If `condition` is not met, return an error. Otherwise, proceed with the rules generated by
         * the returned builder.
         * <p>
         * This method returns a new builder that must be used!
         *
         * @param condition a coercible {@link Condition}
         * @param error     an error description if the condition is not matched
         * @return new builder to attach subsequent rules to
         */
        public Builder validateOrElse(String error, Into<Condition>... condition) {
            Builder next = new Builder(SourceLocationHelpers.javaLocation());
            next.onBuild = (Rule r) -> this.treeRule(
                    Rule.builder().conditions(condition).treeRule(r),
                    Rule.builder().error(error)
            );
            return next;
        }
    }
}
