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

package software.amazon.smithy.rulesengine.reterminus.eval;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.reterminus.error.InnerParseError;
import software.amazon.smithy.rulesengine.reterminus.lang.Identifier;
import software.amazon.smithy.rulesengine.reterminus.lang.expr.Ref;

public class Scope<T> {
    private final Deque<ScopeLayer<T>> scope;

    public Scope() {
        this.scope = new ArrayDeque<>();
        this.scope.push(new ScopeLayer<>());
    }

    public static Scope<Value> fromNode(Node node) {
        ObjectNode on = node.expectObjectNode("scope must be loaded from object node");
        Scope<Value> scope = new Scope<>();
        on.getMembers().forEach((member, value) -> scope.insert(member.getValue(), Value.fromNode(value)));
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

    public void setNonNull(Ref name) {
        this.scope.getFirst().getNonNullRefs().add(name);
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
        HashMap<Identifier, T> toPrint = new HashMap<>();
        for (ScopeLayer<T> layer : scope) {
            toPrint.putAll(layer.getTypes());
        }
        return toPrint.toString();
    }

    public boolean isNonNull(Ref ref) {
        return scope.stream().anyMatch(s -> s.getNonNullRefs().contains(ref));
    }

    public T expectValue(Identifier name) throws InnerParseError {
        for (ScopeLayer<T> layer : scope) {
            if (layer.getTypes().containsKey(name)) {
                return layer.getTypes().get(name);
            }
        }
        throw new InnerParseError(String.format("No field named %s", name));
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
