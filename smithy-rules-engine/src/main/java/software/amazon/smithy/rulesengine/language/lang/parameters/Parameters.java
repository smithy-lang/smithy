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

package software.amazon.smithy.rulesengine.language.lang.parameters;

import static software.amazon.smithy.rulesengine.language.error.RuleError.ctx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.SourceAwareBuilder;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.lang.Identifier;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

@SmithyUnstableApi
public final class Parameters implements FromSourceLocation, ToNode, ToSmithyBuilder<Parameters> {
    private final List<Parameter> parameters;
    private final SourceLocation sourceLocation;

    public Parameters(Builder builder) {
        this.parameters = builder.parameters;
        this.sourceLocation = builder.getSourceLocation();
    }

    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    public static Parameters fromNode(ObjectNode node) throws RuleError {
        Builder builder = new Builder(node);
        for (Map.Entry<StringNode, Node> kv : node.getMembers().entrySet()) {
            builder.addParameter(
                    Parameter.fromNode(
                            kv.getKey(),
                            ctx("when parsing parameter", () -> kv.getValue().expectObjectNode())));
        }
        return builder.build();
    }

    public void writeToScope(Scope<Type> scope) {
        this.parameters.forEach(
                (param) ->
                        ctx(
                                String.format("while typechecking %s", param.getName()),
                                param,
                                () -> scope.insert(param.getName(), param.toType())));
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameters);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Parameters that = (Parameters) o;
        return parameters.equals(that.parameters);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Parameter param : parameters) {
            sb.append(param);
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public Builder toBuilder() {
        return builder();
    }

    public List<Parameter> toList() {
        return parameters;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public Optional<Parameter> get(Identifier name) {
        return parameters.stream().filter((param) -> param.getName().equals(name)).findFirst();
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder params = ObjectNode.builder();
        parameters.forEach(p -> params.withMember(p.getName().getName(), p));
        return params.build();
    }

    public static class Builder extends SourceAwareBuilder<Builder, Parameters> {
        private final List<Parameter> parameters = new ArrayList<>();

        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
        }

        public Builder addParameter(Parameter parameter) {
            this.parameters.add(parameter);
            return this;
        }

        public Builder addParameter(SmithyBuilder<Parameter> parameter) {
            this.parameters.add(parameter.build());
            return this;
        }

        @Override
        public Parameters build() {
            return new Parameters(this);
        }
    }
}
