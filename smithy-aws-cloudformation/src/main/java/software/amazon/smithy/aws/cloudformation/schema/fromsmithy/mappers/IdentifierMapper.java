/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
