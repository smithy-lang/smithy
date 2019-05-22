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

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.FunctionalUtils;

/**
 * Emits warnings when unstable features are used in a model, for example
 * document shapes.
 *
 * <p>This validator should be expanded in the future as necessary when
 * unstable features are added. We could potentially also add things like
 * an "unstable" trait to indicate that a shape is unstable.
 */
public final class UnstableFeatureValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.getShapeIndex().shapes(DocumentShape.class)
                .filter(FunctionalUtils.not(Prelude::isPreludeShape))
                .map(shape -> warning(shape, "The document shape type is currently unstable and subject to "
                                             + "change. It is not generally supported across tooling and should "
                                             + "only be used to test the functionality of the feature."))
                .collect(Collectors.toList());
    }
}
