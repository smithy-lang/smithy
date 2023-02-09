/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.openapi.fromsmithy.mappers;

import java.util.Map;
import java.util.function.BiConsumer;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.Component;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.traits.SpecificationExtensionTrait;

/**
 * Maps trait shapes tagged with {@link SpecificationExtensionTrait} into <a href="https://spec.openapis.org/oas/v3.1.0#specification-extensions">OpenAPI specification extensions</a>.
 */
public class SpecificationExtensionsMapper implements OpenApiMapper {
    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openapi) {
        return attachAllExtensionsFromShape(openapi, context.getModel(), context.getService());
    }

    @Override
    public OperationObject updateOperation(
            Context<? extends Trait> context,
            OperationShape shape,
            OperationObject operation,
            String httpMethodName,
            String path
    ) {
        return attachAllExtensionsFromShape(operation, context.getModel(), shape);
    }

    private static <T extends Component> T attachAllExtensionsFromShape(T component, Model model, Shape shape) {
        Map<String, Node> extensions = component.getExtensions();
        findSpecificationExtensions(model, shape, extensions::put);
        return component;
    }

    /**
     * Find all specification extension trait values attached to the given shape.
     *
     * @param model    Model the shape belongs to.
     * @param shape    Shape to get extensions for.
     * @param consumer Consumer called for each specification extension key-value pair found.
     */
    public static void findSpecificationExtensions(Model model, Shape shape, BiConsumer<String, Node> consumer) {
        shape.getAllTraits().forEach((traitShapeId, trait) ->
                model.getShape(traitShapeId)
                        .flatMap(traitShape -> traitShape.getTrait(SpecificationExtensionTrait.class))
                        .map(specificationExtensionTrait -> specificationExtensionTrait.extensionNameFor(traitShapeId))
                        .ifPresent(name -> consumer.accept(name, trait.toNode())));
    }
}
