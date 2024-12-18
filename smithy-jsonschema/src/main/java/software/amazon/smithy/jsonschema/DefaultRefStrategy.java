/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema;

import java.util.Optional;
import java.util.regex.Pattern;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.SimpleShape;
import software.amazon.smithy.model.traits.EnumTrait;

/**
 * This ref strategy converts Smithy shapes into the following:
 *
 * <ul>
 *     <li>
 *         Structures, unions, maps, and enums are always created as a top-level
 *         JSON schema definition.
 *     </li>
 *     <li>
 *         <p>Members that target structures, unions, enums, intEnums, and maps use a $ref to
 *         the targeted shape. With the exception of maps, these kinds of shapes are almost
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

    private static final Pattern NON_ALPHA_NUMERIC = Pattern.compile("[^A-Za-z0-9]");

    private final Model model;
    private final String rootPointer;
    private final PropertyNamingStrategy propertyNamingStrategy;
    private final JsonSchemaConfig config;
    private final ServiceShape serviceContext;

    DefaultRefStrategy(Model model, JsonSchemaConfig config, PropertyNamingStrategy propertyNamingStrategy) {
        this.model = model;
        this.propertyNamingStrategy = propertyNamingStrategy;
        this.config = config;
        rootPointer = computePointer(config);
        serviceContext = Optional.ofNullable(config.getService())
                .map(shape -> model.expectShape(shape, ServiceShape.class))
                .orElse(null);
    }

    private static String computePointer(JsonSchemaConfig config) {
        String pointer = config.getDefinitionPointer();
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

        // Use the "rename" property when providing refs for shapes within
        // the context of a service.
        String shapeName = id.getName();
        if (serviceContext != null) {
            shapeName = serviceContext.getContextualName(id);
        }

        return rootPointer + stripNonAlphaNumericCharsIfNecessary(shapeName);
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
                        container,
                        member,
                        config);
        }
    }

    @Override
    public boolean isInlined(Shape shape) {
        // We could add more logic here in the future if needed to account for
        // member shapes that absolutely must generate a synthesized schema.
        if (shape.asMemberShape().isPresent()) {
            MemberShape member = shape.asMemberShape().get();
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

        // Maps are not inlined by default, but can be if configured.
        // Maps are usually not a generated type in programming languages,
        // but JSON schema represents them as "object" types which code
        // generators may not distinguish from converted structures.
        //
        // Some code generators, however, will treat inline "object" types
        // as maps and referenced ones as structures.
        if (shape.isMapShape() && config.getUseInlineMaps()) {
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

        if (shape.isIntEnumShape() && !config.getDisableIntEnums()) {
            return false;
        }

        // Simple types are always inlined unless the type has the enum trait.
        return shape instanceof SimpleShape;
    }

    private String stripNonAlphaNumericCharsIfNecessary(String result) {
        return config.getAlphanumericOnlyRefs()
                ? NON_ALPHA_NUMERIC.matcher(result).replaceAll("")
                : result;
    }
}
