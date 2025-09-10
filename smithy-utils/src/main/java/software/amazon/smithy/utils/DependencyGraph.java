/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * A basic dependency graph.
 *
 * <p>Iteration and ordering in collection methods are based on insertion order.
 * A topographically sorted view of the graph is provided by {@link #toSortedList()}.
 *
 * @param <T> The data type stored in the graph.
 */
public class DependencyGraph<T> implements Collection<T>, Iterable<T> {

    private final Map<T, Set<T>> forwardDependencies;
    private final Map<T, Set<T>> reverseDependencies;

    /**
     * Constructs a new, empty dependency graph with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the dependency graph.
     */
    public DependencyGraph(int initialCapacity) {
        this.forwardDependencies = new LinkedHashMap<>(initialCapacity);
        this.reverseDependencies = new LinkedHashMap<>(initialCapacity);
    }

    /**
     * Constructs a new dependency graph with the same elements as the specified
     * collection. The dependency graph is created with an initial capacity equal
     * to the size of the collection.
     *
     * @param c the collection whose elements are to be added to this graph.
     */
    public DependencyGraph(Collection<T> c) {
        this(c.size());
        this.addAll(c);
    }

    /**
     * Constructs a new dependency graph with the default initial capacity (16).
     */
    public DependencyGraph() {
        this(16);
    }

    @Override
    public boolean add(T t) {
        if (forwardDependencies.containsKey(t)) {
            return false;
        }
        forwardDependencies.put(t, new LinkedHashSet<>());
        reverseDependencies.put(t, new LinkedHashSet<>());
        return true;
    }

    /**
     * Adds a dependency between two nodes.
     *
     * <p>If either node is not already present in the graph, it is added.
     *
     * @param what The node to add a dependency to.
     * @param dependsOn The dependency to add.
     */
    public void addDependency(T what, T dependsOn) {
        if (!forwardDependencies.containsKey(what)) {
            add(what);
        }
        if (!forwardDependencies.containsKey(dependsOn)) {
            add(dependsOn);
        }
        forwardDependencies.get(what).add(dependsOn);
        reverseDependencies.get(dependsOn).add(what);
    }

    /**
     * Adds a set of dependencies to a node.
     *
     * <p>If any node is not already present in the graph, it is added.
     *
     * @param what The node to add dependencies to.
     * @param dependsOn The dependencies to add.
     */
    public void addDependencies(T what, Collection<T> dependsOn) {
        if (!forwardDependencies.containsKey(what)) {
            add(what);
        }
        Set<T> dependencies = forwardDependencies.get(what);
        for (T dependency : dependsOn) {
            if (dependencies.contains(dependency)) {
                continue;
            }
            if (!forwardDependencies.containsKey(dependency)) {
                add(dependency);
            }
            dependencies.add(dependency);
            reverseDependencies.get(dependency).add(what);
        }
    }

    /**
     * Sets the dependencies for a node.
     *
     * <p>If the node already has dependencies, they will be replaced.
     *
     * @param what The node to set dependencies for.
     * @param dependsOn The dependencies to set.
     */
    public void setDependencies(T what, Collection<T> dependsOn) {
        Set<T> current = forwardDependencies.get(what);
        if (current != null && !current.isEmpty()) {
            List<T> toRemove = new ArrayList<>();
            for (T dependency : current) {
                if (!dependsOn.contains(dependency)) {
                    toRemove.add(dependency);
                }
            }
            removeDependencies(what, toRemove);
        }

        addDependencies(what, dependsOn);
    }

    @Override
    public boolean remove(Object o) {
        if (!forwardDependencies.containsKey(o)) {
            return false;
        }
        for (T dependant : reverseDependencies.get(o)) {
            forwardDependencies.get(dependant).remove(o);
        }
        forwardDependencies.remove(o);
        reverseDependencies.remove(o);
        return true;
    }

    /**
     * Removes a dependency from a node.
     *
     * @param what The node to remove a dependency from.
     * @param dependsOn The dependency to remove.
     */
    public void removeDependency(T what, T dependsOn) {
        forwardDependencies.get(what).remove(dependsOn);
        reverseDependencies.get(dependsOn).remove(what);
    }

    /**
     * Removes a set of dependencies from a node.
     *
     * @param what The node to remove dependencies from.
     * @param dependsOn The dependencies to remove.
     */
    public void removeDependencies(T what, Collection<T> dependsOn) {
        forwardDependencies.get(what).removeAll(dependsOn);
        for (T dependency : dependsOn) {
            reverseDependencies.get(dependency).remove(what);
        }
    }

    /**
     * @return Returns a set of nodes that have no remaining dependencies.
     */
    public Set<T> getIndependentNodes() {
        Set<T> result = new LinkedHashSet<>();
        for (Map.Entry<T, Set<T>> node : forwardDependencies.entrySet()) {
            if (node.getValue().isEmpty()) {
                result.add(node.getKey());
            }
        }
        return result;
    }

    /**
     * Gets all the direct dependencies of a given node.
     *
     * @param node The node whose dependencies should be fetched.
     * @return A set of dependencies.
     */
    public Set<T> getDirectDependencies(T node) {
        Set<T> result = forwardDependencies.get(node);
        if (result == null) {
            return null;
        }
        return SetUtils.copyOf(result);
    }

    /**
     * Gets all the nodes that depend on a given node.
     *
     * @param node The node whose dependants should be fetched.
     * @return A set of dependants.
     */
    public Set<T> getDirectDependants(T node) {
        Set<T> result = reverseDependencies.get(node);
        if (result == null) {
            return null;
        }
        return SetUtils.copyOf(result);
    }

    /**
     * Finds all strongly-connected components of the graph.
     *
     * <p>Cycles returned are not *elementary* cycles. That is, if two or more
     * cycles share any nodes, they will be returned as a single cycle. For
     * example, take the following graph:
     *
     * <pre>
     *     A -> B -> C -> A
     *          B -> D -> B
     * </pre>
     *
     * <p>This graph has one set of strongly-connected components
     * ({@code {B, C, A, D}}) made up of two elementary cycles
     * ({@code {B, C, A}} and {@code D, B}). This method will return the
     * set of strongly-connected components, {@code {B, C, A, D}}.
     *
     * @return A list of all strongly-connected components in the graph.
     */
    public List<List<T>> findCycles() {
        List<List<T>> cycles = new ArrayList<>();
        Map<T, Integer> indexes = new HashMap<>(size());
        Map<T, Integer> lowLinks = new HashMap<>(size());
        Deque<T> stack = new ArrayDeque<>(size());
        Set<T> onStack = new HashSet<>(size());
        int index = 0;

        for (T node : reverseDependencies.keySet()) {
            if (!indexes.containsKey(node)) {
                index = strongConnect(node, indexes, lowLinks, stack, onStack, cycles, index);
            }
        }
        return cycles;
    }

    private int strongConnect(
            T current,
            Map<T, Integer> indexes,
            Map<T, Integer> lowLinks,
            Deque<T> stack,
            Set<T> onStack,
            List<List<T>> cycles,
            int index
    ) {
        indexes.put(current, index);
        lowLinks.put(current, index);
        index++;
        stack.push(current);
        onStack.add(current);

        for (T dependent : reverseDependencies.get(current)) {
            if (!indexes.containsKey(dependent)) {
                index = strongConnect(dependent, indexes, lowLinks, stack, onStack, cycles, index);
                lowLinks.put(current, Math.min(lowLinks.get(current), lowLinks.get(dependent)));
            } else if (onStack.contains(dependent)) {
                lowLinks.put(current, Math.min(lowLinks.get(current), indexes.get(dependent)));
            }
        }

        if (lowLinks.get(current).equals(indexes.get(current))) {
            List<T> cycle = new ArrayList<>();
            T node;
            do {
                node = stack.pop();
                onStack.remove(node);
                cycle.add(node);
            } while (!node.equals(current));
            if (cycle.size() > 1) {
                cycles.add(cycle);
            }
        }
        return index;
    }

    @Override
    public int size() {
        return forwardDependencies.size();
    }

    @Override
    public boolean isEmpty() {
        return forwardDependencies.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return forwardDependencies.containsKey(o);
    }

    @Override
    public Iterator<T> iterator() {
        return forwardDependencies.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return forwardDependencies.keySet().toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return forwardDependencies.keySet().toArray(a);
    }

    /**
     * Gets a topographically sorted view of the graph.
     *
     * @return Returns a topographically sorted list view of the graph.
     */
    public List<T> toSortedList() {
        return toSortedList(new ArrayDeque<>());
    }

    /**
     * Gets a topographically sorted view of the graph where independent
     * nodes are evaluated in the order given by the comparator.
     *
     * @param comparator A comparator used to sort independent nodes.
     * @return Returns a topographically sorted list view of the graph.
     */
    public List<T> toSortedList(Comparator<T> comparator) {
        return toSortedList(new PriorityQueue<>(comparator));
    }

    private List<T> toSortedList(Queue<T> satisfied) {
        // Create a mapping of dependency counts so we don't have to modify the graph.
        Map<T, Integer> inDegree = new HashMap<>(forwardDependencies.size());
        for (Map.Entry<T, Set<T>> entry : forwardDependencies.entrySet()) {
            int degree = entry.getValue().size();
            inDegree.put(entry.getKey(), degree);

            // If the node has no dependencies, go ahead and add it to the queue.
            if (entry.getValue().isEmpty()) {
                satisfied.offer(entry.getKey());
            }
        }

        List<T> result = new ArrayList<>(forwardDependencies.size());

        // Process nodes in priority order.
        while (!satisfied.isEmpty()) {
            T node = satisfied.poll();
            result.add(node);

            // For each dependent node, decrease its dependency count by one.
            for (T dependent : reverseDependencies.get(node)) {
                int newCount = inDegree.get(dependent) - 1;
                inDegree.put(dependent, newCount);

                // If all dependencies are satisfied, add the dependent to the queue.
                if (newCount == 0) {
                    satisfied.add(dependent);
                }
            }
        }

        // Check for cycles.
        if (result.size() != reverseDependencies.size()) {
            List<String> remaining = new ArrayList<>(reverseDependencies.size() - result.size());
            for (T node : reverseDependencies.keySet()) {
                if (!result.contains(node)) {
                    remaining.add(node.toString());
                }
            }
            throw new IllegalStateException(
                    String.format("Cycle(s) detected in dependency graph while attempting to sort among [%s]",
                            String.join(", ", remaining)));
        }

        return result;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return forwardDependencies.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean changed = false;
        for (T element : c) {
            changed = add(element) || changed;
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object element : c) {
            changed = remove(element) || changed;
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        List<T> toRemove = new ArrayList<>();
        for (T element : forwardDependencies.keySet()) {
            if (!c.contains(element)) {
                toRemove.add(element);
            }
        }
        return this.removeAll(toRemove);
    }

    @Override
    public void clear() {
        forwardDependencies.clear();
        reverseDependencies.clear();
    }
}
