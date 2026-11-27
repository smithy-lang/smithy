/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.mappers;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.TrailerTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Fails the conversion if a member marked with the {@code trailer} trait is
 * found in the output of an operation being converted.
 *
 * <p>Trailer members have no OpenAPI representation. Without this check,
 * they would be silently omitted because they are always {@code UNBOUND} in
 * the {@link software.amazon.smithy.model.knowledge.HttpBindingIndex}.
 */
@SmithyInternalApi
public final class CheckForTrailers implements OpenApiMapper {
    @Override
    public byte getOrder() {
        return -128;
    }

    @Override
    public void before(Context<? extends Trait> context, OpenApi.Builder builder) {
        Model model = context.getModel();
        OperationIndex operationIndex = OperationIndex.of(model);

        // Only outputs need to be checked: trailers can't be applied to input
        // members, and errors can't contain streaming members.
        for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(context.getService())) {
            for (MemberShape member : operationIndex.getOutputMembers(operation).values()) {
                if (member.hasTrait(TrailerTrait.ID)) {
                    throw new OpenApiException(String.format(
                            "The `trailer` trait is not supported by OpenAPI and was found on `%s`",
                            member.getId()));
                }
            }
        }
    }
}
