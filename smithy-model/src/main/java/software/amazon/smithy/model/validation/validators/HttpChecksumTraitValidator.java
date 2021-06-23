/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.HttpChecksumProperty;
import software.amazon.smithy.model.traits.HttpChecksumTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates the HttpChecksum trait is modeled with at least one of the
 * request or response property. Validates for possible conflict with
 * httpHeader and httpPrefixHeaders trait usage within the same
 * input or output structure.
 */
@SmithyInternalApi
public class HttpChecksumTraitValidator extends AbstractValidator {

    private Model model;

    @Override
    public List<ValidationEvent> validate(Model model) {
        this.model = model;

        List<ValidationEvent> events = new ArrayList<>();
        model.shapes(OperationShape.class)
                .filter(operation -> operation.hasTrait(HttpChecksumTrait.class))
                .forEach(operation -> events.addAll(validateOperation(operation)));

        return events;
    }

    private List<ValidationEvent> validateOperation(OperationShape operation) {
        List<ValidationEvent> events = new ArrayList<>();

        HttpChecksumTrait trait = operation.expectTrait(HttpChecksumTrait.class);
        List<HttpChecksumProperty> requestProperties = trait.getRequestProperties();
        List<HttpChecksumProperty> responseProperties = trait.getResponseProperties();

        if (requestProperties.isEmpty() && responseProperties.isEmpty()) {
            events.add(error(operation, trait,
                    "The `httpChecksum` trait must have at least one of the `request` or `response` properties set."));
        }

        for (HttpChecksumProperty property : requestProperties) {
            if (operation.getInput().isPresent()) {
                StructureShape shape = model.expectShape(operation.getInput().get(), StructureShape.class);
                events.addAll(validateName(operation, shape, property.getName(), "request"));
            }
        }

        for (HttpChecksumProperty property : responseProperties) {
            if (operation.getOutput().isPresent()) {
                StructureShape shape = model.expectShape(operation.getInput().get(), StructureShape.class);
                events.addAll(validateName(operation, shape, property.getName(), "response"));
            }

            if (!operation.getErrors().isEmpty()) {
                for (ShapeId id : operation.getErrors()) {
                    StructureShape shape = model.expectShape(id, StructureShape.class);
                    events.addAll(validateName(operation, shape, property.getName(), "response"));
                }
            }
        }

        return events;
    }

    /**
     * Validates the name used with the httpChecksum trait does not conflict with header names modeled using the
     * `httpPrefixHeaders` trait or `httpHeader` trait.
     *
     * @param operation      Operation shape on which httpChecksum trait is modeled.
     * @param containerShape Structure shape referenced by the operation shape, this can be either input, output,
     *                       or error shape.
     * @param name           Name string modeled on the httpChecksum trait.
     * @param propertyType   Property type being validated for httpChecksum trait, this can be either `request`
     *                       or `response` property.
     * @return Returns list of triggered validation events.
     */
    private List<ValidationEvent> validateName(
            OperationShape operation, StructureShape containerShape,
            String name, String propertyType
    ) {
        List<ValidationEvent> events = new ArrayList<>();

        for (MemberShape member : containerShape.members()) {
            // Trigger a DANGER event if the name used with the HttpChecksum trait starts with the prefix string used
            // with HttpPrefixHeaders trait. This is a DANGER event as member modeled with HttpPrefixHeaders
            // may unintentionally conflict with the checksum header resolved using HttpChecksum trait.
            member.getTrait(HttpPrefixHeadersTrait.class).ifPresent(httpPrefixHeadersTrait -> {
                String headerPrefix = httpPrefixHeadersTrait.getValue();
                if (name.startsWith(headerPrefix)) {
                    events.add(danger(operation, String.format("The `%s` property of the `httpChecksum` trait models"
                            + " name %s that starts with the prefix modeled with the `httpPrefixHeaders` trait on"
                            + " member %s.", propertyType, name, member.getId())));
                }
            });

            // Service may model members that are bound to the http header for checksum on input or output shape.
            // This enables customers to provide checksum as input, or access returned checksum value in output.
            // We trigger a WARNING event to prevent any unintentional behavior.
            member.getTrait(HttpHeaderTrait.class).ifPresent(headerTrait -> {
                String headerName = headerTrait.getValue();
                if (name.equalsIgnoreCase(headerName)) {
                    events.add(warning(operation, String.format("The `httpHeader` binding of `%s` on `%s` matches"
                                    + " the name `%s` modeled on the `%s` property of `httpChecksum` trait.",
                            headerName, member.getId(), name, propertyType)));
                }
            });
        }
        return events;
    }
}
