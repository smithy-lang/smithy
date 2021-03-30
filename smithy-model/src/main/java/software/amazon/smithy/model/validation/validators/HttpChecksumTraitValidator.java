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
import software.amazon.smithy.model.traits.HttpChecksumProperties.Location;
import software.amazon.smithy.model.traits.HttpChecksumTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public class HttpChecksumTraitValidator extends AbstractValidator {

    /**
     * Validates a model and returns a list of validation events.
     *
     * @param model Model to validate.
     * @return List of validation events.
     */
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

        if (requestProperty.isPresent()) {
            HttpChecksumProperties property = requestProperty.get();
            if (property.getAlgorithms().isEmpty()) {
                events.add(error(operation, trait,
                        "The `request` property of the `httpChecksum` trait must contain at least one `algorithms` "
                                + "property entry, found none."));
            }
        }

        if (responseProperty.isPresent()) {
            HttpChecksumProperties property = responseProperty.get();
            // Response property only supports header as location.
            if (!property.getLocation().equals(Location.HEADER)) {
                events.add(error(operation, trait,
                       String.format("The `httpChecksum` trait only supports the `location` of \"header\" for the "
                               + "`response`, found \"%s\".", property.getLocation().toString())));
            }

            if (property.getAlgorithms().isEmpty()) {
                events.add(error(operation, trait,
                        "The `response` property of the `httpChecksum` trait must contain at least one `algorithms` "
                                + "property entry, found none."));
            }
        }
        return events;
    }
}
