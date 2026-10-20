/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.DiscriminatedTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates unions marked with {@code @discriminated}.
 *
 * <p>The members of a discriminated union are serialized as a flat object that contains the discriminator field
 * alongside the fields of the member's target. This is only well defined when each member targets a structure, and
 * requires that the discriminator field can't be confused with a field of any targeted structure.
 */
public final class DiscriminatedTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (UnionShape union : model.getUnionShapes()) {
            DiscriminatedTrait trait = union.getTrait(DiscriminatedTrait.class).orElse(null);
            if (trait != null) {
                for (MemberShape member : union.getAllMembers().values()) {
                    validateMember(model, trait, member, events);
                }
            }
        }
        return events;
    }

    private void validateMember(
            Model model,
            DiscriminatedTrait trait,
            MemberShape member,
            List<ValidationEvent> events
    ) {
        Shape target = model.expectShape(member.getTarget());

        if (member.hasTrait(JsonNameTrait.class)) {
            events.add(warning(member,
                    member.expectTrait(JsonNameTrait.class),
                    "The `@jsonName` trait has no effect on a member of a union marked with `@discriminated`. The "
                            + "discriminator value is always the name of the member.",
                    "UnionMemberJsonName"));
        }

        if (target.hasTrait(UnitTypeTrait.class) || !target.isStructureShape()) {
            events.add(error(member,
                    String.format(
                            "Members of a union marked with `@discriminated` must target a structure, but "
                                    + "this member targets %s `%s`. The discriminator is serialized alongside "
                                    + "the fields of the member's target, so the target must be a structure with "
                                    + "named fields. Consider wrapping the target in a structure.",
                            describe(target),
                            target.getId()),
                    "MemberTarget"));
            return;
        }

        String discriminator = trait.getValue();
        for (MemberShape targetMember : target.getAllMembers().values()) {
            // A member is named on the wire by its member name, or by its jsonName in protocols that use it. Which
            // protocol applies isn't known here, so check both. A collision under every protocol is an error. A
            // collision under only some protocols is a danger, which can be suppressed by a service that only uses
            // protocols where it does not apply.
            String memberName = targetMember.getMemberName();
            String jsonName = targetMember.getTrait(JsonNameTrait.class)
                    .map(JsonNameTrait::getValue)
                    .orElse(memberName);
            boolean collidesWithoutJsonName = memberName.equals(discriminator);
            boolean collidesWithJsonName = jsonName.equals(discriminator);

            if (collidesWithoutJsonName && collidesWithJsonName) {
                events.add(error(member,
                        collisionMessage(member, discriminator, target, "the member `" + memberName + "`", ""),
                        "DiscriminatorCollision"));
            } else if (collidesWithoutJsonName) {
                events.add(danger(member,
                        collisionMessage(member,
                                discriminator,
                                target,
                                "the member `" + memberName + "`",
                                " in protocols that do not use the `@jsonName` of the member (`" + jsonName + "`)"),
                        "DiscriminatorCollision"));
            } else if (collidesWithJsonName) {
                events.add(danger(member,
                        collisionMessage(member,
                                discriminator,
                                target,
                                "the `@jsonName` of the member `" + memberName + "`",
                                " in protocols that use `@jsonName`"),
                        "DiscriminatorCollision"));
            }
        }
    }

    private static String collisionMessage(
            MemberShape member,
            String discriminator,
            Shape target,
            String collidesWith,
            String when
    ) {
        return String.format(
                "The discriminator field `%s` of the `@discriminated` union `%s` collides with %s of the "
                        + "structure `%s` targeted by this member%s. The discriminator field cannot have the same "
                        + "name as the serialized name of a member of any structure targeted by the union.",
                discriminator,
                member.getContainer(),
                collidesWith,
                target.getId(),
                when);
    }

    private static String describe(Shape target) {
        return target.hasTrait(UnitTypeTrait.class) ? "the unit type" : "the " + target.getType() + " shape";
    }
}
