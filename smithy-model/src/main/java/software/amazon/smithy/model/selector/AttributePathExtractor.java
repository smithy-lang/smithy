/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * A precompiled extraction pipeline that resolves an attribute path from a shape to an {@link AttributeValue}.
 */
@FunctionalInterface
interface AttributePathExtractor {
    /**
     * Extracts the attribute value from a shape.
     *
     * @param shape The shape to extract from.
     * @param vars The current variable bindings (needed for {@code [var|name]} access).
     * @return The resolved attribute value, or {@link AttributeValueImpl#EMPTY} if not present.
     */
    AttributeValue extract(Shape shape, Map<String, Set<Shape>> vars);

    /**
     * Compiles a path into a precompiled extractor.
     *
     * @param path The attribute path segments.
     * @return A precompiled extractor (never null).
     */
    static AttributePathExtractor compile(List<String> path) {
        if (path.isEmpty()) {
            return AttributeValue::shape;
        }

        switch (path.get(0)) {
            case "trait": {
                AttributePathExtractor specialized = compileTrait(path);
                if (specialized != null) {
                    return specialized;
                }
                break;
            }
            case "id": {
                AttributePathExtractor specialized = compileId(path);
                if (specialized != null) {
                    return specialized;
                }
                break;
            }
            default:
                break;
        }

        // Generic fallback: walks the path at runtime via the AttributeValue chain.
        return (shape, vars) -> AttributeValue.shape(shape, vars).getPath(path);
    }

    static AttributePathExtractor compileTrait(List<String> path) {
        if (path.size() < 2) {
            // Bare [trait]: checking if a shape has any traits at all is rare, so fall back.
            return null;
        }

        String traitName = path.get(1);
        if (traitName.startsWith("(")) {
            // Projection: (keys), (values), (length). These allocate lists; compile as direct calls.
            return null;
        }

        // Resolve the trait ShapeId once.
        ShapeId traitId = ShapeId.from(Trait.makeAbsoluteName(traitName));

        if (path.size() == 2) {
            // [trait|X] with a comparator: extract the trait's node value.
            return (shape, vars) -> {
                Trait trait = shape.findTrait(traitId).orElse(null);
                return trait == null
                        ? AttributeValueImpl.EMPTY
                        : AttributeValue.node(trait.toNode());
            };
        }

        // [trait|X|property|...]: if all remaining segments are plain member names (no pseudo-properties),
        // walk the raw Node tree directly without intermediate AttributeValue allocations.
        List<String> remainingPath = path.subList(2, path.size());
        if (remainingPath.stream().noneMatch(s -> s.startsWith("("))) {
            return (shape, vars) -> {
                Trait trait = shape.findTrait(traitId).orElse(null);
                if (trait == null) {
                    return AttributeValueImpl.EMPTY;
                }
                Node current = trait.toNode();
                for (String segment : remainingPath) {
                    if (!current.isObjectNode()) {
                        return AttributeValueImpl.EMPTY;
                    }
                    current = current.expectObjectNode().getMember(segment).orElse(null);
                    if (current == null) {
                        return AttributeValueImpl.EMPTY;
                    }
                }
                return AttributeValue.node(current);
            };
        }

        // Remaining path has pseudo-properties, so fall back to AttributeValue chain for projections/length.
        return (shape, vars) -> {
            Trait trait = shape.findTrait(traitId).orElse(null);
            if (trait == null) {
                return AttributeValueImpl.EMPTY;
            }
            AttributeValue current = AttributeValue.node(trait.toNode());
            for (String segment : remainingPath) {
                current = current.getProperty(segment);
                if (!current.isPresent()) {
                    return AttributeValueImpl.EMPTY;
                }
            }
            return current;
        };
    }

    static AttributePathExtractor compileId(List<String> path) {
        if (path.size() == 1) {
            // [id]
            return (shape, vars) -> AttributeValue.id(shape.getId());
        } else if (path.size() > 2) {
            // [id|name|...]: invalid, properties don't have sub-properties, fall back.
            return null;
        }

        String property = path.get(1);
        switch (property) {
            case "name":
                return (shape, vars) -> AttributeValue.literal(shape.getId().getName());
            case "namespace":
                return (shape, vars) -> AttributeValue.literal(shape.getId().getNamespace());
            case "member":
                return (shape, vars) -> shape.getId()
                        .getMember()
                        .map(AttributeValue::literal)
                        .orElse(AttributeValueImpl.EMPTY);
            default:
                // Includes (length) or other unknown property, fall back to generic.
                return null;
        }
    }
}
