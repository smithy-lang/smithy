/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers;

import java.util.Optional;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CfnMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.Context;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
import software.amazon.smithy.aws.cloudformation.traits.CfnResource;
import software.amazon.smithy.aws.cloudformation.traits.CfnResourceIndex;
import software.amazon.smithy.aws.cloudformation.traits.CfnResourceProperty;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Generates the resource's required properties list based on the
 * required operation members that are part of the derived resource.
 *
 * @see <a href="https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html">required Docs</a>
 */
@SmithyInternalApi
public final class RequiredMapper implements CfnMapper {

    @Override
    public void before(Context context, ResourceSchema.Builder resourceSchema) {
        if (context.getConfig().getDisableRequiredPropertyGeneration()) {
            return;
        }
        // If any of the pseudo-resource structure's members are
        // required and are not only used in output structures then
        // require the CFN property as well.
        Model model = context.getModel();
        StructureShape resourceStructure = context.getResourceStructure();
        for (MemberShape member : resourceStructure.members()) {
            if (member.getMemberTrait(model, RequiredTrait.class).isPresent()) {
                if (hasWriteOrCreateMutability(context, member)) {
                    resourceSchema.addRequired(context.getJsonSchemaConverter().toPropertyName(member));
                }
            }
        }
    }

    private boolean hasWriteOrCreateMutability(Context context, MemberShape member) {
        CfnResource cfnResource = context.getCfnResource();
        String memberId = member.getId().toShapeId().getMember().get();
        Optional<CfnResourceProperty> cfnResourceProperty = cfnResource.getProperty(memberId);
        return cfnResourceProperty.map(CfnResourceProperty::getMutabilities)
                .map(mutabilities -> mutabilities.contains(CfnResourceIndex.Mutability.WRITE)
                        || mutabilities.contains(CfnResourceIndex.Mutability.CREATE))
                .orElse(false);
    }
}
