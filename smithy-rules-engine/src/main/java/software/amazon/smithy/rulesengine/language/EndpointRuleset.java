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

package software.amazon.smithy.rulesengine.language;

import static software.amazon.smithy.rulesengine.language.error.RuleError.ctx;
import static software.amazon.smithy.rulesengine.language.util.StringUtils.indent;

import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Typecheck;
import software.amazon.smithy.rulesengine.language.lang.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.lang.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.lang.rule.Rule;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A set of EndpointRules. Endpoint Rules describe the endpoint resolution behavior for a service.
 */
@SmithyUnstableApi
public final class EndpointRuleset implements FromSourceLocation, Typecheck, ToNode {
    private static final String LATEST_VERSION = "1.3";
    private static final String VERSION = "version";
    private static final String PARAMETERS = "parameters";
    private static final String RULES = "rules";

    private final List<Rule> rules;
    private final Parameters parameters;
    private final String version;

    private final SourceLocation sourceLocation;

    private EndpointRuleset(Builder builder) {
        rules = builder.rules.copy();
        parameters = SmithyBuilder.requiredState("parameters", builder.parameters);
        version = SmithyBuilder.requiredState("version", builder.version);
        sourceLocation = builder.getSourceLocation();
    }

    public static EndpointRuleset fromNode(Node node) throws RuleError {
        return ctx("when parsing endpoint ruleset", () -> EndpointRuleset.newFromNode(node));
    }

    private static EndpointRuleset newFromNode(Node node) throws RuleError {
        ObjectNode on = node.expectObjectNode("The root of a ruleset must be an object");
        EndpointRuleset.Builder builder = new Builder(node);
        Parameters parameters = Parameters.fromNode(on.expectObjectMember(PARAMETERS));
        StringNode version = on.expectStringMember(VERSION);

        on.expectArrayMember(RULES)
                .getElements().forEach(n -> {
                    builder.addRule(ctx("while parsing rule", n, () -> EndpointRule.fromNode(n)));
                });
        return builder.version(version.getValue()).parameters(parameters).build();
    }

    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    public Parameters getParameters() {
        return parameters;
    }

    public List<Rule> getRules() {
        return rules;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Type typecheck(Scope<Type> scope) {
        return scope.inScope(() -> {
            parameters.writeToScope(scope);
            for (Rule rule : rules) {
                rule.typecheck(scope);
            }
            return Type.endpoint();
        });
    }

    public void typecheck() {
        typecheck(new Scope<>());
    }

    @Override
    public Node toNode() {
        return ObjectNode.builder()
                .withMember(VERSION, version)
                .withMember(PARAMETERS, parameters)
                .withMember(RULES, rulesNode())
                .build();
    }

    private Node rulesNode() {
        ArrayNode.Builder node = ArrayNode.builder();
        rules.forEach(node::withValue);
        return node.build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(rules, parameters, version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EndpointRuleset that = (EndpointRuleset) o;
        return rules.equals(that.rules) && parameters.equals(that.parameters) && version.equals(that.version);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("version: %s%n", version));
        builder.append("params: \n").append(indent(parameters.toString(), 2));
        builder.append("rules: \n");
        rules.forEach(rule -> builder.append(indent(rule.toString(), 2)));
        return builder.toString();
    }

    public static class Builder extends SourceAwareBuilder<Builder, EndpointRuleset> {
        private final BuilderRef<List<Rule>> rules = BuilderRef.forList();
        private Parameters parameters;
        // default the version to the latest.
        private String version = LATEST_VERSION;

        /**
         * Construct a builder from a {@link SourceLocation}.
         *
         * @param sourceLocation The source location
         */
        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
        }

        /**
         * Sets the version for the {@link EndpointRuleset}.
         * If not set, the version will default to the latest version.
         *
         * @param version The version to set
         * @return the {@link Builder}
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Adds a rule to this ruleset. The rule be evaluated if all previous rules do not match.
         *
         * @param rule The {@link Rule} to add
         * @return the {@link Builder}
         */
        public Builder addRule(Rule rule) {
            this.rules.get().add(rule);
            return this;
        }

        /**
         * Set the parameters for this {@link EndpointRuleset}.
         *
         * @param parameters {@link Parameters} to set
         * @return the {@link Builder}
         */
        public Builder parameters(Parameters parameters) {
            this.parameters = parameters;
            return this;
        }

        @Override
        public EndpointRuleset build() {
            return new EndpointRuleset(this);
        }
    }
}
