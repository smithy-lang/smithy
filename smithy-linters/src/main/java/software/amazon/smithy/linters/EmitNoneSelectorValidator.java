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

package software.amazon.smithy.linters;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.selector.SelectorSyntaxException;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;
import software.amazon.smithy.utils.ListUtils;

/**
 * Emits a validation event if no shapes match the given selector.
 */
public final class EmitNoneSelectorValidator extends AbstractValidator {
    private final Selector selector;

    private EmitNoneSelectorValidator(Selector selector) {
        this.selector = selector;
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(EmitNoneSelectorValidator.class, configuration -> {
                Selector selector = parse(configuration.expectStringMember("selector"));
                return new EmitNoneSelectorValidator(selector);
            });
        }
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
        // Filter out prelude types.
        Set<Shape> shapes = selector.select(model.getShapeIndex()).stream()
                .filter(shape -> !Prelude.isPreludeShape(shape.getId()))
                .collect(Collectors.toSet());

        if (shapes.isEmpty()) {
            return ListUtils.of(ValidationEvent.builder()
                    .eventId(getName())
                    .severity(Severity.DANGER)
                    .message("Expected at least one shape to match selector: " + selector)
                    .build());
        }

        return ListUtils.of();
    }
}
