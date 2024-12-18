/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.linters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelSyntaxException;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.selector.AttributeValue;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.SimpleParser;

/**
 * Emits a validation event for each shape that matches a selector.
 */
public final class EmitEachSelectorValidator extends AbstractValidator {

    /**
     * EmitEachSelector configuration settings.
     */
    public static final class Config {

        private Selector selector;
        private ShapeId bindToTrait;
        private MessageTemplate messageTemplate;

        /**
         * Gets the required selector that matches shapes.
         *
         * <p>Each shape that matches the given selector will emit a
         * validation event.
         *
         * @return Selector to match on.
         */
        public Selector getSelector() {
            return selector;
        }

        public void setSelector(Selector selector) {
            this.selector = selector;
        }

        /**
         * Gets the optional trait that each emitted event is bound to.
         *
         * <p>An event is only emitted for shapes that have this trait.
         *
         * @return Returns the trait to bind each event to.
         */
        public ShapeId getBindToTrait() {
            return bindToTrait;
        }

        public void setBindToTrait(ShapeId bindToTrait) {
            this.bindToTrait = bindToTrait;
        }

        /**
         * Gets the optional message template that can reference selector variables.
         *
         * @return Returns the message template.
         */
        public String getMessageTemplate() {
            return messageTemplate == null ? null : messageTemplate.toString();
        }

        /**
         * Sets the optional message template for each emitted event.
         *
         * @param messageTemplate Message template to set.
         * @throws ModelSyntaxException if the message template is invalid.
         */
        public void setMessageTemplate(String messageTemplate) {
            this.messageTemplate = new MessageTemplateParser(messageTemplate).parse();
        }
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(EmitEachSelectorValidator.class, configuration -> {
                NodeMapper mapper = new NodeMapper();
                Config config = mapper.deserialize(configuration, Config.class);
                return new EmitEachSelectorValidator(config);
            });
        }
    }

    private final Config config;

    /**
     * @param config Validator configuration.
     */
    public EmitEachSelectorValidator(Config config) {
        this.config = config;
        Objects.requireNonNull(config.selector, "selector is required");
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        // Short-circuit the validation if the binding trait is never used.
        if (config.bindToTrait != null && !model.getAppliedTraits().contains(config.getBindToTrait())) {
            return Collections.emptyList();
        } else if (config.messageTemplate == null) {
            return validateWithSimpleMessages(model);
        } else {
            return validateWithTemplate(model);
        }
    }

    private List<ValidationEvent> validateWithSimpleMessages(Model model) {
        return config.getSelector()
                .select(model)
                .stream()
                .flatMap(shape -> OptionalUtils.stream(createSimpleEvent(shape)))
                .collect(Collectors.toList());
    }

    private Optional<ValidationEvent> createSimpleEvent(Shape shape) {
        FromSourceLocation location = determineEventLocation(shape);
        // Only create a validation event if the bound trait (if any) is present on the shape.
        if (location == null) {
            return Optional.empty();
        }

        return Optional.of(danger(shape, location, "Selector capture matched selector: " + config.getSelector()));
    }

    // Determine where to bind the event. Only emit an event when `bindToTrait` is
    // set if the shape actually has the trait.
    private FromSourceLocation determineEventLocation(Shape shape) {
        return config.bindToTrait == null
                ? shape.getSourceLocation()
                : shape.findTrait(config.bindToTrait).orElse(null);
    }

    // Created events with a message template requires emitting matches
    // into a BiConsumer and building up a mutated List of events.
    private List<ValidationEvent> validateWithTemplate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        config.getSelector().consumeMatches(model, match -> {
            createTemplatedEvent(match).ifPresent(events::add);
        });
        return events;
    }

    private Optional<ValidationEvent> createTemplatedEvent(Selector.ShapeMatch match) {
        FromSourceLocation location = determineEventLocation(match.getShape());
        // Only create a validation event if the bound trait (if any) is present on the shape.
        if (location == null) {
            return Optional.empty();
        }

        // Create an AttributeValue from the matched shape and context vars.
        // This is then used to expand message template scoped attributes.
        AttributeValue value = AttributeValue.shape(match.getShape(), match);
        return Optional.of(danger(match.getShape(), location, config.messageTemplate.expand(value)));
    }

    /**
     * A message template is made up of "parts", where each part is a function that accepts
     * an {@link AttributeValue} and returns a String.
     */
    private static final class MessageTemplate {
        private final CharSequence template;
        private final List<Function<AttributeValue, CharSequence>> parts;

        private MessageTemplate(CharSequence template, List<Function<AttributeValue, CharSequence>> parts) {
            this.template = template;
            this.parts = parts;
        }

        /**
         * Expands the MessageTemplate using the provided AttributeValue.
         *
         * <p>Each selector result shape along with the variables that were captured
         * when the shape was matched are used to create an AttributeValue which
         * is then passed to this message to create a validation event message.
         *
         * @param value The attribute value to pass to each part.
         * @return Returns the expanded message template.
         */
        private String expand(AttributeValue value) {
            StringBuilder builder = new StringBuilder();
            for (Function<AttributeValue, CharSequence> part : parts) {
                builder.append(part.apply(value));
            }
            return builder.toString();
        }

        @Override
        public String toString() {
            return template.toString();
        }
    }

    /**
     * Parses message templates by slicing out literals and scoped attribute selectors.
     *
     * <p>Two "@" characters in a row (@@) are considered a single "@" because the
     * first "@" acts as an escape character for the second.
     */
    private static final class MessageTemplateParser extends SimpleParser {
        private int mark = 0;
        private final List<Function<AttributeValue, CharSequence>> parts = new ArrayList<>();

        private MessageTemplateParser(String expression) {
            super(expression);
        }

        MessageTemplate parse() {
            while (!eof()) {
                consumeWhile(c -> c != '@');
                // '@' followed by '@' is an escaped '@", so keep parsing
                // the marked literal if that's the case.
                if (peek(1) == '@') {
                    skip(); // consume the first @.
                    addLiteralPartIfNecessary();
                    skip(); // skip the escaped @.
                    mark++;
                } else if (!eof()) {
                    addLiteralPartIfNecessary();
                    List<String> path = AttributeValue.parseScopedAttribute(this);
                    parts.add(attributeValue -> attributeValue.getPath(path).toMessageString());
                    mark = position();
                }
            }

            addLiteralPartIfNecessary();
            return new MessageTemplate(input(), parts);
        }

        @Override
        public RuntimeException syntax(String message) {
            return new RuntimeException("Syntax error at line " + line() + " column " + column()
                    + " of EmitEachSelector message template: " + message);
        }

        private void addLiteralPartIfNecessary() {
            CharSequence slice = borrowSliceFrom(mark);
            if (slice.length() > 0) {
                parts.add(ignoredAttribute -> slice);
            }
            mark = position();
        }
    }
}
