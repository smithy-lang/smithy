/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CfnMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.Context;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema.Builder;
import software.amazon.smithy.aws.cloudformation.traits.CfnResource;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Applies the resource's identifier and annotated additional identifiers
 * to the resulting resource schema.
 *
 * @see <a href="https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-primaryidentifier">primaryIdentifier Docs</a>
 * @see <a href="https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-additionalidentifiers">additionalIdentifiers Docs</a>
 */
@SmithyInternalApi
final class IdentifierMapper implements CfnMapper {

    @Override
    public void before(Context context, Builder builder) {
        CfnResource cfnResource = context.getCfnResource();

        // Add the primary identifier.
        Set<String> primaryIdentifier = cfnResource.getPrimaryIdentifiers();
        builder.primaryIdentifier(primaryIdentifier.stream()
                .map(context::getPropertyPointer)
                .collect(Collectors.toList()));

        // Add any additional identifiers.
        List<Set<String>> additionalIdentifiers = cfnResource.getAdditionalIdentifiers();
        for (Set<String> additionalIdentifier : additionalIdentifiers) {
            // Convert the names into their property pointer.
            List<String> additionalIdentifierPointers = new ArrayList<>();
            for (String additionalIdentifierName : additionalIdentifier) {
                additionalIdentifierPointers.add(context.getPropertyPointer(additionalIdentifierName));
            }

            builder.addAdditionalIdentifier(additionalIdentifierPointers);
        }
    }
}
