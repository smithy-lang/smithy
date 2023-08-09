/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.openapi;

import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.traits.SpecificationExtensionTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class OpenApiUtils {
    private OpenApiUtils() {}

    /**
     * Gets the specification extension name for a given meta trait.
     *
     * Either an explicitly configured extension name in specificationExtensionTrait, or a normalization of the shape
     * ID. The normalization replaces all "." and "#" in a shapeId to "-".
     *
     * @param metaTraitId Trait shape to get the extension name.
     * @return Extension name for the given trait shape.
     */
    public static String getSpecificationExtensionName(
        ShapeId metaTraitId,
        SpecificationExtensionTrait specificationExtensionTrait
    ) {
        return specificationExtensionTrait.getAs()
            .orElse("x-" + metaTraitId.toString().replaceAll("[.#]", "-"));
    }

    /**
     * Return specification extensions attached to a given shape.
     *
     * @param shape    Shape to get extensions for.
     * @param model    Model the shape belongs to.
     * @return map of specification extension names to node values
     */
    public static Map<String, Node> getSpecificationExtensionsMap(Model model, Shape shape) {
        Map<String, Node> specificationExtensions = new LinkedHashMap<String, Node>();
        shape.getAllTraits().forEach((traitId, trait) ->
            // Get Applied Trait
            model.getShape(traitId)
                    // Get SpecificationExtensionTrait on the Applied Trait
                    .flatMap(traitShape -> traitShape.getTrait(SpecificationExtensionTrait.class))
                    // Get specification extension name from the Applied Trait and SpecificationExtensionTrait
                    .map(specificationExtension -> getSpecificationExtensionName(traitId, specificationExtension))
                    // Put the specification extension name and Applied Meta trait into the map.
                    .ifPresent(name -> specificationExtensions.put(name, trait.toNode())));
        return specificationExtensions;
    }
}
