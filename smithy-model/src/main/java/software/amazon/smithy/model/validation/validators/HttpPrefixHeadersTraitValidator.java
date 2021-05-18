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

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that httpHeader traits do not case-insensitively start with an
 * httpPrefixHeader on the same structure.
 */
public final class HttpPrefixHeadersTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (MemberShape member : model.getMemberShapesWithTrait(HttpPrefixHeadersTrait.class)) {
            model.getShape(member.getContainer()).flatMap(Shape::asStructureShape).ifPresent(structure -> {
                events.addAll(validateMember(structure, member, member.expectTrait(HttpPrefixHeadersTrait.class)));
            });
        }

        return events;
    }

    private List<ValidationEvent> validateMember(
            StructureShape structure,
            MemberShape member,
            HttpPrefixHeadersTrait prefixTrait
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        String prefix = prefixTrait.getValue().toLowerCase(Locale.ENGLISH);

        // Find all structure members that case-insensitively start with the same prefix.
        for (MemberShape otherMember : structure.getAllMembers().values()) {
            otherMember.getTrait(HttpHeaderTrait.class).ifPresent(httpHeaderTrait -> {
                String lowerCaseHeader = httpHeaderTrait.getValue().toLowerCase(Locale.ENGLISH);
                if (lowerCaseHeader.startsWith(prefix)) {
                    events.add(error(otherMember, httpHeaderTrait, String.format(
                            "`httpHeader` binding of `%s` conflicts with the `httpPrefixHeaders` binding of `%s` "
                            + "to `%s`. `httpHeader` bindings must not case-insensitively start with any "
                            + "`httpPrefixHeaders` bindings.", lowerCaseHeader, member.getId(), prefix)));
                }
            });
        }

        return events;
    }
}
