/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.linters;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.selector.Selector;
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

    /**
     * EmitNoneSelector configuration settings.
     */
    public static final class Config {
        private Selector selector;

        /**
         * Gets the selector that if no shapes match, a validation event
         * is emitted.
         *
         * @return Returns the selector.
         */
        public Selector getSelector() {
            return selector;
        }

        public void setSelector(Selector selector) {
            this.selector = selector;
        }
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(EmitNoneSelectorValidator.class, configuration -> {
                NodeMapper mapper = new NodeMapper();
                Config config = mapper.deserialize(configuration, Config.class);
                return new EmitNoneSelectorValidator(config);
            });
        }
    }

    private final Config config;

    private EmitNoneSelectorValidator(Config config) {
        this.config = config;
        Objects.requireNonNull(config.selector, "selector is required");
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        // Filter out prelude types.
        Set<Shape> shapes = config.getSelector()
                .select(model)
                .stream()
                .filter(shape -> !Prelude.isPreludeShape(shape.getId()))
                .collect(Collectors.toSet());

        if (shapes.isEmpty()) {
            return ListUtils.of(ValidationEvent.builder()
                    .id(getName())
                    .severity(Severity.DANGER)
                    .message("Expected at least one shape to match selector: " + config.getSelector())
                    .build());
        }

        return ListUtils.of();
    }
}
