/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class UnionValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (UnionShape union : model.getUnionShapes()) {
            if (union.members().isEmpty()) {
                events.add(error(union, "Tagged unions must have one or more members"));
            } else {
                for (MemberShape member : union.getAllMembers().values()) {
                    Shape target = model.expectShape(member.getTarget());
                    validateUnionMemberTarget(member, target, events);
                    validateUnionMember(member, events);
                }
            }
        }
        return events;
    }

    private void validateUnionMemberTarget(MemberShape member, Shape target, List<ValidationEvent> events) {
        if (target.hasTrait(DefaultTrait.class)) {
            events.add(note(member,
                    String.format(
                            "This union member targets `%s`, a shape with a default value of `%s`. Note that "
                                    + "default values are only applicable to structures and ignored in tagged unions. It "
                                    + "is a best practice for union members to target shapes with no default value (for example, "
                                    + "instead of targeting PrimitiveInteger, target Integer).",
                            target.getId(),
                            Node.printJson(target.expectTrait(DefaultTrait.class).toNode()))));
        }
    }

    private void validateUnionMember(MemberShape member, List<ValidationEvent> events) {
        if (member.hasTrait(BoxTrait.class)) {
            events.add(warning(member,
                    member.expectTrait(BoxTrait.class),
                    "Invalid box trait found on a union member. The box trait on union members "
                            + "has no effect because union members are implicitly nullable."));
        }
    }
}
