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

package software.smithy.linters;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.selector.SelectorSyntaxException;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;

/**
 * Emits a validation event for each shape that matches a selector.
 */
public final class EmitEachSelectorValidator extends AbstractValidator {
    private final Selector selector;

    private EmitEachSelectorValidator(Selector selector) {
        this.selector = selector;
    }

    public static ValidatorService provider() {
        return ValidatorService.createProvider(EmitEachSelectorValidator.class, configuration -> {
            Selector selector = parse(configuration.expectMember("selector").expectStringNode());
            return new EmitEachSelectorValidator(selector);
        });
    }

    private static Selector parse(StringNode expression) {
        try {
            return Selector.parse(expression.getValue().trim());
        } catch (SelectorSyntaxException e) {
            throw new SourceException("Invalid selector expression: " + e.getMessage(), expression, e);
        }
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        return selector.select(model.getShapeIndex()).stream()
                .map(shape -> danger(shape, "Selector capture matched selector: " + selector))
                .collect(Collectors.toList());
    }
}
