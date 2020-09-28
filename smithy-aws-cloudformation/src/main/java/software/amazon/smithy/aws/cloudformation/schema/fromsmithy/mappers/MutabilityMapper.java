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

import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CfnMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.Context;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
import software.amazon.smithy.aws.cloudformation.traits.CfnResource;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Applies property mutability restrictions to their proper location
 * in the resulting resource schema.
 *
 * @see <a href="https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-createonlyproperties">createOnlyProperties Docs</a>
 * @see <a href="https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-readonlyproperties">readOnlyProperties Docs</a>
 * @see <a href="https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-writeonlyproperties">writeOnlyProperties Docs</a>
 */
@SmithyInternalApi
final class MutabilityMapper implements CfnMapper {

    @Override
    public void before(Context context, ResourceSchema.Builder builder) {
        CfnResource cfnResource = context.getCfnResource();

        // Add any createOnlyProperty entries, if present.
        cfnResource.getCreateOnlyProperties().stream()
                .map(context::getPropertyPointer)
                .forEach(builder::addCreateOnlyProperty);

        // Add any readOnlyProperty entries, if present.
        cfnResource.getReadOnlyProperties().stream()
                .map(context::getPropertyPointer)
                .forEach(builder::addReadOnlyProperty);

        // Add any writeOnlyProperty entries, if present.
        cfnResource.getWriteOnlyProperties().stream()
                .map(context::getPropertyPointer)
                .forEach(builder::addWriteOnlyProperty);
    }
}
