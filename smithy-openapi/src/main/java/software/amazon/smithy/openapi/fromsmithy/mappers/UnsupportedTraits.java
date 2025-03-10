/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.mappers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.aws.traits.HttpChecksumTrait;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.model.traits.HostLabelTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Logs each instance of traits and features that are known to not
 * work in OpenAPI.
 */
@SmithyInternalApi
public final class UnsupportedTraits implements OpenApiMapper {
    private static final Logger LOGGER = Logger.getLogger(UnsupportedTraits.class.getName());
    private static final Set<ShapeId> TRAITS = SetUtils.of(
            EndpointTrait.ID,
            HostLabelTrait.ID,
            HttpChecksumTrait.ID);

    @Override
    public byte getOrder() {
        return -128;
    }

    @Override
    public void before(Context<? extends Trait> context, OpenApi.Builder builder) {
        Map<ShapeId, List<ShapeId>> violations = new LinkedHashMap<>();
        for (Shape shape : context.getModel().toSet()) {
            List<ShapeId> unsupportedTraits = new ArrayList<>(TRAITS.size());
            for (ShapeId trait : TRAITS) {
                if (shape.hasTrait(trait)) {
                    unsupportedTraits.add(trait);
                }
            }
            if (!unsupportedTraits.isEmpty()) {
                violations.put(shape.getId(), unsupportedTraits);
            }
        }

        if (violations.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder(
                "Encountered unsupported Smithy traits when converting to OpenAPI:");
        for (Map.Entry<ShapeId, List<ShapeId>> entry : violations.entrySet()) {
            message.append(String.format(
                    " (`%s`: [`%s`])",
                    entry.getKey(),
                    entry.getValue()
                            .stream()
                            .map(ShapeId::toString)
                            .collect(Collectors.joining("`, `"))));
        }
        message.append(". While these traits may still be meaningful to clients and servers using the Smithy "
                + "model directly, they have no direct corollary in OpenAPI and can not be included in "
                + "the generated model.");

        if (context.getConfig().getIgnoreUnsupportedTraits()) {
            LOGGER.warning(message.toString());
        } else {
            throw new OpenApiException(message.toString());
        }
    }
}
