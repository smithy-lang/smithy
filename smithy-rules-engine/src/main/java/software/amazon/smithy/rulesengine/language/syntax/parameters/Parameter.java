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

package software.amazon.smithy.rulesengine.language.syntax.parameters;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expr;
import software.amazon.smithy.rulesengine.language.syntax.fn.BooleanEquals;
import software.amazon.smithy.rulesengine.language.util.SourceLocationTrackingBuilder;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

@SmithyUnstableApi
public final class Parameter implements ToSmithyBuilder<Parameter>, ToParameterReference, FromSourceLocation, ToNode {
    public static final String TYPE = "type";
    public static final String DEPRECATED = "deprecated";
    public static final String DOCUMENTATION = "documentation";
    public static final String DEFAULT = "default";
    private static final String BUILT_IN = "builtIn";
    private static final String REQUIRED = "required";
    private final ParameterType type;
    private final Identifier name;

    private final Value value;

    private final String builtIn;

    private final Value defaultValue;

    private final boolean required;

    private final SourceLocation sourceLocation;

    private final Deprecated deprecated;

    private final String documentation;

    public Parameter(Builder builder) {
        if (builder.defaultValue != null && builder.builtIn == null) {
            throw new RuntimeException("Cannot set a default value for non-builtin parameters");
        }
        if (builder.defaultValue != null && !builder.required) {
            throw new RuntimeException("When a default value is set, the field must also be marked as required");
        }
        this.type = SmithyBuilder.requiredState("type", builder.type);
        this.name = SmithyBuilder.requiredState("name", builder.name);
        this.builtIn = builder.builtIn;
        this.value = builder.value;
        this.required = builder.required;
        this.sourceLocation = builder.getSourceLocation();
        this.deprecated = builder.deprecated;
        this.documentation = builder.documentation;
        this.defaultValue = builder.defaultValue;
    }

    public static Parameter fromNode(StringNode name, ObjectNode node) throws RuleError {
        // TODO: support documentation from JSON
        return RuleError.ctx("while parsing the parameter `" + name + "`", node, () -> {
            node.expectNoAdditionalProperties(Arrays.asList(BUILT_IN, REQUIRED, TYPE, DEPRECATED, DOCUMENTATION,
                    DEFAULT));
            Builder builder = new Builder(node.getSourceLocation());
            String builtIn = node.getStringMember(BUILT_IN).map(StringNode::getValue).orElse(null);
            ParameterType parameterType = RuleError.ctx("while parsing the parameter type", node,
                    () -> ParameterType.fromNode(node.expectStringMember(TYPE)));
            Optional<Deprecated> deprecated = node.getObjectMember(DEPRECATED).map(Deprecated::fromNode);
            deprecated.ifPresent(builder::deprecated);

            node.getStringMember(DOCUMENTATION).map(StringNode::getValue).ifPresent(builder::documentation);
            node.getMember(DEFAULT).map(Value::fromNode).ifPresent(builder::defaultValue);

            boolean required = node.getBooleanMemberOrDefault(REQUIRED, false);
            return builder.name(Identifier.of(name)).builtIn(builtIn).type(parameterType).required(required).build();
        });
    }

    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    public Optional<String> getBuiltIn() {
        return Optional.ofNullable(builtIn);
    }

    public Optional<Value> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public Optional<Deprecated> getDeprecated() {
        return Optional.ofNullable(deprecated);
    }

    public boolean isRequired() {
        return required;
    }

    public ParameterType getType() {
        return type;
    }

    public Type toType() {
        Type out;

        switch (this.type) {
            case STRING:
                out = Type.str();
                break;
            case BOOLEAN:
                out = Type.bool();
                break;
            default:
                throw new IllegalArgumentException("unexpected parameter type: " + this.type);
        }

        if (defaultValue != null) {
            if (!defaultValue.type().equals(out)) {
                throw new RuntimeException(String.format("Invalid type for field \"default\": Type must match "
                                                         + "parameter type. Expected %s, found %s.", out,
                        defaultValue.type()));
            }
        }
        if (!this.required) {
            out = Type.optional(out);
        }
        return out;
    }

    public Identifier getName() {
        return name;
    }

    public boolean isBuiltIn() {
        return builtIn != null;
    }

    public Optional<Value> getValue() {
        return Optional.ofNullable(value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, value, builtIn, required);
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
        return required == parameter.required && type == parameter.type && name.equals(parameter.name)
               && Objects.equals(value, parameter.value) && Objects.equals(builtIn, parameter.builtIn)
               && Objects.equals(defaultValue, parameter.defaultValue)
               && Objects.equals(deprecated, parameter.deprecated)
               && Objects.equals(documentation, parameter.documentation);
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

    @Override
    public Builder toBuilder() {
        return builder()
                .type(getType())
                .name(getName())
                .builtIn(builtIn)
                .documentation(documentation)
                .value(value);
    }

    @Override
    public ParameterReference toParameterReference() {
        return ParameterReference.builder()
                .name(getName().asString())
                .build();
    }

    public String template() {
        return "{" + name + "}";
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

    public Expr expr() {
        return Expr.ref(this.name, SourceLocation.none());
    }

    public BooleanEquals eq(boolean b) {
        return BooleanEquals.fromParam(this, Expr.of(b));
    }

    public BooleanEquals eq(Expr e) {
        return BooleanEquals.fromParam(this, e);
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
        return Optional.ofNullable(this.defaultValue);
    }

    public static final class Deprecated implements ToNode {
        private static final String MESSAGE = "message";
        private static final String SINCE = "since";
        private final String message;
        private final String since;

        public Deprecated(String message, String since) {
            this.message = message;
            this.since = since;
        }

        public static Deprecated fromNode(ObjectNode objectNode) {
            String message = objectNode.getStringMember(MESSAGE).map(StringNode::getValue).orElse(null);
            String since = objectNode.getStringMember(SINCE).map(StringNode::getValue).orElse(null);
            return new Deprecated(message, since);
        }

        @Override
        public Node toNode() {
            NodeMapper mapper = new NodeMapper();
            mapper.disableToNodeForClass(Deprecated.class);
            return mapper.serialize(this);
        }

        public String getMessage() {
            return message;
        }

        public String getSince() {
            return since;
        }

        @Override
        public int hashCode() {
            return Objects.hash(message, since);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            Deprecated that = (Deprecated) obj;
            return Objects.equals(this.message, that.message)
                   && Objects.equals(this.since, that.since);
        }

        @Override
        public String toString() {
            return "Deprecated["
                   + "message=" + message + ", "
                   + "since=" + since + ']';
        }

    }

    public static final class Builder extends SourceLocationTrackingBuilder<Builder, Parameter> {
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

        public Builder sourceLocation(SourceLocation value) {
            this.sourceLocation = value;
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
