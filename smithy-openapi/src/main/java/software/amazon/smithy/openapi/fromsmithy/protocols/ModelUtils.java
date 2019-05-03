/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.openapi.fromsmithy.protocols;

import java.util.Optional;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.model.ParameterObject;

/**
 * Model utility functions for converting to OpenAPI.
 *
 * <p>These helper methods are currently package-private, but could be made
 * public if needed.
 */
final class ModelUtils {
    private ModelUtils() {}

    /**
     * Gets a trait from a shape or the shape targeted by the shape if the
     * shape is a member.
     *
     * @param context Conversion context.
     * @param shape Shape or member shape to get the trait from.
     * @param trait Trait type to get.
     * @param <T> Trait type to get.
     * @return Returns the optionally found trait on the shape or member.
     */
    static <T extends Trait> Optional<T> getMemberTrait(Context context, Shape shape, Class<T> trait) {
        return shape.asMemberShape()
                .flatMap(member -> member.getMemberTrait(context.getModel().getShapeIndex(), trait));
    }

    /**
     * Gets the mediaType trait value of the shape targeted by a member.
     *
     * @param context Context used to resolve targets.
     * @param member Members to resolve.
     * @return Returns the resolved media type.
     */
    static String getMediaType(Context context, MemberShape member) {
        return getMemberTrait(context, member, MediaTypeTrait.class)
                .map(MediaTypeTrait::getValue)
                .orElseGet(() -> context.getModel().getShapeIndex().getShape(member.getTarget())
                        .map(target -> {
                            if (target.isStringShape()) {
                                return "text/plain";
                            } else if (target.isBlobShape()) {
                                return "application/octet-stream";
                            } else {
                                return "application/octet-stream";
                            }
                        })
                        .orElse("application/octet-stream"));
    }

    /**
     * Creates a request parameter from a member using some default settings.
     *
     * <p>If the member is required, the parameter is marked as required, the
     * name of the parameter is set to the name of the member, and the
     * description of the parameter is set to the documentation of the member
     * if present, otherwise it's set to the shape targeted by the member's
     * documentation if present.
     *
     * @param context Context used to resolve targets.
     * @param member Members to convert.
     * @return Returns the parameter object builder.
     */
    static ParameterObject.Builder createParameterMember(Context context, MemberShape member) {
        ParameterObject.Builder builder = ParameterObject.builder();
        builder.required(member.isRequired());
        builder.name(member.getMemberName());
        getMemberTrait(context, member, DocumentationTrait.class).map(DocumentationTrait::getValue)
                .ifPresent(builder::description);
        return builder;
    }

    /**
     * Converts a given schema to a Schema builder that uses a string.
     *
     * <p>Any properties that are specific to numbers, arrays, etc are removed.
     *
     * @param schema Schema to convert.
     * @return Returns the schema as a string with only properties relevant to strings.
     */
    static Schema.Builder convertSchemaToStringBuilder(Schema schema) {
        return schema.toBuilder()
                .type("string")
                .maximum(null)
                .minimum(null)
                .exclusiveMaximum(null)
                .exclusiveMinimum(null)
                .multipleOf(null)
                .items(null)
                .properties(null)
                .required(null)
                .propertyNames(null)
                .oneOf(null)
                .anyOf(null)
                .not(null)
                .ref(null)
                .minProperties(null)
                .maxProperties(null)
                .minItems(null)
                .maxItems(null);
    }
}
