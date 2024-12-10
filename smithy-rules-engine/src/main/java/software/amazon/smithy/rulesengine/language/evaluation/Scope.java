/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
 * Scope is a stack for tracking facts for named values of the given type.
 *
 * @param <T> The type of values in scope.
 */
@SmithyUnstableApi
public final class Scope<T> {
    private final Deque<ScopeLayer<T>> scope = new ArrayDeque<>();

    /**
     * Crates a new, empty scope with a single layer.
     */
    public Scope() {
        scope.push(new ScopeLayer<>());
    }

    /**
     * Creates a {@link Scope} instance from the given Node information.
     *
     * @param node the node to deserialize.
     * @return the created Scope.
     */
    public static Scope<Value> fromNode(Node node) {
        ObjectNode objectNode = node.expectObjectNode("scope must be loaded from object node");
        Scope<Value> scope = new Scope<>();
        for (Map.Entry<String, Node> entry : objectNode.getStringMap().entrySet()) {
            scope.insert(entry.getKey(), Value.fromNode(entry.getValue()));
        }
        return scope;
    }

    /**
     * Assesses a value supplier in the current scope.
     *
     * @param <U> the type of the value returned by the supplier.
     * @param supplier the value supplier to assess.
     * @return the value returned by the supplier.
     */
    public <U> U inScope(Supplier<U> supplier) {
        push();
        try {
            return supplier.get();
        } finally {
            pop();
        }
    }

    /**
     * Pushes an empty {@link ScopeLayer} into the current scope.
     */
    public void push() {
        scope.push(new ScopeLayer<>());
    }

    /**
     * Pops the most recent {@link ScopeLayer} out of the current scope.
     */
    public void pop() {
        scope.pop();
    }

    /**
     * Inserts a named value into the current scope.
     *
     * @param name the name of the value to insert.
     * @param value the value to insert.
     */
    public void insert(String name, T value) {
        insert(Identifier.of(name), value);
    }

    /**
     * Inserts a named value into the current scope.
     *
     * @param name the name of the value to insert.
     * @param value the value to insert.
     */
    public void insert(Identifier name, T value) {
        scope.getFirst().putType(name, value);
    }

    /**
     * Inserts a non-null reference into the current scope.
     *
     * @param name the name of the reference to insert.
     */
    public void setNonNull(Reference name) {
        scope.getFirst().addNonNullReference(name);
    }

    /**
     * Gets if a reference is non-null in the current scope.
     *
     * @param reference the reference to check nullability for.
     * @return true if the reference is non-null, false otherwise.
     */
    public boolean isNonNull(Reference reference) {
        for (ScopeLayer<T> layer : scope) {
            if (layer.containsNonNullReference(reference)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the first declaration in scope for the specified identifier.
     *
     * @param name the identifier to retrieve a declaration for.
     * @return an optional of the declaration for the identifier, or empty otherwise.
     */
    public Optional<Map.Entry<Identifier, T>> getDeclaration(Identifier name) {
        for (ScopeLayer<T> layer : scope) {
            if (layer.containsType(name)) {
                return Optional.ofNullable(layer.getTypeEntry(name));
            }
        }
        return Optional.empty();
    }

    /**
     * Gets the first value in scope for the specified identifier, throwing
     * {@link InnerParseError} if the identifier is undefined in all the scope.
     *
     * @param name the identifier to retrieve a declaration for.
     * @return the value for the identifier.
     * @throws InnerParseError when the identifier has no value in the scope.
     */
    public T expectValue(Identifier name) throws InnerParseError {
        for (ScopeLayer<T> layer : scope) {
            if (layer.containsType(name)) {
                return layer.getType(name);
            }
        }
        throw new InnerParseError(String.format("No field named %s", name));
    }

    /**
     * Gets the first value in scope for the specified identifier.
     *
     * @param name the identifier to retrieve a value for.
     * @return an optional of the value for the identifier, or empty otherwise.
     */
    public Optional<T> getValue(Identifier name) {
        for (ScopeLayer<T> layer : scope) {
            if (layer.containsType(name)) {
                return Optional.of(layer.getType(name));
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        List<String> scopeStrings = new ArrayList<>();
        for (ScopeLayer<T> layer : scope) {
            scopeStrings.add(layer.toString());
        }
        return "{" + String.join(", ", scopeStrings) + "}";
    }
}
