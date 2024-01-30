/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.model.validation.linters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.UnreferencedShapes;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.selector.SelectorSyntaxException;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;

/**
 * Finds shapes that aren't connected to any shapes considered "roots" (defaults to service shapes).
 *
 * <p>This linter is in smithy-model rather than smithy-linters so that it's easier for developers relying on the
 * deprecated UnreferencedShape validator that was always on to migrate to this one instead without needing another
 * dependency.
 */
public final class UnreferencedShapeValidator extends AbstractValidator {

    public static final class Config {

        private Selector rootShapeSelector = Selector.parse("service");

        /**
         * The Selector used to find root shapes, and any shape that is not connected to any of the returned root
         * shapes is considered unreferenced.
         *
         * <p>Defaults to "service" if not set.
         *
         * @return Returns the root selector;
         */
        public Selector getRootShapeSelector() {
            return rootShapeSelector;
        }

        public void setRootShapeSelector(Selector rootShapeSelector) {
            this.rootShapeSelector = Objects.requireNonNull(rootShapeSelector);
        }
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(UnreferencedShapeValidator.class, configuration -> {
                Config config = new Config();
                ObjectNode node = configuration.expectObjectNode()
                        .expectNoAdditionalProperties(Collections.singleton("rootShapeSelector"));
                node.getStringMember("rootShapeSelector").ifPresent(rootShapeNode -> {
                    try {
                        config.setRootShapeSelector(Selector.parse(rootShapeNode.getValue()));
                    } catch (SelectorSyntaxException e) {
                        throw new ExpectationNotMetException("Error parsing `rootShapeSelector`: " + e.getMessage(),
                                                             rootShapeNode);
                    }
                });
                return new UnreferencedShapeValidator(config);
            });
        }
    }

    private final Config config;

    private UnreferencedShapeValidator(Config config) {
        this.config = config;
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (Shape shape : new UnreferencedShapes(config.rootShapeSelector).compute(model)) {
            events.add(note(shape, "This shape is unreferenced. It has no modeled connections to shapes "
                                   + "that match the following selector: `" + config.rootShapeSelector + "`"));
        }

        return events;
    }
}
