/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.protocols;

import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
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
        member.getMemberTrait(context.getModel(), DocumentationTrait.class)
                .map(DocumentationTrait::getValue)
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
