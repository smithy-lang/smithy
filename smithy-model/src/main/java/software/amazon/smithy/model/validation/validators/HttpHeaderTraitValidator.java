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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Validates that httpHeader traits are case-insensitively unique.
 */
public final class HttpHeaderTraitValidator extends AbstractValidator {

    private static final Set<String> BLACKLIST = SetUtils.of(
            "authorization",
            "connection",
            "content-length",
            "expect",
            "host",
            "max-forwards",
            "proxy-authenticate",
            "server",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "user-agent",
            "www-authenticate",
            "x-forwarded-for");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = model.getShapeIndex().shapes(StructureShape.class)
                .flatMap(shape -> validateStructure(shape).stream())
                .collect(Collectors.toList());

        events.addAll(model.getShapeIndex().shapes(MemberShape.class)
                .flatMap(member -> Trait.flatMapStream(member, HttpHeaderTrait.class))
                .filter(pair -> BLACKLIST.contains(pair.getRight().getValue().toLowerCase(Locale.US)))
                .map(pair -> danger(pair.getLeft(), String.format(
                        "httpHeader cannot be set to `%s`", pair.getRight().getValue()
                )))
                .collect(Collectors.toList()));

        return events;
    }

    private List<ValidationEvent> validateStructure(StructureShape structure) {
        return structure.getAllMembers().values().stream()
                .flatMap(member -> Trait.flatMapStream(member, HttpHeaderTrait.class))
                .collect(groupingBy(pair -> pair.getRight().getValue().toLowerCase(Locale.US),
                                    mapping(pair -> pair.getLeft().getMemberName(), toList())))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> error(structure, String.format(
                        "`httpHeader` field name binding conflicts found for the `%s` header in the "
                        + "following structure members: %s",
                        entry.getKey(), ValidationUtils.tickedList(entry.getValue()))))
                .collect(toList());
    }
}
