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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.RulesComponentBuilder;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

@SmithyUnstableApi
public final class Parameters implements FromSourceLocation, ToNode, ToSmithyBuilder<Parameters>, Iterable<Parameter> {
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
        for (Map.Entry<StringNode, Node> entry : node.getMembers().entrySet()) {
            builder.addParameter(Parameter.fromNode(entry.getKey(),
                    RuleError.context("when parsing parameter", () -> entry.getValue().expectObjectNode())));
        }
        return builder.build();
    }

    public void writeToScope(Scope<Type> scope) {
        for (Parameter parameter : parameters) {
            RuleError.context(String.format("while typechecking %s", parameter.getName()), parameter,
                    () -> scope.insert(parameter.getName(), parameter.toType()));
        }
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public Optional<Parameter> get(Identifier name) {
        for (Parameter parameter : parameters) {
            if (parameter.getName().equals(name)) {
                return Optional.of(parameter);
            }
        }
        return Optional.empty();
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder params = ObjectNode.builder();
        for (Parameter parameter : parameters) {
            params.withMember(parameter.getName().getName(), parameter);
        }
        return params.build();
    }

    @Override
    public Builder toBuilder() {
        Builder b = builder().sourceLocation(sourceLocation);
        parameters.forEach(b::addParameter);
        return b;
    }

    @Override
    public Iterator<Parameter> iterator() {
        return parameters.iterator();
    }

    @Override
    public void forEach(Consumer<? super Parameter> action) {
        parameters.forEach(action);
    }

    @Override
    public Spliterator<Parameter> spliterator() {
        return parameters.spliterator();
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
    public int hashCode() {
        return Objects.hash(parameters);
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

    public static class Builder extends RulesComponentBuilder<Builder, Parameters> {
        private final List<Parameter> parameters = new ArrayList<>();

        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
        }

        public Builder addParameter(Parameter parameter) {
            this.parameters.add(parameter);
            return this;
        }

        @Override
        public Parameters build() {
            return new Parameters(this);
        }
    }
}
