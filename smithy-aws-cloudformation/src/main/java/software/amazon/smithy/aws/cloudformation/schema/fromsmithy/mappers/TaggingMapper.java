/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers;

import static software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers.HandlerPermissionMapper.getPermissionsEntriesForOperation;

import java.util.Optional;
import software.amazon.smithy.aws.cloudformation.schema.CfnConfig;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CfnMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.Context;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
import software.amazon.smithy.aws.cloudformation.schema.model.Tagging;
import software.amazon.smithy.aws.cloudformation.traits.CfnResource;
import software.amazon.smithy.aws.cloudformation.traits.CfnResourceIndex;
import software.amazon.smithy.aws.traits.tagging.AwsTagIndex;
import software.amazon.smithy.aws.traits.tagging.TaggableTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.utils.SmithyInternalApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * Generates the resource's Tagging configuration based on the AwsTagIndex, including
 * the tagging property and operations that interact with tags.
 *
 * @see <a href="https://github.com/aws-cloudformation/cloudformation-cli/blob/master/src/rpdk/core/data/schema/provider.definition.schema.v1.json#L198">permissions property definition</a>
 */
@SmithyInternalApi
public final class TaggingMapper implements CfnMapper {
    private static final String DEFAULT_TAGS_NAME = "Tags";

    @SmithyInternalApi
    public static void injectTagsMember(
            CfnConfig config,
            Model model,
            ResourceShape resource,
            StructureShape.Builder builder
    ) {
        String tagMemberName = getTagMemberName(config, resource);
        if (resource.hasTrait(TaggableTrait.class)) {
            AwsTagIndex tagIndex = AwsTagIndex.of(model);
            TaggableTrait trait = resource.expectTrait(TaggableTrait.class);
            CfnResourceIndex resourceIndex = CfnResourceIndex.of(model);
            CfnResource cfnResource = resourceIndex.getResource(resource).get();

            if (!trait.getProperty().isPresent() || !cfnResource.getProperties()
                    .containsKey(trait.getProperty().get())) {
                if (trait.getProperty().isPresent()) {
                    ShapeId definition = resource.getProperties().get(trait.getProperty().get());
                    builder.addMember(tagMemberName, definition);
                } else {
                    // A valid TagResource operation certainly has a single tags input member.
                    Optional<ShapeId> tagOperation = tagIndex.getTagResourceOperation(resource.getId());
                    MemberShape member = tagIndex.getTagsMember(tagOperation.get()).get();
                    member = member.toBuilder().id(builder.getId().withMember(tagMemberName)).build();
                    builder.addMember(member);
                }
            }
        }
    }

    @Override
    public ResourceSchema after(Context context, ResourceSchema resourceSchema) {
        ResourceShape resourceShape = context.getResource();
        if (!resourceShape.hasTrait(TaggableTrait.class)) {
            return resourceSchema;
        }

        Model model = context.getModel();
        ServiceShape service = context.getService();
        AwsTagIndex tagsIndex = AwsTagIndex.of(model);
        TaggableTrait trait = resourceShape.expectTrait(TaggableTrait.class);
        Tagging.Builder tagBuilder = Tagging.builder()
                .taggable(true)
                .tagOnCreate(tagsIndex.isResourceTagOnCreate(resourceShape.getId()))
                .tagProperty("/properties/" + getTagMemberName(context.getConfig(), resourceShape))
                .cloudFormationSystemTags(!trait.getDisableSystemTags())
                // Unless tag-on-create is supported, Smithy tagging means
                .tagUpdatable(true);

        // Add the tagging permissions based on the defined tagging operations.
        tagsIndex.getTagResourceOperation(resourceShape)
                .map(operation -> getPermissionsEntriesForOperation(model, service, operation))
                .ifPresent(tagBuilder::addPermissions);
        tagsIndex.getListTagsForResourceOperation(resourceShape)
                .map(operation -> getPermissionsEntriesForOperation(model, service, operation))
                .ifPresent(tagBuilder::addPermissions);
        tagsIndex.getUntagResourceOperation(resourceShape)
                .map(operation -> getPermissionsEntriesForOperation(model, service, operation))
                .ifPresent(tagBuilder::addPermissions);

        return resourceSchema.toBuilder().tagging(tagBuilder.build()).build();
    }

    private static String getTagMemberName(CfnConfig config, ResourceShape resource) {
        return resource.getTrait(TaggableTrait.class)
                .flatMap(TaggableTrait::getProperty)
                .map(property -> {
                    if (config.getDisableCapitalizedProperties()) {
                        return property;
                    }
                    return StringUtils.capitalize(property);
                })
                .orElse(DEFAULT_TAGS_NAME);
    }
}
