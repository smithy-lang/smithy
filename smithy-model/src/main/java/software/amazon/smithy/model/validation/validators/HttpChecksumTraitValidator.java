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
import java.util.Locale;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpChecksumProperties;
import software.amazon.smithy.model.traits.HttpChecksumTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * HttpChecksumTraitValidator validates HttpChecksum trait is modeled with at
 * least one of the request or response property. This also validates the
 * resolved locations and algorithms list are not empty.
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
        Optional<HttpChecksumProperties> requestProperty = trait.getRequestProperty();
        Optional<HttpChecksumProperties> responseProperty = trait.getResponseProperty();

        if (!requestProperty.isPresent() && !responseProperty.isPresent()) {
            events.add(error(operation, trait,
                    "The `httpChecksum` trait must have at least one of the `request` or `response` properties set."));
        }

        requestProperty.ifPresent(property -> {
            if (property.getLocations().isEmpty()) {
                events.add(error(operation, trait,
                        "The `request` property of the `httpChecksum` trait must contain at least one `location` "
                                + "property entry, found none."));
            }
            if (property.getAlgorithms().isEmpty()) {
                events.add(error(operation, trait,
                        "The `request` property of the `httpChecksum` trait must contain at least one `algorithms` "
                                + "property entry, found none."));
            }

            if (operation.getInput().isPresent()) {
                Shape shape = model.expectShape(operation.getInput().get());
                events.addAll(validatePrefix(operation, shape, property.getPrefix(), "request"));
            }
        });

        responseProperty.ifPresent(property -> {
            if (property.getLocations().isEmpty()) {
                events.add(error(operation, trait,
                        "The `response` property of the `httpChecksum` trait must contain at least one `location` "
                                + "property entry, found none."));
            }
            if (property.getAlgorithms().isEmpty()) {
                events.add(error(operation, trait,
                        "The `response` property of the `httpChecksum` trait must contain at least one `algorithms` "
                                + "property entry, found none."));
            }

            if (operation.getOutput().isPresent()) {
               Shape shape = model.expectShape(operation.getOutput().get());
               events.addAll(validatePrefix(operation, shape, property.getPrefix(), "response"));
            }

            if (!operation.getErrors().isEmpty()) {
                for (ShapeId id: operation.getErrors()) {
                   Shape shape = model.expectShape(id);
                   events.addAll(validatePrefix(operation, shape, property.getPrefix(), "response"));
                }
            }
        });

        return events;
    }


    /**
     * Validates if prefix used with the httpChecksum trait conflicts with headers modeled using the
     * `httpPrefixHeaders` trait or `httpHeader` trait.
     *
     * @param operation is the operation shape on which httpChecksum trait is modeled
     * @param containerShape is the shape referenced by the operation shape, this can be either input shape
     *                       or output shape
     * @param prefix is the prefix modeled on the httpChecksum trait
     * @param propertyType is the property type being validated for httpChecksum trait, this can be either `request`
     *                    or `response` property
     * @return list of triggered validation events
     */
    private List<ValidationEvent> validatePrefix(
            OperationShape operation, Shape containerShape,
            String prefix, String propertyType
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        Walker walker = new Walker(model);

        for (Shape shape : walker.walkShapes(containerShape)) {
            // since httpPrefixHeaders or httpHeader trait can be only be
            // bound to a member within a structure shape.
            if (!shape.isStructureShape()) {
                continue;
            }

            for (MemberShape member : shape.members()) {
                // validate any conflict with HttpPrefixHeadersTrait
                Optional<HttpPrefixHeadersTrait> prefixHeadersTrait = member.getTrait(HttpPrefixHeadersTrait.class);
                if (prefixHeadersTrait.isPresent()) {
                    HttpPrefixHeadersTrait prefixTrait = prefixHeadersTrait.get();
                    String headerPrefix = prefixTrait.getValue();
                    if (prefix.equalsIgnoreCase(headerPrefix)) {
                        events.add(danger(operation, String.format("The `%s` property of the `httpChecksum` "
                                + "trait uses the same prefix modeled with `httpPrefixHeaders` trait on "
                                + "member %s.", propertyType, member.getId())));
                    }
                }

                // validate conflict with HttpHeaderTrait
                Optional<HttpHeaderTrait> httpHeaderTrait = member.getTrait(HttpHeaderTrait.class);
                if (httpHeaderTrait.isPresent()) {
                    HttpHeaderTrait headerTrait = httpHeaderTrait.get();
                    String lowerCaseHeader = headerTrait.getValue().toLowerCase(Locale.ENGLISH);
                    String lowerCasePrefix = prefix.toLowerCase(Locale.ENGLISH);
                    if (lowerCaseHeader.startsWith(lowerCasePrefix)) {
                        events.add(warning(operation, String.format("The `%s` property of the `httpChecksum` "
                                + "trait uses a prefix that matches http headers modeled with `httpHeader` trait "
                                + " on member %s.", propertyType, member.getId())));
                    }
                }
            }
        }
        return events;
    }

}
