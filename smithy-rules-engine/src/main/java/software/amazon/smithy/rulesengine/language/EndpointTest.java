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
import static software.amazon.smithy.rulesengine.language.lang.rule.Rule.ENDPOINT;
import static software.amazon.smithy.rulesengine.language.util.NodeUtil.listToNode;
import static software.amazon.smithy.rulesengine.language.util.NodeUtil.mapToNode;
import static software.amazon.smithy.rulesengine.language.util.StringUtils.indent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.eval.RuleEngine;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.lang.Identifier;
import software.amazon.smithy.rulesengine.language.lang.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.lang.parameters.ParameterType;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyBuilder;

public class EndpointTest implements FromSourceLocation, ToNode {
    public static final String EXPECT = "expect";
    public static final String PARAMS = "params";
    public static final String DOCUMENTATION = "documentation";
    public static final String OPERATION_INPUTS = "operationInputs";

    private final String documentation;
    private final Expectation expectation;
    private final Value.Record params;
    private final SourceLocation sourceLocation;
    private final List<OperationInput> operationInputs;

    private EndpointTest(Builder builder) {
        this.documentation = builder.documentation;
        this.expectation = SmithyBuilder.requiredState("expectation", builder.expectation);
        this.params = SmithyBuilder.requiredState("params", builder.params);
        this.operationInputs = builder.operationInputs.copy();
        this.sourceLocation = builder.getSourceLocation();
    }

    public static EndpointTest fromNode(Node node) {
        Builder builder = new Builder(node);
        return ctx("while parsing endpoint test", node, () -> {
            ObjectNode on = node.expectObjectNode("endpoint tests must be objects");
            on.getStringMember(DOCUMENTATION).ifPresent(docs -> builder.documentation(docs.getValue()));
            Value.Record params = Value.fromNode(on.expectObjectMember(PARAMS)).expectRecord();
            builder.params(params);
            builder.expectation(Expectation.fromNode(on.expectObjectMember(EXPECT)));
            on.getArrayMember(OPERATION_INPUTS)
                    .orElse(ArrayNode.arrayNode())
                    .forEach(opInput -> builder.operationInput(OperationInput.fromNode(opInput)));
            return builder.build();
        });
    }

    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    public Expectation getExpectation() {
        return expectation;
    }

    public List<OperationInput> getOperationInputs() {
        return operationInputs;
    }

    public Optional<String> getDocumentation() {
        return Optional.of(documentation);
    }

    public List<Pair<Identifier, Value>> getParams() {
        ArrayList<Pair<Identifier, Value>> out = new ArrayList<>();
        params.forEach((name, value) -> {
            out.add(Pair.of(name, value));
        });
        return out;
    }

    public List<Parameter> getParameters() {
        ArrayList<Parameter> result = new ArrayList<>();
        params.forEach((name, value) -> {

            Parameter.Builder pb = Parameter.builder().name(name).sourceLocation(value.getSourceLocation());

            if (value instanceof Value.Str) {
                pb.type(ParameterType.STRING);
                result.add(pb.build());
            } else if (value instanceof Value.Bool) {
                pb.type(ParameterType.BOOLEAN);
                result.add(pb.build());
            }
        });
        return result;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public void execute(EndpointRuleset ruleset) {
        Value actual = RuleEngine.evaluate(ruleset, this.params.getValue());
        ctx(
                String.format("while executing test case%s", Optional
                        .ofNullable(documentation)
                        .map(d -> " " + d)
                        .orElse("")),
                this,
                () -> expectation.check(actual)
        );
    }

    public Node toNode() {
        ObjectNode.Builder node = ObjectNode.builder();
        if (documentation != null) {
            node.withMember(DOCUMENTATION, documentation);
        }
        return node
                .withMember(PARAMS, params)
                .withMember(EXPECT, expectation)
                .withMember(OPERATION_INPUTS, listToNode(operationInputs))
                .build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentation, expectation, params, operationInputs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EndpointTest that = (EndpointTest) o;
        return Objects.equals(documentation, that.documentation) && expectation.equals(that.expectation)
               && params.equals(that.params) && operationInputs.equals(that.operationInputs);
    }

    /**
     * An {@link OperationInput} represents a fully-configured SDK input that will produce identical endpoint parameters
     * as this test case. An {@link OperationInput} can be used to generate an integration test from an endpoint
     * testcase.
     */
    public static class OperationInput implements ToNode {
        public static final String OPERATION_NAME = "operationName";
        public static final String OPERATION_PARAMS = "operationParams";
        public static final String BUILTIN_PARAMS = "builtinParams";
        public static final String CLIENT_PARAMS = "clientParams";

        private final Identifier operationName;
        private final Map<Identifier, Value> operationParameters;
        private final Map<Identifier, Value> builtinParameters;

        private final Map<Identifier, Value> clientParameters;

        private OperationInput(OperationInput.Builder builder) {
            this.operationName = SmithyBuilder.requiredState(OPERATION_NAME, builder.operationName);
            this.builtinParameters = builder.builtInParameters.copy();
            this.clientParameters = builder.clientParameters.copy();
            this.operationParameters = builder.operationParameters.copy();
        }

        public static OperationInput fromNode(Node node) {
            ObjectNode on = node.expectObjectNode("Operation inputs must be an object node");
            Builder builder = new Builder(node);
            builder.operationName(Identifier.of(on.expectStringMember(OPERATION_NAME)));
            on.getObjectMember(OPERATION_PARAMS).ifPresent(params -> {
                params.getMembers().forEach((key, value) -> builder.operationParameter(Identifier.of(key), Value.fromNode(value)));
            });
            on.getObjectMember(BUILTIN_PARAMS).ifPresent(params -> {
                params.getMembers().forEach((key, value) -> builder.builtInParameter(Identifier.of(key), Value.fromNode(value)));
            });
            on.getObjectMember(CLIENT_PARAMS).ifPresent(params -> {
                params.getMembers().forEach((key, value) -> builder.clientParameter(Identifier.of(key), Value.fromNode(value)));
            });
            return builder.build();
        }

        public static Builder builder() {
            return new Builder(SourceAwareBuilder.javaLocation());
        }

        /**
         * @return The operation name
         */
        public Identifier getOperationName() {
            return operationName;
        }

        /**
         * @return The operation-specific parameters
         */
        public Map<Identifier, Value> getOperationParameters() {
            return operationParameters;
        }

        /**
         * @return The builtIn parameters
         */
        public Map<Identifier, Value> getBuiltinParameters() {
            return builtinParameters;
        }

        /**
         * @return The client-specific parameters
         */
        public Map<Identifier, Value> getClientParameters() {
            return clientParameters;
        }

        @Override
        public int hashCode() {
            return Objects.hash(operationName, operationParameters, builtinParameters, clientParameters);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            OperationInput that = (OperationInput) o;
            return operationName.equals(that.operationName) && operationParameters.equals(that.operationParameters)
                   && builtinParameters.equals(that.builtinParameters) && clientParameters.equals(that.clientParameters);
        }

        @Override
        public Node toNode() {
            ObjectNode.Builder builder = ObjectNode.builder();
            builder.withMember(OPERATION_NAME, operationName);
            builder.withMember(CLIENT_PARAMS, mapToNode(clientParameters));
            builder.withMember(OPERATION_PARAMS, mapToNode(operationParameters));
            builder.withMember(BUILTIN_PARAMS, mapToNode(builtinParameters));
            return builder.build();
        }


        /**
         * Builder for an {@link OperationInput}
         */
        public static class Builder extends SourceAwareBuilder<Builder, OperationInput> {
            private final BuilderRef<Map<Identifier, Value>> operationParameters = BuilderRef.forOrderedMap();
            private final BuilderRef<Map<Identifier, Value>> builtInParameters = BuilderRef.forOrderedMap();
            private final BuilderRef<Map<Identifier, Value>> clientParameters = BuilderRef.forOrderedMap();
            private Identifier operationName;

            public Builder(FromSourceLocation sourceLocation) {
                super(sourceLocation);
            }

            /**
             * Sets the `OperationName` for this parameter. This name MUST NOT be fully qualified.
             *
             * @param operationName The name of the operation
             * @return the builder
             */
            public Builder operationName(Identifier operationName) {
                this.operationName = operationName;
                return this;
            }

            /**
             * Adds an operationParameter to the builder
             *
             * @param key   The name of the operation parameter. Note: this is the member name in the service model NOT
             *              the name of the bound parameter.
             * @param value The value to set the parameter to.
             * @return the builder
             */
            public Builder operationParameter(Identifier key, Value value) {
                this.operationParameters.get().put(key, value);
                return this;
            }

            /**
             * Sets a builtIn parameter
             *
             * @param key   the name of the builtIn, eg. `AWS::Region`
             * @param value the value of the builtIn
             * @return The builder
             */
            public Builder builtInParameter(String key, Value value) {
                this.builtInParameters.get().put(Identifier.of(key), value);
                return this;
            }

            /**
             * Sets a builtIn parameter
             *
             * @param key   the name of the builtIn, eg. `AWS::Region`
             * @param value the value of the builtIn
             * @return The builder
             */
            public Builder builtInParameter(Identifier key, Value value) {
                this.builtInParameters.get().put(key, value);
                return this;
            }

            /**
             * Sets a clientParameter on the builder
             *
             * @param key   The name of the client parameter
             * @param value the value of the client parameter
             * @return the builder
             */
            public Builder clientParameter(Identifier key, Value value) {
                this.clientParameters.get().put(key, value);
                return this;
            }

            /**
             * Construct an {@link OperationInput} from this builder
             *
             * @return The {@link OperationInput}
             */
            public OperationInput build() {
                return new OperationInput(this);
            }

        }
    }

    public static abstract class Expectation implements FromSourceLocation, ToNode {
        public static final String ERROR = "error";

        private SourceLocation sourceLocation;

        public static Expectation fromNode(Node node) {
            ObjectNode on = node.expectObjectNode("test expectation must be an object");
            Optional<StringNode> error = on.getStringMember(ERROR);
            Optional<ObjectNode> endpoint = on.getObjectMember(ENDPOINT);
            Expectation result;
            if (error.isPresent()) {
                result = new Error(error.get().getValue());
            } else if (endpoint.isPresent()) {
                result = new Endpoint(Value.endpointFromNode(endpoint.get()));
            } else {
                throw new RuntimeException("endpoint or URL must be defined");
            }
            result.sourceLocation = node.getSourceLocation();
            return result;
        }

        public static Error error(String message) {
            return new Error(message);
        }

        @Override
        public SourceLocation getSourceLocation() {
            return Optional.ofNullable(sourceLocation).orElse(SourceLocation.none());
        }

        abstract void check(Value value);

        public static class Error extends Expectation {
            private final String message;

            public Error(String message) {
                this.message = Objects.requireNonNull(message);
            }

            public String getMessage() {
                return message;
            }

            @Override
            void check(Value value) {
                ctx("While checking endpoint test (expecting an error)", this, () -> {
                    if (!value.expectString().equals(this.message)) {
                        throw new AssertionError(String.format("Expected error `%s` but got `%s`", this.message, value));
                    }
                });
            }

            @Override
            public Node toNode() {
                return ObjectNode.builder()
                        .withMember("error", message)
                        .build();
            }

            @Override
            public int hashCode() {
                return message.hashCode();
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }

                Error error = (Error) o;

                return message.equals(error.message);
            }
        }

        public static class Endpoint extends Expectation {
            private final Value.Endpoint endpoint;

            public Endpoint(Value.Endpoint endpoint) {
                this.endpoint = endpoint;
            }

            public Value.Endpoint getEndpoint() {
                return endpoint;
            }

            @Override
            void check(Value value) {
                Value.Endpoint actual = value.expectEndpoint();
                if (!actual.equals(this.endpoint)) {
                    throw new AssertionError(
                            String.format("Expected endpoint:%n%s but got:%n%s (generated by %s)",
                                    indent(this.endpoint.toString(), 2),
                                    indent(actual.toString(), 2),
                                    actual.getSourceLocation()));
                }
            }

            @Override
            public Node toNode() {
                return ObjectNode.builder().withMember("endpoint", endpoint).build();
            }

            @Override
            public int hashCode() {
                return Objects.hash(getEndpoint());
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                Endpoint endpoint1 = (Endpoint) o;
                return getEndpoint().equals(endpoint1.getEndpoint());
            }
        }
    }

    public static class Builder extends SourceAwareBuilder<Builder, EndpointTest> {
        private final BuilderRef<List<OperationInput>> operationInputs = BuilderRef.forList();

        private String documentation;
        private Expectation expectation;
        private Value.Record params;

        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder expectation(Expectation expectation) {
            this.expectation = expectation;
            return this;
        }

        public Builder params(Value.Record params) {
            this.params = params;
            return this;
        }


        @Override
        public EndpointTest build() {
            return new EndpointTest(this);
        }

        /**
         * Adds an operationInput to this test case
         *
         * @param operationInput the {@link OperationInput} to add.
         * @return the builder
         */
        public Builder operationInput(OperationInput operationInput) {
            this.operationInputs.get().add(operationInput);
            return this;
        }
    }
}
