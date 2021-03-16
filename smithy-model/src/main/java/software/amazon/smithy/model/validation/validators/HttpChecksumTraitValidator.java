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
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.HttpChecksumTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public class HttpChecksumTraitValidator extends AbstractValidator {

    private static final String HEADER = "header";

    /**
     * Validates a model and returns a list of validation events.
     *
     * @param model Model to validate.
     * @return List of validation events.
     */
    @Override
    public List<ValidationEvent> validate(
            Model model
    ) {
        return model.shapes(OperationShape.class)
                .filter(operation -> operation.hasTrait(HttpChecksumTrait.class))
                .flatMap(operation -> validateOperation(model, operation).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateOperation(Model model, OperationShape operation) {
        List<ValidationEvent> events = new ArrayList<>();
        HttpChecksumTrait trait = operation.expectTrait(HttpChecksumTrait.class);

        HttpChecksumTrait.HttpChecksumProperties requestProperty = trait.getRequestProperty();
        HttpChecksumTrait.HttpChecksumProperties responseProperty = trait.getResponseProperty();

        if (requestProperty == null && responseProperty == null) {
            events.add(error(operation, trait, String.format(
                    "`%s` trait must have at least one of request or response properties",
                    HttpChecksumTrait.ID
            )));
        }

        if (responseProperty != null) {
            // Response property only supports header as location.
            if (!responseProperty.getLocation().equalsIgnoreCase(HEADER)) {
                events.add(error(operation, trait,
                        String.format("`%s` trait only supports header as location", HttpChecksumTrait.ID)));
            }

        }


        return events;
    }
}
