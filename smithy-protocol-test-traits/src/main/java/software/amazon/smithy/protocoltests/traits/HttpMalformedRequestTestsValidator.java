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

package software.amazon.smithy.protocoltests.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Ensures that parameters attached to @httpMalformedRequestTest cases are well-formed.
 */
@SmithyInternalApi
public final class HttpMalformedRequestTestsValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        Stream.concat(model.shapes(OperationShape.class), model.shapes(StructureShape.class)).forEach(shape -> {
            shape.getTrait(HttpMalformedRequestTestsTrait.class).ifPresent(trait -> {
                trait.getParameterizedTestCases().forEach(testCase -> {
                    if (!testCase.getTestParameters().isEmpty()) {
                        Set<Integer> sizes = testCase.getTestParameters().values()
                                .stream().map(List::size).collect(Collectors.toSet());
                        if (sizes.size() != 1) {
                            events.add(error(shape, trait.getSourceLocation(), "Each list associated to a key "
                                    + "in `testParameters` must be of the same length."));
                        }
                    }
                });
            });
        });
        return events;
    }
}
