/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

public final class HttpUriFormatValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        CharsetEncoder encoder = StandardCharsets.US_ASCII.newEncoder();

        for (Shape shape : model.getShapesWithTrait(HttpTrait.class)) {
            HttpTrait trait = shape.expectTrait(HttpTrait.class);
            String uri = trait.getUri().toString();
            if (!encoder.canEncode(uri)) {
                events.add(error(shape, trait, "@http trait `uri` is invalid: " + uri));
            }
        }
        return events;
    }
}
