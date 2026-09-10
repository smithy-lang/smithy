/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language;

import static software.amazon.smithy.rulesengine.language.error.RuleError.context;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.TypeCheck;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionNode;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.rulesengine.validators.AuthSchemeValidator;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyInternalApi;
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

    // Default endpoint components discovered from this class's own classloader, used when no
    // EndpointComponentFactory is supplied. A caller assembling a model with an explicit closure
    // classloader threads its own factory through fromNode instead (see
    // EndpointRuleSetTrait#getComponentFactory), so extensions living only in that closure (e.g.
    // smithy-aws-endpoints, which registers aws.partition) are found. Lazily initialized via the
    // holder idiom so ServiceLoader runs at most once for the default.
    private static final class DefaultComponentFactoryHolder {
        private static final EndpointComponentFactory INSTANCE =
                EndpointComponentFactory.createServiceFactory(EndpointRuleSet.class.getClassLoader());
    }

    private static EndpointComponentFactory defaultComponentFactory() {
        return DefaultComponentFactoryHolder.INSTANCE;
    }

    /**
     * Gets the shared {@link EndpointComponentFactory} discovered from this class's own classloader.
     *
     * <p>Reused for any rule-set that does not need a distinct classloader (no explicit loader, or a
     * loader equal to this class's own), so those cases avoid re-running service discovery.
     *
     * @return the shared default component factory.
     */
    @SmithyInternalApi
    public static EndpointComponentFactory getDefaultComponentFactory() {
        return defaultComponentFactory();
    }

    private final Parameters parameters;
    private final List<Rule> rules;
    private final SourceLocation sourceLocation;
    private final String version;
    private final RulesVersion rulesVersion;
    private final EndpointComponentFactory componentFactory;

    private EndpointRuleSet(Builder builder) {
        super();
        parameters = SmithyBuilder.requiredState(PARAMETERS, builder.parameters);
        rules = builder.rules.copy();
        sourceLocation = SmithyBuilder.requiredState("source", builder.getSourceLocation());
        version = SmithyBuilder.requiredState(VERSION, builder.version);
        rulesVersion = RulesVersion.of(version);
        componentFactory = builder.componentFactory != null
                ? builder.componentFactory
                : defaultComponentFactory();
    }

    /**
     * Gets the {@link EndpointComponentFactory} used to resolve this rule-set's functions and
     * built-ins.
     *
     * <p>Captured from the classloader-aware {@link #fromNode(Node, EndpointComponentFactory)} (the
     * classloader used to assemble the model) and carried across {@link #toBuilder()}, so transforms
     * that round-trip a rule-set through the node form re-resolve functions against the same
     * classloader. Defaults to the shared factory discovered from this class's own loader.
     *
     * @return the component factory for this rule-set.
     */
    @SmithyInternalApi
    public EndpointComponentFactory getComponentFactory() {
        return componentFactory;
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
        return fromNode(node, defaultComponentFactory());
    }

    /**
     * Creates an {@link EndpointRuleSet} from the given Node, discovering endpoint functions and
     * built-ins using the given {@code classLoader}.
     *
     * <p>Convenience overload that builds an {@link EndpointComponentFactory} from
     * {@code classLoader} (or this class's own loader when null). Prefer
     * {@link #fromNode(Node, EndpointComponentFactory)} when a factory is already available so it
     * can be reused across the deserialization and validation of the same rule-set.
     *
     * @param node the node to deserialize.
     * @param classLoader the classloader used to discover endpoint extensions, or null.
     * @return the created EndpointRuleSet.
     */
    public static EndpointRuleSet fromNode(Node node, ClassLoader classLoader) throws RuleError {
        return fromNode(node,
                classLoader != null
                        ? EndpointComponentFactory.createServiceFactory(classLoader)
                        : defaultComponentFactory());
    }

    /**
     * Creates an {@link EndpointRuleSet} from the given Node, resolving endpoint functions and
     * built-ins using the given {@link EndpointComponentFactory}.
     *
     * <p>The factory wraps the classloader used to discover {@link EndpointRuleSetExtension}s and
     * caches the discovered components, so a caller can build it once (typically from a
     * dependency-closure classloader) and reuse it. When {@code factory} is {@code null}, the
     * default factory discovered from this class's own loader is used.
     *
     * @param node the node to deserialize.
     * @param factory the factory used to resolve endpoint components, or null.
     * @return the created EndpointRuleSet.
     */
    @SmithyInternalApi
    public static EndpointRuleSet fromNode(Node node, EndpointComponentFactory factory) throws RuleError {
        EndpointComponentFactory resolved = factory != null ? factory : defaultComponentFactory();
        return RuleError.context("when parsing endpoint ruleset", () -> {
            ObjectNode objectNode = node.expectObjectNode("The root of a ruleset must be an object");

            EndpointRuleSet.Builder builder = new Builder(node);
            builder.componentFactory(resolved);
            builder.parameters(Parameters.fromNode(objectNode.expectObjectMember(PARAMETERS)));
            objectNode.expectStringMember(VERSION, builder::version);

            for (Node element : objectNode.expectArrayMember(RULES).getElements()) {
                builder.addRule(context("while parsing rule", element, () -> Rule.fromNode(element, resolved)));
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

    /**
     * Get the parsed rules engine version.
     *
     * @return parsed version.
     */
    public RulesVersion getRulesVersion() {
        return rulesVersion;
    }

    public Type typeCheck() {
        return typeCheck(new Scope<>());
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
        return new Builder(this);
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

    /**
     * Returns true if a built-in of the provided name has been registered.
     *
     * @param name the name of the built-in to check for.
     * @return true if the built-in is present, false otherwise.
     */
    @SmithyInternalApi
    public static boolean hasBuiltIn(String name) {
        return defaultComponentFactory().hasBuiltIn(name);
    }

    /**
     * Gets the built-in names as a joined string.
     *
     * @return a string of the built-in names.
     */
    @SmithyInternalApi
    public static String getKeyString() {
        return defaultComponentFactory().getKeyString();
    }

    /**
     * Creates a {@link LibraryFunction} factory function using the loaded function definitions.
     *
     * @return the created factory.
     */
    @SmithyInternalApi
    public static Function<FunctionNode, Optional<LibraryFunction>> createFunctionFactory() {
        return defaultComponentFactory().createFunctionFactory();
    }

    /**
     * Gets loaded authentication scheme validators.
     *
     * @return a list of {@link AuthSchemeValidator}s.
     */
    @SmithyInternalApi
    public static List<AuthSchemeValidator> getAuthSchemeValidators() {
        return defaultComponentFactory().getAuthSchemeValidators();
    }

    /**
     * A builder used to create a {@link EndpointRuleSet} class.
     */
    public static class Builder extends RulesComponentBuilder<Builder, EndpointRuleSet> {
        private final BuilderRef<List<Rule>> rules = BuilderRef.forList();
        private Parameters parameters;
        // Default the version to the latest.
        private String version = LATEST_VERSION;
        private EndpointComponentFactory componentFactory;

        /**
         * Construct a builder from a {@link SourceLocation}.
         *
         * @param sourceLocation The source location
         */
        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
        }

        private Builder(EndpointRuleSet ruleSet) {
            super(ruleSet.sourceLocation);
            this.parameters = ruleSet.parameters;
            this.version = ruleSet.version;
            this.rules.setBorrowed(ruleSet.rules);
            this.componentFactory = ruleSet.componentFactory;
        }

        /**
         * Sets the {@link EndpointComponentFactory} used to resolve functions and built-ins when a
         * rule-set built from this builder is later round-tripped through the node form. May be
         * null, in which case the shared default factory is used.
         *
         * @param componentFactory the factory, or null.
         * @return the {@link Builder}
         */
        @SmithyInternalApi
        public Builder componentFactory(EndpointComponentFactory componentFactory) {
            this.componentFactory = componentFactory;
            return this;
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
            ruleSet.typeCheck();
            return ruleSet;
        }
    }

    /**
     * {@link EndpointRuleSet} visitor that collects a map of JSON pointer paths to {@link Endpoint}s.
     */
    @SmithyInternalApi
    public static final class EndpointPathCollector {
        private static final String ENDPOINT = "endpoint";
        private static final String TYPE = "type";
        private final Map<String, Endpoint> visitedEndpoints = new HashMap();
        private final ObjectNode endpointRuleSet;
        private final EndpointComponentFactory componentFactory;

        private EndpointPathCollector(EndpointRuleSetTrait endpointRuleSetTrait) {
            this.endpointRuleSet = endpointRuleSetTrait.getRuleSet().expectObjectNode();
            this.componentFactory = endpointRuleSetTrait.getComponentFactory();
        }

        /**
         * Creates a collector from an {@link EndpointRuleSetTrait}.
         *
         * @param endpointRuleSetTrait EndpointRuleSet trait instance.
         */
        @SmithyInternalApi
        public static EndpointPathCollector from(EndpointRuleSetTrait endpointRuleSetTrait) {
            return new EndpointPathCollector(endpointRuleSetTrait);
        }

        /**
         * Collects the mapped JSON pointer paths to {@link Endpoint}s.
         *
         * @return a map of JSON pointer paths to {@link Endpoint}s.
         */
        @SmithyInternalApi
        public Map<String, Endpoint> collect() {
            arrayNode(endpointRuleSet.expectArrayMember(RULES), "/" + RULES);
            return visitedEndpoints;
        }

        private void objectNode(ObjectNode node, String parentPath) {
            boolean isEndpointRuleObject = node
                    .getMember(TYPE)
                    .map(n -> n.asStringNode()
                            .map(s -> s.getValue().equals(ENDPOINT))
                            .orElse(false))
                    .orElse(false);
            if (isEndpointRuleObject) {
                Endpoint endpoint = Endpoint.fromNode(node.expectMember(ENDPOINT), componentFactory);
                visitedEndpoints.put(parentPath + "/" + ENDPOINT, endpoint);
                return;
            }
            for (Entry<StringNode, Node> member : node.getMembers().entrySet()) {
                String key = member.getKey().getValue();
                Node value = member.getValue();
                switch (value.getType()) {
                    case OBJECT: {
                        objectNode(value.expectObjectNode(), parentPath + "/" + key);
                        break;
                    }
                    case ARRAY: {
                        arrayNode(value.expectArrayNode(), parentPath + "/" + key);
                        break;
                    }
                    default:
                        break;
                }
            }
        }

        private void arrayNode(ArrayNode node, String parentPath) {
            List<Node> elements = node.getElements();
            for (int i = 0; i < elements.size(); i++) {
                Node element = elements.get(i);
                switch (element.getType()) {
                    case OBJECT: {
                        objectNode(element.expectObjectNode(), parentPath + "/" + i);
                        break;
                    }
                    case ARRAY: {
                        arrayNode(element.expectArrayNode(), parentPath + "/" + i);
                        break;
                    }
                    default:
                        break;
                }
            }
        }
    }
}
