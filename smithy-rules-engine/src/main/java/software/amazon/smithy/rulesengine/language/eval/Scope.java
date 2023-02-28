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

package software.amazon.smithy.rulesengine.language.eval;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.eval.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Scope is a stack for tracking facts for named values of type T.
 *
 * @param <T> The type of values in scope.
 */
@SmithyUnstableApi
public final class Scope<T> {
    private final Deque<ScopeLayer<T>> scope;

    public Scope() {
        this.scope = new ArrayDeque<>();
        this.scope.push(new ScopeLayer<>());
    }

    public static Scope<Value> fromNode(Node node) {
        ObjectNode objectNode = node.expectObjectNode("scope must be loaded from object node");
        Scope<Value> scope = new Scope<>();
        objectNode.getMembers().forEach((member, value) -> scope.insert(member.getValue(), Value.fromNode(value)));
        return scope;
    }

    public void push() {
        scope.push(new ScopeLayer<>());
    }

    public void pop() {
        scope.pop();
    }

    public void insert(String name, T value) {
        this.insert(Identifier.of(name), value);
    }

    public void insert(Identifier name, T value) {
        this.scope.getFirst().getTypes().put(name, value);
    }

    public void setNonNull(Reference name) {
        this.scope.getFirst().getNonNullReferences().add(name);
    }

    public <U> U inScope(Supplier<U> func) {
        this.push();
        try {
            return func.get();
        } finally {
            this.pop();
        }
    }

    @Override
    public String toString() {
        Map<Identifier, T> toPrint = new LinkedHashMap<>();
        for (ScopeLayer<T> layer : scope) {
            toPrint.putAll(layer.getTypes());
        }
        return toPrint.toString();
    }

    public boolean isNonNull(Reference reference) {
        for (ScopeLayer<T> layer : scope) {
            if (layer.getNonNullReferences().contains(reference)) {
                return true;
            }
        }
        return false;
    }

    public T expectValue(Identifier name) throws InnerParseError {
        for (ScopeLayer<T> layer : scope) {
            if (layer.getTypes().containsKey(name)) {
                return layer.getTypes().get(name);
            }
        }
        throw new InnerParseError(String.format("No field named %s", name));
    }

    public Optional<Map.Entry<Identifier, T>> getDeclaration(Identifier name) {
        for (ScopeLayer<T> layer : scope) {
            if (layer.getTypes().containsKey(name)) {
                for (Map.Entry<Identifier, T> type : layer.getTypes().entrySet()) {
                    if (type.getKey().equals(name)) {
                        return Optional.of(type);
                    }
                }
                return Optional.empty();
            }
        }
        return Optional.empty();

    }

    public Optional<T> getValue(Identifier name) {
        for (ScopeLayer<T> layer : scope) {
            if (layer.getTypes().containsKey(name)) {
                return Optional.of(layer.getTypes().get(name));
            }
        }
        return Optional.empty();
    }
}
