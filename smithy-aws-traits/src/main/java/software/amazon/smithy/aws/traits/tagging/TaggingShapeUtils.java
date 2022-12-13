/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.traits.tagging;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.PropertyBindingIndex;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;

/**
 * Logic for validating that a shape looks like a tag.
 */
final class TaggingShapeUtils {
    static final String TAG_RESOURCE_OPNAME = "TagResource";
    static final String UNTAG_RESOURCE_OPNAME = "UntagResource";
    static final String LIST_TAGS_OPNAME = "ListTagsForResource";

    private static final Pattern TAG_PROPERTY_REGEX = Pattern
            .compile("^[T|t]ag(s|[L|l]ist)$");
    private static final Pattern RESOURCE_ARN_REGEX = Pattern
            .compile("^([R|r]esource)?([A|a]rn|ARN)$");
    private static final Pattern TAG_KEYS_REGEX = Pattern
            .compile("^[T|t]ag[K|k]eys$");

    private TaggingShapeUtils() {}

    // Recommended name is more limited than the accepted regular expression.
    static String getDesiredTagsPropertyName() {
        return "[T|t]ags";
    }

    // Recommended name is more limited than the accepted regular expression.
    static String getDesiredArnName() {
        return "[R|r]esourceArn";
    }

    // Recommended name is more limited than the accepted regular expression.
    static String getDesiredTagKeysName() {
        return "[T|t]agKeys";
    }

    // Used to validate tag property name and tag member name.
    static boolean isTagDesiredName(String memberName) {
        return TAG_PROPERTY_REGEX.matcher(memberName).matches();
    }

    // Used for checking if member name is good for resource ARN input.
    static boolean isArnMemberDesiredName(String memberName) {
        return RESOURCE_ARN_REGEX.matcher(memberName).matches();
    }

    // Used for checking if member name is good for tag keys input for untag operation.
    static boolean isTagKeysDesiredName(String memberName) {
        return TAG_KEYS_REGEX.matcher(memberName).matches();
    }

    static boolean hasResourceArnInput(Map<String, MemberShape> inputMembers, Model model) {
        for (Map.Entry<String, MemberShape> memberEntry : inputMembers.entrySet()) {
            if (TaggingShapeUtils.isArnMemberDesiredName(memberEntry.getKey())
                    && model.expectShape(memberEntry.getValue().getTarget()).isStringShape()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if and only if a provided shape meets criteria that appears to
     * represent a list or map of tag key value pairs.
     *
     * @param model Model to retrieve target shapes from when examining
     *              targets.
     * @param tagShape shape to examine if it appears to be tags.
     * @return true if and only if shape meets the criteria for being a TagList
     */
    static boolean verifyTagsShape(Model model, Shape tagShape) {
        return verifyTagListShape(model, tagShape) || verifyTagMapShape(model, tagShape);
    }

    static boolean verifyTagListShape(Model model, Shape tagShape) {
        if (tagShape.isListShape()) {
            ListShape listShape = tagShape.asListShape().get();
            Shape listTargetShape = model.expectShape(listShape.getMember().getTarget());
            if (listTargetShape.isStructureShape()) {
                StructureShape memberStructureShape = listTargetShape.asStructureShape().get();
                //Verify member count is two, and both point to string types.
                if (memberStructureShape.members().size() == 2) {
                    boolean allStrings = true;
                    for (MemberShape member : memberStructureShape.members()) {
                        allStrings &= model.expectShape(member.getTarget()).isStringShape();
                    }
                    return allStrings;
                }
            }
        }
        return false;
    }

    static boolean verifyTagMapShape(Model model, Shape tagShape) {
        if (tagShape.isMapShape()) {
            MapShape mapShape = tagShape.asMapShape().get();
            Shape valueTargetShape = model.expectShape(mapShape.getValue().getTarget());
            return valueTargetShape.isStringShape();
        }
        return false;
    }

    static boolean verifyTagKeysShape(Model model, Shape tagShape) {
        // A list or set that targets a string shape qualifies as listing tag keys
        return (tagShape.isListShape()
                    && model.expectShape(tagShape.asListShape().get().getMember().getTarget()).isStringShape());
    }

    static boolean verifyTagResourceOperation(Model model,
        ServiceShape service,
        OperationShape tagResourceOperation,
        OperationIndex operationIndex
    ) {
        Map<String, MemberShape> inputMembers = operationIndex.getInputMembers(tagResourceOperation);
        int taglistMemberCount = 0;
        for (Map.Entry<String, MemberShape> memberEntry : inputMembers.entrySet()) {
            if (TaggingShapeUtils.isTagDesiredName(memberEntry.getKey())
                && TaggingShapeUtils.verifyTagsShape(model,
                    model.expectShape(memberEntry.getValue().getTarget()))) {
                ++taglistMemberCount;
            }
        }
        return taglistMemberCount == 1 && TaggingShapeUtils.hasResourceArnInput(inputMembers, model);
    }

    static boolean verifyUntagResourceOperation(
        Model model,
        ServiceShape service,
        OperationShape untagResourceOperation,
        OperationIndex operationIndex
    ) {
        Map<String, MemberShape> inputMembers = operationIndex.getInputMembers(untagResourceOperation);
        int untagKeyMemberCount = 0;
        for (Map.Entry<String, MemberShape> memberEntry : inputMembers.entrySet()) {
            if (TaggingShapeUtils.isTagKeysDesiredName(memberEntry.getKey())
                && TaggingShapeUtils.verifyTagKeysShape(model,
                    model.expectShape(memberEntry.getValue().getTarget()))) {
                ++untagKeyMemberCount;
            }
        }
        return untagKeyMemberCount == 1 && TaggingShapeUtils.hasResourceArnInput(inputMembers, model);
    }

    static boolean verifyListTagsOperation(
        Model model,
        ServiceShape service,
        OperationShape listTagsResourceOperation,
        OperationIndex operationIndex
    ) {
        Map<String, MemberShape> inputMembers = operationIndex.getInputMembers(listTagsResourceOperation);
        Map<String, MemberShape> outputMembers = operationIndex.getOutputMembers(listTagsResourceOperation);
        int taglistMemberCount = 0;
        for (Map.Entry<String, MemberShape> memberEntry : outputMembers.entrySet()) {
            if (TaggingShapeUtils.isTagDesiredName(memberEntry.getKey())
                && TaggingShapeUtils.verifyTagsShape(model,
                    model.expectShape(memberEntry.getValue().getTarget()))) {
                ++taglistMemberCount;
            }
        }
        return taglistMemberCount == 1 && TaggingShapeUtils.hasResourceArnInput(inputMembers, model);
    }

    static boolean isTagPropertyInInput(
        Optional<ShapeId> operationId,
        Model model,
        ResourceShape resource,
        PropertyBindingIndex propertyBindingIndex
    ) {
        Optional<String> property = resource.expectTrait(TaggableTrait.class).getProperty();
        if (property.isPresent()) {
            if (operationId.isPresent()) {
                OperationShape operation = model.expectShape(operationId.get()).asOperationShape().get();
                Shape inputShape = model.expectShape(operation.getInputShape());
                return isTagPropertyInShape(property.get(), inputShape, propertyBindingIndex);
            }
        }
        return false;
    }

    static boolean isTagPropertyInOutput(
        Optional<ShapeId> operationId,
        Model model,
        ResourceShape resource,
        PropertyBindingIndex propertyBindingIndex
    ) {
        Optional<String> property = resource.expectTrait(TaggableTrait.class).getProperty();
        if (property.isPresent()) {
            if (operationId.isPresent()) {
                OperationShape operation = model.expectShape(operationId.get()).asOperationShape().get();
                Shape outputShape = model.expectShape(operation.getOutputShape());
                return isTagPropertyInShape(property.get(), outputShape, propertyBindingIndex);
            }
        }
        return false;
    }

    static boolean isTagPropertyInShape(
        String tagPropertyName,
        Shape shape,
        PropertyBindingIndex propertyBindingIndex
    ) {
        for (MemberShape member : shape.members()) {
            Optional<Boolean> isMatch = propertyBindingIndex.getPropertyName(member.getId())
                    .map(name -> name.equals(tagPropertyName));
            if (isMatch.isPresent() && isMatch.get()) {
                return true;
            }
        }
        return false;
    }
}
