/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.mappers;

import java.util.logging.Logger;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Checks for greedy labels and fails/warns depending on configuration.
 *
 * <p>Some vendors like API Gateway support greedy labels in the form of
 * "{foo+}", while others do not.
 */
@SmithyInternalApi
public class CheckForGreedyLabels implements OpenApiMapper {
    private static final Logger LOGGER = Logger.getLogger(CheckForGreedyLabels.class.getName());

    @Override
    public byte getOrder() {
        return -128;
    }

    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openApi) {
        for (String path : openApi.getPaths().keySet()) {
            // Throw an exception or warning when greedy URI labels are found in the path.
            if (path.contains("+}")) {
                String message = "Greedy URI path label found in path `" + path + "`. Not all OpenAPI "
                        + "tools support this style of URI labels. Greedy URI labels are expected "
                        + "to capture all remaining components of a URI, so if a tool does not "
                        + "support them, the API will not function properly.";
                if (context.getConfig().getForbidGreedyLabels()) {
                    throw new OpenApiException(message);
                } else {
                    LOGGER.warning(message);
                }
            }
        }

        return openApi;
    }
}
