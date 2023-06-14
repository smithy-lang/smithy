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
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

@SmithyUnstableApi
public final class Parameter implements ToSmithyBuilder<Parameter>, FromSourceLocation, ToNode {
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
        if (builder.defaultValue != null && builder.builtIn == null) {
            throw new RuntimeException("Cannot set a default value for non-builtin parameters");
        }
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

    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public Identifier getName() {
        return name;
    }

    public String getTemplate() {
        return "{" + name + "}";
    }

    public boolean isRequired() {
        return required;
    }

    public ParameterType getType() {
        return type;
    }

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

    public Optional<String> getBuiltIn() {
        return Optional.ofNullable(builtIn);
    }

    public boolean isBuiltIn() {
        return builtIn != null;
    }

    public Optional<Value> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    public Optional<Deprecated> getDeprecated() {
        return Optional.ofNullable(deprecated);
    }

    public Optional<Value> getValue() {
        return Optional.ofNullable(value);
    }

    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    /**
     * The default value for this Parameter.
     *
     * @return The value. This value must match the type of this parameter.
     */
    public Optional<Value> getDefault() {
        return Optional.ofNullable(defaultValue);
    }

    /**
     * Provides a reference to this parameter as an expression.
     *
     * @return the reference to the parameter.
     */
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
