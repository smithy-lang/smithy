/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Validates that httpHeader traits are case-insensitively unique.
 */
public final class HttpHeaderTraitValidator extends AbstractValidator {

    /** Gather the allowed characters for HTTP headers (tchar from RFC 9110 section 5.6.2). **/
    private static final Set<Character> TCHAR = SetUtils.of(
            // "!" / "#" / "$" / "%" / "&" / "'" / "*" / "+" / "-" / "." / "^" / "_" / "`" / "|" / "~"
            '!',
            '#',
            '$',
            '%',
            '&',
            '\'',
            '*',
            '+',
            '-',
            '.',
            '^',
            '_',
            '`',
            '|',
            '~',
            // DIGIT (0-9)
            '0',
            '1',
            '2',
            '3',
            '4',
            '5',
            '6',
            '7',
            '8',
            '9',
            // ALPHA (A-Z)
            'A',
            'B',
            'C',
            'D',
            'E',
            'F',
            'G',
            'H',
            'I',
            'J',
            'K',
            'L',
            'M',
            'N',
            'O',
            'P',
            'Q',
            'R',
            'S',
            'T',
            'U',
            'V',
            'W',
            'X',
            'Y',
            'Z',
            // ALPHA (a-z)
            'a',
            'b',
            'c',
            'd',
            'e',
            'f',
            'g',
            'h',
            'i',
            'j',
            'k',
            'l',
            'm',
            'n',
            'o',
            'p',
            'q',
            'r',
            's',
            't',
            'u',
            'v',
            'w',
            'x',
            'y',
            'z');

    private static final Set<String> BLOCKLIST = SetUtils.of(
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
        if (!model.isTraitApplied(HttpHeaderTrait.class)) {
            return Collections.emptyList();
        }

        List<ValidationEvent> events = new ArrayList<>();

        for (StructureShape structure : model.getStructureShapes()) {
            events.addAll(validateStructure(structure));
        }

        for (MemberShape member : model.getMemberShapesWithTrait(HttpHeaderTrait.class)) {
            HttpHeaderTrait httpHeaderTrait = member.expectTrait(HttpHeaderTrait.class);
            validateHeader(member, httpHeaderTrait).ifPresent(events::add);
        }

        return events;
    }

    private Optional<ValidationEvent> validateHeader(MemberShape member, HttpHeaderTrait trait) {
        String header = trait.getValue();

        if (BLOCKLIST.contains(header.toLowerCase(Locale.ENGLISH))) {
            return Optional.of(danger(member,
                    trait,
                    String.format(
                            "`%s` is not an allowed HTTP header binding",
                            header)));
        }

        for (int i = 0; i < header.length(); i++) {
            if (!TCHAR.contains(header.charAt(i))) {
                return Optional.of(danger(member,
                        trait,
                        String.format(
                                "`%s` is not a valid HTTP header field name according to section 5.6.2 of RFC 9110",
                                header)));
            }
        }

        return Optional.empty();
    }

    private List<ValidationEvent> validateStructure(StructureShape structure) {
        return structure.getAllMembers()
                .values()
                .stream()
                .filter(member -> member.hasTrait(HttpHeaderTrait.class))
                .collect(groupingBy(shape -> shape.expectTrait(HttpHeaderTrait.class).getValue().toLowerCase(Locale.US),
                        mapping(MemberShape::getMemberName, toList())))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> error(structure,
                        String.format(
                                "`httpHeader` field name binding conflicts found for the `%s` header in the "
                                        + "following structure members: %s",
                                entry.getKey(),
                                ValidationUtils.tickedList(entry.getValue()))))
                .collect(toList());
    }
}
