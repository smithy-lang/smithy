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
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.HttpChecksumProperties;
import software.amazon.smithy.model.traits.HttpChecksumTrait;
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

    @Override
    public List<ValidationEvent> validate(Model model) {
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
        });

        return events;
    }
}
