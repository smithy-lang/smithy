/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.mappers;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Logs each instance of traits and features that are known to not
 * work in OpenAPI.
 */
@SmithyInternalApi
public final class UnsupportedTraits implements OpenApiMapper {
    private static final Logger LOGGER = Logger.getLogger(UnsupportedTraits.class.getName());
    private static final Set<String> TRAITS = SetUtils.of("endpoint", "hostLabel", "aws.protocols#httpChecksum");

    @Override
    public byte getOrder() {
        return -128;
    }

    @Override
    public void before(Context<? extends Trait> context, OpenApi.Builder builder) {
        List<Pair<ShapeId, List<String>>> violations = context.getModel()
                .shapes()
                .map(shape -> Pair.of(shape.getId(),
                        TRAITS.stream()
                                .filter(trait -> shape.findTrait(trait).isPresent())
                                .collect(Collectors.toList())))
                .filter(pair -> pair.getRight().size() > 0)
                .collect(Collectors.toList());

        if (violations.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder(
                "Encountered unsupported Smithy traits when converting to OpenAPI:");
        violations.forEach(pair -> message.append(String.format(
                " (`%s`: [%s])",
                pair.getLeft(),
                String.join(",", pair.getRight()))));
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
