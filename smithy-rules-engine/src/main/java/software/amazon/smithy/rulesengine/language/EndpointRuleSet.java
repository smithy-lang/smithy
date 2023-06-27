/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language;

import static software.amazon.smithy.rulesengine.language.error.RuleError.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.TypeCheck;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionNode;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.validators.AuthSchemeValidator;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * A set of EndpointRules. EndpointType Rules describe the endpoint resolution behavior for a service.
 */
@SmithyUnstableApi
public final class EndpointRuleSet implements FromSourceLocation, ToNode, ToSmithyBuilder<EndpointRuleSet>, TypeCheck {
    private static final String LATEST_VERSION = "1.3";
    private static final String VERSION = "version";
    private static final String PARAMETERS = "parameters";
    private static final String RULES = "rules";

    private static boolean loaded = false;
    private static final Map<String, Parameter> BUILT_INS = new HashMap<>();
    private static final Map<String, FunctionDefinition> FUNCTIONS = new HashMap<>();
    private static final List<AuthSchemeValidator> AUTH_SCHEME_VALIDATORS = new ArrayList<>();

    private final Parameters parameters;
    private final List<Rule> rules;
    private final SourceLocation sourceLocation;
    private final String version;

    private EndpointRuleSet(Builder builder) {
        super();
        parameters = SmithyBuilder.requiredState("parameters", builder.parameters);
        rules = builder.rules.copy();
        sourceLocation = SmithyBuilder.requiredState("source", builder.getSourceLocation());
        version = SmithyBuilder.requiredState("version", builder.version);
    }

    /**
     * Builder to create a {@link EndpointRuleSet} instance.
     *
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    /**
     * Creates an {@link EndpointRuleSet} of a specific type from the given Node information.
     *
     * @param node the node to deserialize.
     * @return the created EndpointRuleSet.
     */
    public static EndpointRuleSet fromNode(Node node) throws RuleError {
        loadExtensions();
        return RuleError.context("when parsing endpoint ruleset", () -> {
            ObjectNode objectNode = node.expectObjectNode("The root of a ruleset must be an object");

            EndpointRuleSet.Builder builder = new Builder(node);
            builder.parameters(Parameters.fromNode(objectNode.expectObjectMember(PARAMETERS)));
            objectNode.expectStringMember(VERSION, builder::version);

            for (Node element : objectNode.expectArrayMember(RULES).getElements()) {
                builder.addRule(context("while parsing rule", element, () -> EndpointRule.fromNode(element)));
            }

            return builder.build();
        });
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    /**
     * Gets the {@link Parameters} defined in this rule-set.
     *
     * @return the parameters defined in the rule-set.
     */
    public Parameters getParameters() {
        return parameters;
    }

    /**
     * Gets the list of {@link Rule}s defined in this rule-set.
     *
     * @return the rules defined in this rule-set.
     */
    public List<Rule> getRules() {
        return rules;
    }

    /**
     * Gets the version of this rule-set.
     *
     * @return the rule-set version.
     */
    public String getVersion() {
        return version;
    }

    @Override
    public Type typeCheck(Scope<Type> scope) {
        return scope.inScope(() -> {
            parameters.writeToScope(scope);
            for (Rule rule : rules) {
                rule.typeCheck(scope);
            }
            return Type.endpointType();
        });
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .parameters(parameters)
                .rules(rules)
                .version(version);
    }

    @Override
    public Node toNode() {
        ArrayNode.Builder rulesBuilder = ArrayNode.builder();
        rules.forEach(rulesBuilder::withValue);

        return ObjectNode.builder()
                .withMember(VERSION, version)
                .withMember(PARAMETERS, parameters)
                .withMember(RULES, rulesBuilder.build())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EndpointRuleSet that = (EndpointRuleSet) o;
        return rules.equals(that.rules) && parameters.equals(that.parameters) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rules, parameters, version);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("version: %s%n", version));
        builder.append("params: \n").append(StringUtils.indent(parameters.toString(), 2));
        builder.append("rules: \n");
        rules.forEach(rule -> builder.append(StringUtils.indent(rule.toString(), 2)));
        return builder.toString();
    }

    private static void loadExtensions() {
        if (loaded) {
            return;
        }
        loaded = true;

        for (EndpointRuleSetExtension extension : ServiceLoader.load(EndpointRuleSetExtension.class)) {
            String name;
            for (Parameter builtIn : extension.getBuiltIns()) {
                name = builtIn.getBuiltIn().get();
                if (BUILT_INS.containsKey(name)) {
                    throw new RuntimeException("Attempted to load a duplicate built-in parameter: " + name);
                }
                BUILT_INS.put(name, builtIn);
            }

            for (FunctionDefinition functionDefinition : extension.getLibraryFunctions()) {
                name = functionDefinition.getId();
                if (FUNCTIONS.containsKey(name)) {
                    throw new RuntimeException("Attempted to load a duplicate library function: " + name);
                }
                FUNCTIONS.put(name, functionDefinition);
            }

            AUTH_SCHEME_VALIDATORS.addAll(extension.getAuthSchemeValidators());
        }
    }


    /**
     * Returns true if a built-in of the provided name has been registered.
     *
     * @param name the name of the built-in to check for.
     * @return true if the built-in is present, false otherwise.
     */
    public static boolean hasBuiltIn(String name) {
        return BUILT_INS.containsKey(name);
    }

    /**
     * Gets the built-in names as a joined string.
     *
     * @return a string of the built-in names.
     */
    public static String getKeyString() {
        return String.join(", ", BUILT_INS.keySet());
    }

    /**
     * Creates a {@link LibraryFunction} factory function using the loaded function definitions.
     *
     * @return the created factory.
     */
    public static Function<FunctionNode, Optional<LibraryFunction>> createFunctionFactory() {
        return node -> {
            if (FUNCTIONS.containsKey(node.getName())) {
                return Optional.of(FUNCTIONS.get(node.getName()).createFunction(node));
            }
            return Optional.empty();
        };
    }

    /**
     * Gets loaded authentication scheme validators.
     *
     * @return a list of {@link AuthSchemeValidator}s.
     */
    public static List<AuthSchemeValidator> getAuthSchemeValidators() {
        return AUTH_SCHEME_VALIDATORS;
    }

    /**
     * A builder used to create a {@link EndpointRuleSet} class.
     */
    public static class Builder extends RulesComponentBuilder<Builder, EndpointRuleSet> {
        private final BuilderRef<List<Rule>> rules = BuilderRef.forList();
        private Parameters parameters;
        // Default the version to the latest.
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
         * Sets the version for the {@link EndpointRuleSet}.
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
         * Inserts a rule into the ruleset.
         *
         * @param  index the position to add the rule at.
         * @param rule The {@link Rule} to add
         * @return the {@link Builder}
         */
        public Builder addRule(int index, Rule rule) {
            this.rules.get().add(index, rule);
            return this;
        }

        /**
         * Add rules to this ruleset. The rules be evaluated if all previous rules do not match.
         *
         * @param rules The Collection of {@link Rule} to add
         * @return the {@link Builder}
         */
        public Builder rules(Collection<Rule> rules) {
            this.rules.clear();
            this.rules.get().addAll(rules);
            return this;
        }

        /**
         * Set the parameters for this {@link EndpointRuleSet}.
         *
         * @param parameters {@link Parameters} to set
         * @return the {@link Builder}
         */
        public Builder parameters(Parameters parameters) {
            this.parameters = parameters;
            return this;
        }

        @Override
        public EndpointRuleSet build() {
            EndpointRuleSet ruleSet = new EndpointRuleSet(this);
            ruleSet.typeCheck(new Scope<>());
            return ruleSet;
        }
    }
}
