/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.metadata.ShapeClosure;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.selector.SelectorException;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SetUtils;

/**
 * Resolves {@code shapeClosures} metadata into the set of shapes that
 * make up each declared closure.
 *
 * <p>A closure starts from the shapes that match its
 * {@code includeNamespaces} and {@code includeBySelector} criteria and
 * is expanded transitively through directed neighbor relationships. The resolved
 * set may therefore contain member shapes and any prelude shapes
 * reached by walking those neighbors (e.g. {@code smithy.api#String}
 * via a structure member).
 *
 * <p>The {@code rename} member of a closure is also exposed so
 * consumers can apply the renames consistently.
 */
public final class ShapeClosureIndex implements KnowledgeIndex {

    // Only follow directed relationships so a closure descends into members
    // and targets without crawling up to containers or binding services, the
    // same way RecursiveNeighborSelector traverses neighbors.
    private static final Predicate<Relationship> ONLY_DIRECTED =
            rel -> rel.getRelationshipType().getDirection() == RelationshipDirection.DIRECTED;

    private final Map<String, Set<Shape>> closureShapes = new LinkedHashMap<>();
    private final Map<String, Set<Shape>> rootClosureShapes = new LinkedHashMap<>();
    private final Map<String, Map<ShapeId, String>> closureRenames = new LinkedHashMap<>();

    public ShapeClosureIndex(Model model) {
        Map<String, ShapeClosure> closures = ShapeClosure.fromModel(model);
        if (closures.isEmpty()) {
            return;
        }

        Walker walker = new Walker(NeighborProviderIndex.of(model).getProvider());
        for (ShapeClosure closure : closures.values()) {
            computeClosure(model, walker, closure);
            closureRenames.put(closure.getId(), closure.getRename());
        }
    }

    private void computeClosure(Model model, Walker walker, ShapeClosure closure) {
        Set<Shape> roots = new TreeSet<>();

        Set<String> namespaces = closure.getIncludeNamespaces();
        if (!namespaces.isEmpty()) {
            for (Shape shape : model.toSet()) {
                if (namespaces.contains(shape.getId().getNamespace())) {
                    roots.add(shape);
                }
            }
        }

        closure.getIncludeBySelector().ifPresent(selector -> {
            try {
                roots.addAll(Selector.parse(selector).select(model));
            } catch (SelectorException e) {
                // Invalid selectors are reported by ShapeClosureValidator. We swallow
                // the exception here to prevent one invalid closure from breaking
                // the full validation of closures with valid selectors.
            }
        });
        this.rootClosureShapes.put(closure.getId(), SetUtils.orderedCopyOf(roots));

        Set<Shape> result = new TreeSet<>();
        for (Shape root : roots) {
            // A root already reached by an earlier directed walk has its whole
            // subtree covered, so skip re-walking it.
            if (!result.contains(root)) {
                result.addAll(walker.walkShapes(root, ONLY_DIRECTED));
            }
        }
        this.closureShapes.put(closure.getId(), SetUtils.orderedCopyOf(result));
    }

    public static ShapeClosureIndex of(Model model) {
        return model.getKnowledge(ShapeClosureIndex.class, ShapeClosureIndex::new);
    }

    /**
     * Gets the shapes in the named closure, including any prelude shapes
     * that were reached by transitive walking.
     *
     * @param closure The id of the closure to look up.
     * @return The shapes in the closure.
     * @throws ExpectationNotMetException if no closure with the given id is defined in the model.
     */
    public Set<Shape> getShapesInClosure(String closure) {
        if (!closureShapes.containsKey(closure)) {
            throw new ExpectationNotMetException(
                    "No shape closure named `" + closure + "` is defined in the model.",
                    Node.objectNode());
        }
        return closureShapes.get(closure);
    }

    /**
     * Gets the root shapes in the named closure.
     *
     * <p>This only includes shapes directly matched by {@code includeBySelector} and
     * shape that are part of a namespace in {@code includeNamespaces}. It does NOT
     * include any connected shapes that are not themselves matched by the selector
     * or part of an included namespace.
     *
     * @param closure The id of the closure to look up.
     * @return The shapes in the closure.
     * @throws ExpectationNotMetException if no closure with the given id is defined in the model.
     */
    public Set<Shape> getRootShapesInClosure(String closure) {
        if (!rootClosureShapes.containsKey(closure)) {
            throw new ExpectationNotMetException(
                    "No shape closure named `" + closure + "` is defined in the model.",
                    Node.objectNode());
        }
        return rootClosureShapes.get(closure);
    }

    /**
     * Gets the renames declared by the named closure.
     *
     * <p>The keys are the original shape ids and the values are the new
     * names (without a namespace) that consumers should use for those
     * shapes.
     *
     * @param closure The id of the closure to look up.
     * @return The renames declared by the closure.
     * @throws ExpectationNotMetException if no closure with the given id is defined in the model.
     */
    public Map<ShapeId, String> getRenames(String closure) {
        Map<ShapeId, String> renames = closureRenames.get(closure);
        if (renames == null) {
            throw new ExpectationNotMetException(
                    "No shape closure named `" + closure + "` is defined in the model.",
                    Node.objectNode());
        }
        return Collections.unmodifiableMap(renames);
    }

    /**
     * @return The ids of every closure declared in the model.
     */
    public Set<String> getClosureIds() {
        return Collections.unmodifiableSet(closureShapes.keySet());
    }
}
