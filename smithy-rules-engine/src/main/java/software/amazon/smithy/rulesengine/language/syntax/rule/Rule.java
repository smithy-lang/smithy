/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.rule;

import static software.amazon.smithy.rulesengine.language.RulesComponentBuilder.javaLocation;
import static software.amazon.smithy.rulesengine.language.error.RuleError.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.TypeCheck;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.ToCondition;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * The core functionality of a rule-set rule.
 */
@SmithyUnstableApi
public abstract class Rule implements TypeCheck, ToNode, FromSourceLocation {
    public static final String DOCUMENTATION = "documentation";
    public static final String ENDPOINT = "endpoint";
    public static final String ERROR = "error";
    public static final String TREE = "tree";
    public static final String RULES = "rules";
    public static final String TYPE = "type";
    private static final String CONDITIONS = "conditions";

    private final List<Condition> conditions = new ArrayList<>();
    private final String documentation;
    private final SourceLocation sourceLocation;

    protected Rule(Builder builder) {
        for (ToCondition condition : builder.conditions) {
            conditions.add(condition.toCondition());
        }
        documentation = builder.documentation;
        sourceLocation = builder.sourceLocation;
    }

    /**
     * Builder to create a {@link Rule} instance.
     *
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    /**
     * Builder to create a {@link Rule} instance.
     *
     * @param sourceLocation the source of the rule.
     * @return returns a new Builder.
     */
    public static Builder builder(FromSourceLocation sourceLocation) {
        return new Builder(sourceLocation);
    }

    /**
     * Creates a {@link Rule} instance from the given Node information.
     *
     * @param node the node to deserialize.
     * @return the created Rule.
     */
    public static Rule fromNode(Node node) {
        ObjectNode objectNode = node.expectObjectNode();

        Builder builder = new Builder(node);
        objectNode.getStringMember(DOCUMENTATION, builder::description);
        builder.conditions(objectNode.expectArrayMember(CONDITIONS).getElementsAs(Condition::fromNode));

        String type = objectNode.expectStringMember(TYPE).getValue();
        switch (type) {
            case ENDPOINT:
                return builder.endpoint(Endpoint.fromNode(objectNode.expectMember(ENDPOINT)));
            case ERROR:
                return builder.error(objectNode.expectMember(ERROR));
            case TREE:
                return builder.treeRule(objectNode.expectArrayMember(RULES).getElementsAs(Rule::fromNode));
            default:
                throw new IllegalStateException("Unexpected rule type: " + type);
        }
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    /**
     * Gets the conditions required to satisfy this rule.
     *
     * @return the list of conditions.
     */
    public List<Condition> getConditions() {
        return conditions;
    }

    /**
     * Gets the documentation value.
     *
     * @return returns the optional documentation value.
     */
    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    /**
     * Invoke the {@link RuleValueVisitor} functions for this Rule.
     *
     * @param visitor the visitor to be invoked.
     * @param <T>     the visitor return type.
     * @return the return value of the visitor.
     */
    public abstract <T> T accept(RuleValueVisitor<T> visitor);

    protected abstract Type typecheckValue(Scope<Type> scope);

    abstract void withValueNode(ObjectNode.Builder builder);

    @Override
    public Type typeCheck(Scope<Type> scope) {
        return scope.inScope(() -> { // Ensure that we don't leak scope.
            for (Condition condition : conditions) {
                condition.typeCheck(scope);
            }
            String docs = documentation == null ? "" : String.format(" `%s`", documentation);
            return context(String.format("while typechecking%s", docs), this, () -> typecheckValue(scope));
        });
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = ObjectNode.builder();

        ArrayNode.Builder conditionsBuilder = ArrayNode.builder();
        for (Condition condition : conditions) {
            conditionsBuilder.withValue(condition.toNode());
        }
        builder.withMember(CONDITIONS, conditionsBuilder.build());

        if (documentation != null) {
            builder.withMember(DOCUMENTATION, documentation);
        }

        withValueNode(builder);
        return builder.build();
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
    public int hashCode() {
        return Objects.hash(conditions, documentation);
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
                sb.append(StringUtils.indent(condition.toString(), 2));
            }
            sb.append("then:\n");
        }
        return sb.toString();
    }

    /**
     * A builder used to create a {@link Rule} class.
     */
    public static final class Builder {
        private final List<ToCondition> conditions = new ArrayList<>();
        private final SourceLocation sourceLocation;
        private Function<Rule, Rule> onBuild = Function.identity();
        private String documentation;

        private Builder(FromSourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation.getSourceLocation();
        }

        public Builder conditions(ToCondition... conditions) {
            for (ToCondition condition : conditions) {
                condition(condition.toCondition());
            }
            return this;
        }

        public Builder conditions(List<ToCondition> conditions) {
            this.conditions.addAll(conditions);
            return this;
        }

        public Builder condition(ToCondition condition) {
            this.conditions.add(condition);
            return this;
        }

        public Rule endpoint(Endpoint endpoint) {
            return this.onBuild.apply(new EndpointRule(this, endpoint));
        }

        public Rule error(Node error) {
            return this.onBuild.apply(new ErrorRule(this, Expression.fromNode(error)));
        }

        public Rule error(String error) {
            return this.onBuild.apply(new ErrorRule(this, Literal.of(error)));
        }

        public Rule treeRule(Rule... rules) {
            return this.treeRule(Arrays.asList(rules));
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
         * If `condition` IS met, return an error. Otherwise, proceed with the rules generated by
         * the returned builder.
         * <p>
         * This method returns a new builder that must be used!
         *
         * @param condition a coercible {@link Condition}
         * @param error     an error description if the condition is not matched
         * @return new builder to attach subsequent rules to
         */
        public Builder errorOrElse(String error, ToCondition... condition) {
            Builder next = new Builder(javaLocation());
            next.onBuild = (Rule rule) -> this.treeRule(Rule.builder().conditions(condition).error(error), rule);
            return next;

        }

        /**
         * If `condition` IS NOT met, return an error. Otherwise, proceed with the rules generated by
         * the returned builder.
         * <p>
         * This method returns a new builder that must be used!
         *
         * @param condition a coercible {@link Condition}
         * @param error     an error description if the condition is not matched
         * @return new builder to attach subsequent rules to
         */
        public Builder validateOrElse(String error, ToCondition... condition) {
            Builder next = new Builder(javaLocation());
            next.onBuild = (Rule rule) -> this.treeRule(
                    Rule.builder().conditions(condition).treeRule(rule),
                    Rule.builder().error(error));
            return next;
        }
    }
}
