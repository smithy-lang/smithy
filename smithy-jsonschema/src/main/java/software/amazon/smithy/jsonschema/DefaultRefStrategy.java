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

package software.amazon.smithy.jsonschema;

import java.util.regex.Pattern;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.SimpleShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.utils.StringUtils;

/**
 * This ref strategy converts Smithy shapes into the following:
 *
 * <ul>
 *     <li>
 *         Structures, unions, maps, and enums are always created as a top-level
 *         JSON schema definition.
 *     </li>
 *     <li>
 *         <p>Members that target structures, unions, enums, and maps use a $ref to the
 *         targeted shape. With the exception of maps, these kinds of shapes are almost
 *         always generated as concrete types by code generators, so it's useful to reuse
 *         them throughout the schema. However, this means that member documentation
 *         and other member traits need to be moved in some way to the containing
 *         shape (for example, documentation needs to be appended to the container
 *         shape).</p>
 *         <p>Maps are included here because they are represented as objects in
 *         JSON schema, and many tools will generate a type or require an explicit
 *         name for all objects. For example, API Gateway will auto-generate a
 *         non-deterministic name for a map if one is not provided.</p>
 *     </li>
 *     <li>
 *         Members that target a collection or simple type are inlined into the generated
 *         container (that is, shapes that do not have the enum trait).
 *     </li>
 * </ul>
 */
final class DefaultRefStrategy implements RefStrategy {

    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\.");
    private static final Pattern NON_ALPHA_NUMERIC = Pattern.compile("[^A-Za-z0-9]");

    private final Model model;
    private final boolean alphanumericOnly;
    private final boolean keepNamespaces;
    private final String rootPointer;
    private final PropertyNamingStrategy propertyNamingStrategy;
    private final ObjectNode config;

    DefaultRefStrategy(Model model, ObjectNode config, PropertyNamingStrategy propertyNamingStrategy) {
        this.model = model;
        this.propertyNamingStrategy = propertyNamingStrategy;
        this.config = config;
        rootPointer = computePointer(config);
        alphanumericOnly = config.getBooleanMemberOrDefault(JsonSchemaConstants.ALPHANUMERIC_ONLY_REFS);
        keepNamespaces = config.getBooleanMemberOrDefault(JsonSchemaConstants.KEEP_NAMESPACES);
    }

    private static String computePointer(ObjectNode config) {
        String pointer = config.getStringMemberOrDefault(JsonSchemaConstants.DEFINITION_POINTER, DEFAULT_POINTER);
        if (!pointer.endsWith("/")) {
            pointer += "/";
        }
        return pointer;
    }

    @Override
    public String toPointer(ShapeId id) {
        if (id.getMember().isPresent()) {
            MemberShape member = model.expectShape(id, MemberShape.class);
            return createMemberPointer(member);
        }

        StringBuilder builder = new StringBuilder();
        appendNamespace(builder, id);
        builder.append(id.getName());
        return rootPointer + stripNonAlphaNumericCharsIfNecessary(builder.toString());
    }

    private String createMemberPointer(MemberShape member) {
        if (!isInlined(member)) {
            return toPointer(member.getTarget());
        }

        Shape container = model.expectShape(member.getContainer());
        String parentPointer = toPointer(container.getId());

        switch (container.getType()) {
            case LIST:
            case SET:
                return parentPointer + "/items";
            case MAP:
                return member.getMemberName().equals("key")
                       ? parentPointer + "/propertyNames"
                       : parentPointer + "/additionalProperties";
            default: // union | structure
                return parentPointer + "/properties/" + propertyNamingStrategy.toPropertyName(
                        container, member, config);
        }
    }

    @Override
    public boolean isInlined(Shape shape) {
        // We could add more logic here in the future if needed to account for
        // member shapes that absolutely must generate a synthesized schema.
        if (shape.isMemberShape()) {
            MemberShape member = shape.expectMemberShape();
            Shape target = model.expectShape(member.getTarget());
            return isInlined(target);
        }

        // Collections (lists and sets) are always inlined. Most importantly,
        // this is done to expose any important traits of list and set members
        // in the generated JSON schema document (for example, documentation).
        // Without this inlining, list and set member documentation would be
        // lost since the items property in the generated JSON schema would
        // just be a $ref pointing to the target of the member. The more
        // things that can be inlined that don't matter the better since it
        // means traits like documentation aren't lost.
        //
        // Members of lists and sets are basically never a generated type in
        // any programming language because most just use some kind of
        // standard library feature. This essentially means that the names
        // of lists or sets changing when round-tripping
        // Smithy -> JSON Schema -> Smithy doesn't matter that much.
        if (shape instanceof CollectionShape) {
            return true;
        }

        // Strings with the enum trait are never inlined. This helps to ensure
        // that the name of an enum string can be round-tripped from
        // Smithy -> JSON Schema -> Smithy, helps OpenAPI code generators to
        // use a good name for any generated types, and it cuts down on the
        // duplication of documentation and constraints in the generated schema.
        if (shape.hasTrait(EnumTrait.class)) {
            return false;
        }

        // Simple types are always inlined unless the type has the enum trait.
        return shape instanceof SimpleShape;
    }

    private void appendNamespace(StringBuilder builder, ShapeId id) {
        // Append each namespace part, capitalizing each segment.
        // For example, "smithy.example" becomes "SmithyExample".
        if (keepNamespaces) {
            for (String part : SPLIT_PATTERN.split(id.getNamespace())) {
                builder.append(StringUtils.capitalize(part));
            }
        }
    }

    private String stripNonAlphaNumericCharsIfNecessary(String result) {
        return alphanumericOnly
               ? NON_ALPHA_NUMERIC.matcher(result).replaceAll("")
               : result;
    }
}
