/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.parameters;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.RulesComponentBuilder;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.SyntaxElement;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * A rule-set parameter, representing a value usable in conditions and rules.
 */
@SmithyUnstableApi
public final class Parameter extends SyntaxElement implements ToSmithyBuilder<Parameter>, FromSourceLocation, ToNode {
    public static final String TYPE = "type";
    public static final String DEPRECATED = "deprecated";
    public static final String DOCUMENTATION = "documentation";
    public static final String DEFAULT = "default";
    private static final String BUILT_IN = "builtIn";
    private static final String REQUIRED = "required";
    private static final List<String> PROPERTIES = ListUtils.of(BUILT_IN, REQUIRED, TYPE, DEPRECATED, DOCUMENTATION,
            DEFAULT);

    private final ParameterType type;
    private final Identifier name;
    private final Value value;
    private final String builtIn;
    private final Value defaultValue;
    private final boolean required;
    private final SourceLocation sourceLocation;
    private final Deprecated deprecated;
    private final String documentation;

    private Parameter(Builder builder) {
        if (builder.defaultValue != null && !builder.required) {
            throw new RuntimeException("When a default value is set, the field must also be marked as required");
        }

        this.type = SmithyBuilder.requiredState("type", builder.type);
        this.name = SmithyBuilder.requiredState("name", builder.name);
        this.value = builder.value;
        this.builtIn = builder.builtIn;
        this.defaultValue = builder.defaultValue;
        this.required = builder.required;
        this.sourceLocation = builder.getSourceLocation();
        this.deprecated = builder.deprecated;
        this.documentation = builder.documentation;
    }

    /**
     * Creates a {@link Parameter} instance from the given Node information.
     *
     * @param name the name of the parameter being deserialized.
     * @param objectNode the node to deserialize.
     * @return the created Parameter.
     */
    public static Parameter fromNode(StringNode name, ObjectNode objectNode) throws RuleError {
        return RuleError.context("while parsing the parameter `" + name + "`", objectNode, () -> {
            Builder builder = new Builder(objectNode.getSourceLocation()).name(Identifier.of(name));
            objectNode.expectNoAdditionalProperties(PROPERTIES);

            builder.required(objectNode.getBooleanMemberOrDefault(REQUIRED, false));
            objectNode.getStringMember(BUILT_IN, builder::builtIn);
            objectNode.getStringMember(DOCUMENTATION, builder::documentation);
            objectNode.getObjectMember(DEPRECATED, node -> builder.deprecated(Deprecated.fromNode(node)));

            objectNode.getMember(DEFAULT).map(Value::fromNode).ifPresent(builder::defaultValue);
            builder.type(RuleError.context("while parsing the parameter type", objectNode,
                    () -> ParameterType.fromNode(objectNode.expectStringMember(TYPE))));
            return builder.build();
        });
    }

    /**
     * Builder to create a {@link Parameter} instance.
     *
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    /**
     * Gets the parameter name.
     *
     * @return returns the parameter name as an {@link Identifier}.
     */
    public Identifier getName() {
        return name;
    }

    /**
     * Gets the parameter in template form.
     *
     * @return returns the template form of the parameter.
     */
    public String getTemplate() {
        return "{" + name + "}";
    }

    /**
     * Gets if the parameter is required or not.
     *
     * @return true if the parameter is required, false otherwise.
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Gets the documentation value.
     *
     * @return returns the optional documentation value.
     */
    public ParameterType getType() {
        return type;
    }

    /**
     * Gets a {@link Type} for the parameter's type.
     *
     * @return a Type for the parameter.
     */
    public Type toType() {
        Type out = Type.fromParameterType(type);

        if (defaultValue != null && !defaultValue.getType().equals(out)) {
            throw new RuntimeException(String.format("Invalid type for field \"default\": Type must match "
                    + "parameter type. Expected `%s`, found `%s`.", out, defaultValue.getType()));
        }

        if (!required) {
            out = Type.optionalType(out);
        }
        return out;
    }

    /**
     * Gets the built-in parameter name.
     *
     * @return returns the optional built-in parameter name.
     */
    public Optional<String> getBuiltIn() {
        return Optional.ofNullable(builtIn);
    }

    /**
     * Gets if the parameter uses a built-in parameter.
     *
     * @return returns true if the parameter uses a built-in, false otherwise.
     */
    public boolean isBuiltIn() {
        return builtIn != null;
    }

    /**
     * Gets the deprecated status.
     *
     * @return returns the optional deprecated state.
     */
    public Optional<Deprecated> getDeprecated() {
        return Optional.ofNullable(deprecated);
    }

    /**
     * Gets the parameter's value.
     *
     * @return returns the optional value.
     */
    public Optional<Value> getValue() {
        return Optional.ofNullable(value);
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
     * Gets the parameter's default value.
     *
     * @return returns the optional default value.
     */
    public Optional<Value> getDefault() {
        return Optional.ofNullable(defaultValue);
    }

    @Override
    public Condition.Builder toConditionBuilder() {
        return Condition.builder().fn(toExpression());
    }

    @Override
    public Expression toExpression() {
        return Expression.getReference(name, SourceLocation.none());
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .type(type)
                .name(name)
                .builtIn(builtIn)
                .value(value)
                .required(required)
                .sourceLocation(sourceLocation)
                .deprecated(deprecated)
                .documentation(documentation)
                .defaultValue(defaultValue);
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder node = ObjectNode.builder();
        if (builtIn != null) {
            node.withMember(BUILT_IN, builtIn);
        }
        node.withMember(REQUIRED, required);
        if (defaultValue != null) {
            node.withMember(DEFAULT, defaultValue);
        }
        if (deprecated != null) {
            node.withMember(DEPRECATED, deprecated);
        }
        if (documentation != null) {
            node.withMember(DOCUMENTATION, documentation);
        }
        node.withMember(TYPE, type.toString());
        return node.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Parameter parameter = (Parameter) o;
        return required == parameter.required
                && Objects.equals(type, parameter.type)
                && name.equals(parameter.name)
                && Objects.equals(value, parameter.value)
                && Objects.equals(builtIn, parameter.builtIn)
                && Objects.equals(defaultValue, parameter.defaultValue)
                && Objects.equals(deprecated, parameter.deprecated)
                && Objects.equals(documentation, parameter.documentation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, value, builtIn, required);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": ").append(type);
        if (builtIn != null) {
            sb.append("; builtIn(").append(builtIn).append(")");
        }
        if (required) {
            sb.append("; required");
        }
        getDeprecated().ifPresent(dep -> sb.append("; ").append(deprecated).append("!"));
        return sb.toString();
    }

    /**
     * A builder used to create a {@link Parameter} class.
     */
    public static final class Builder extends RulesComponentBuilder<Builder, Parameter> {
        private ParameterType type;
        private Identifier name;
        private String builtIn;
        private Deprecated deprecated;
        private Value value;
        private boolean required;
        private String documentation;
        private Value defaultValue;

        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
        }

        public Builder type(ParameterType type) {
            this.type = type;
            return this;
        }

        public Builder deprecated(Deprecated deprecated) {
            this.deprecated = deprecated;
            return this;
        }

        public Builder name(String name) {
            this.name = Identifier.of(name);
            return this;
        }

        public Builder name(Identifier name) {
            this.name = name;
            return this;
        }

        public Builder builtIn(String builtIn) {
            this.builtIn = builtIn;
            return this;
        }

        public Builder value(Node value) {
            this.value = Value.fromNode(value);
            return this;
        }

        public Builder value(Value value) {
            this.value = value;
            return this;
        }

        public Builder defaultValue(Value defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder documentation(String s) {
            this.documentation = s;
            return this;
        }

        @Override
        public Parameter build() {
            return new Parameter(this);
        }
    }
}
