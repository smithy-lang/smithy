/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers;

import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CfnMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.Context;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema.Builder;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Generates the resource's deprecated properties list based on the
 * deprecated operation members that are part of the derived resource.
 *
 * @see <a href="https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-deprecatedproperties">deprecatedProperties Docs</a>
 */
@SmithyInternalApi
final class DeprecatedMapper implements CfnMapper {

    @Override
    public void before(Context context, Builder resourceSchema) {
        if (context.getConfig().getDisableDeprecatedPropertyGeneration()) {
            return;
        }

        // If any of the pseudo-resource structure's members are deprecated,
        // then deprecate the CFN property as well.
        Model model = context.getModel();
        StructureShape resourceStructure = context.getResourceStructure();
        for (MemberShape member : resourceStructure.members()) {
            if (member.getMemberTrait(model, DeprecatedTrait.class).isPresent()) {
                resourceSchema.addDeprecatedProperty(context.getPropertyPointer(member.getMemberName()));
            }
        }
    }
}
