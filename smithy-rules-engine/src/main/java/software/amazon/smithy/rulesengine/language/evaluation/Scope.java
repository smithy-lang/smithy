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

package software.amazon.smithy.rulesengine.language.evaluation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
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
    private final Deque<ScopeLayer<T>> scope = new ArrayDeque<>();

    public Scope() {
        scope.push(new ScopeLayer<>());
    }

    public static Scope<Value> fromNode(Node node) {
        ObjectNode objectNode = node.expectObjectNode("scope must be loaded from object node");
        Scope<Value> scope = new Scope<>();
        for (Map.Entry<String, Node> entry : objectNode.getStringMap().entrySet()) {
            scope.insert(entry.getKey(), Value.fromNode(entry.getValue()));
        }
        return scope;
    }

    public void push() {
        scope.push(new ScopeLayer<>());
    }

    public void pop() {
        scope.pop();
    }

    public void insert(String name, T value) {
        insert(Identifier.of(name), value);
    }

    public void insert(Identifier name, T value) {
        scope.getFirst().putType(name, value);
    }

    public void setNonNull(Reference name) {
        scope.getFirst().addNonNullReference(name);
    }

    public <U> U inScope(Supplier<U> func) {
        push();
        try {
            return func.get();
        } finally {
            pop();
        }
    }

    @Override
    public String toString() {
        List<String> scopeStrings = new ArrayList<>();
        for (ScopeLayer<T> layer : scope) {
            scopeStrings.add(layer.toString());
        }
        return "{" + String.join(", ", scopeStrings) + "}";
    }

    public boolean isNonNull(Reference reference) {
        for (ScopeLayer<T> layer : scope) {
            if (layer.containsNonNullReference(reference)) {
                return true;
            }
        }
        return false;
    }

    public T expectValue(Identifier name) throws InnerParseError {
        for (ScopeLayer<T> layer : scope) {
            if (layer.containsType(name)) {
                return layer.getType(name);
            }
        }
        throw new InnerParseError(String.format("No field named %s", name));
    }

    public Optional<Map.Entry<Identifier, T>> getDeclaration(Identifier name) {
        for (ScopeLayer<T> layer : scope) {
            if (layer.containsType(name)) {
                return Optional.ofNullable(layer.getTypeEntry(name));
            }
        }
        return Optional.empty();
    }

    public Optional<T> getValue(Identifier name) {
        for (ScopeLayer<T> layer : scope) {
            if (layer.containsType(name)) {
                return Optional.of(layer.getType(name));
            }
        }
        return Optional.empty();
    }
}
