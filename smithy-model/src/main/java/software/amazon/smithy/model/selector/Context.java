/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Selector evaluation context object.
 */
final class Context {

    NeighborProviderIndex neighborIndex;
    private final Model model;
    private final List<Set<Shape>> roots;

    // Selector variables are addressed by a dense integer slot assigned at parse time rather than by name. This lets
    // the hot path (storing and getting variables) use plain array indexing instead of hash map lookups, and lets the
    // per-shape variable reset be an array fill instead of a HashMap.clear(). The map-based view is only materialized
    // lazily for the public, name-keyed API surface (ShapeMatch and [var|name] access).
    private final Map<String, Integer> variableIndices;
    private final Set<Shape>[] variableSlots;
    private VarMap varsView;

    // Pre-allocated Holder stack to avoid per-call allocation in receivedShapes. Handles reentrancy through growth.
    private Holder[] holders = new Holder[] { new Holder(), new Holder() };
    private int holderDepth;

    @SuppressWarnings("unchecked")
    Context(
            Model model,
            NeighborProviderIndex neighborIndex,
            List<Set<Shape>> roots,
            Map<String, Integer> variableIndices
    ) {
        this.model = model;
        this.neighborIndex = neighborIndex;
        this.roots = roots;
        this.variableIndices = variableIndices;
        this.variableSlots = (Set<Shape>[]) new Set<?>[variableIndices.size()];
    }

    /**
     * Gets the variables stored in the given slot, or an empty set if unset.
     *
     * @param slot Slot index assigned to the variable at parse time.
     * @return Returns the captured shapes (never null).
     */
    Set<Shape> getVariable(int slot) {
        Set<Shape> result = variableSlots[slot];
        return result == null ? Collections.emptySet() : result;
    }

    /**
     * Stores variables in the given slot.
     *
     * @param slot Slot index assigned to the variable at parse time.
     * @param value Captured shapes to store.
     */
    void setVariable(int slot, Set<Shape> value) {
        variableSlots[slot] = value;
    }

    /**
     * Clears all captured variables. This is called between starting shapes.
     */
    void clearVariables() {
        Arrays.fill(variableSlots, null);
    }

    /**
     * Gets a mutable map view of the captured variables.
     *
     * <p>This is used by the name-keyed API surface (e.g., {@link Selector.ShapeMatch} and {@code [var|name]}
     * attribute access). The map is a view over the underlying variable slots, so reads and writes go directly
     * to the slots.
     *
     * @return Returns the captured variables.
     */
    Map<String, Set<Shape>> getVars() {
        VarMap result = varsView;
        if (result == null) {
            result = new VarMap();
            varsView = result;
        }
        return result;
    }

    Set<Shape> getRootResult(int index) {
        return roots.get(index);
    }

    Model getModel() {
        return model;
    }

    /**
     * A {@link Map} view over the variable slots, keyed by variable name.
     */
    private final class VarMap extends AbstractMap<String, Set<Shape>> {
        @Override
        public Set<Shape> get(Object key) {
            Integer slot = variableIndices.get(key);
            return slot == null ? null : variableSlots[slot];
        }

        @Override
        public boolean containsKey(Object key) {
            Integer slot = variableIndices.get(key);
            return slot != null && variableSlots[slot] != null;
        }

        @Override
        public Set<Shape> put(String key, Set<Shape> value) {
            Integer slot = variableIndices.get(key);
            if (slot == null) {
                throw new IllegalArgumentException("Unknown selector variable: " + key);
            }
            Set<Shape> previous = variableSlots[slot];
            variableSlots[slot] = value;
            return previous;
        }

        @Override
        public void clear() {
            clearVariables();
        }

        @Override
        public Set<Entry<String, Set<Shape>>> entrySet() {
            return new AbstractSet<Entry<String, Set<Shape>>>() {
                @Override
                public Iterator<Entry<String, Set<Shape>>> iterator() {
                    return new VarEntryIterator();
                }

                @Override
                public int size() {
                    int count = 0;
                    for (Set<Shape> slot : variableSlots) {
                        if (slot != null) {
                            count++;
                        }
                    }
                    return count;
                }
            };
        }
    }

    private final class VarEntryIterator implements Iterator<Map.Entry<String, Set<Shape>>> {
        private final Iterator<Map.Entry<String, Integer>> indices = variableIndices.entrySet().iterator();
        private Map.Entry<String, Integer> next;

        VarEntryIterator() {
            advance();
        }

        private void advance() {
            next = null;
            while (indices.hasNext()) {
                Map.Entry<String, Integer> candidate = indices.next();
                if (variableSlots[candidate.getValue()] != null) {
                    next = candidate;
                    return;
                }
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Map.Entry<String, Set<Shape>> next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            String name = next.getKey();
            Set<Shape> value = variableSlots[next.getValue()];
            advance();
            return new AbstractMap.SimpleImmutableEntry<>(name, value);
        }
    }

    /**
     * Placeholder value used to check if a selector emits any values.
     */
    private static final class Holder implements InternalSelector.Receiver {
        boolean set;

        @Override
        public InternalSelector.Response apply(Context context, Shape shape) {
            set = true;
            // Stop receiving shapes once the first value is seen.
            return InternalSelector.Response.STOP;
        }
    }

    /**
     * Checks if the shape matches the predicate by detecting if the
     * predicate pushes any values when provided the shape.
     *
     * @param shape Shape to push to the given {@code predicate}.
     * @param predicate Predicate to test with the given {@code shape}.
     * @return Returns true if the {@code predicate} matches the {@code shape}.
     */
    boolean receivedShapes(Shape shape, InternalSelector predicate) {
        // Short-circuit side-effect-free predicates that can answer without a full push.
        switch (predicate.emitsAnyOptimization(this, shape)) {
            case YES:
                return true;
            case NO:
                return false;
            case MAYBE:
            default:
                break;
        }

        Holder h = getHolder();
        try {
            predicate.push(this, shape, h);
            return h.set;
        } finally {
            holderDepth--;
        }
    }

    private Holder getHolder() {
        if (holderDepth >= holders.length) {
            Holder[] grown = new Holder[holders.length * 2];
            System.arraycopy(holders, 0, grown, 0, holders.length);
            // Allocate the new part of the list.
            for (int i = holders.length; i < grown.length; i++) {
                grown[i] = new Holder();
            }
            holders = grown;
        }

        Holder h = holders[holderDepth++];
        h.set = false;
        return h;
    }
}
