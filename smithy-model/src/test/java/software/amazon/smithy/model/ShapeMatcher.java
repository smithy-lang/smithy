/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Does some heavy-lifting around making assertions about changes made to the model.
 *
 * @param <S> Type of shape we expect to be making assertions about.
 */
@SmithyInternalApi
public final class ShapeMatcher<S extends Shape> extends TypeSafeMatcher<ShapeId> {
    private final Class<S> shapeType;
    private final ValidatedResult<Model> result;
    private final List<Pair<Predicate<S>, Function<S, String>>> assertions;
    private final String description;

    public ShapeMatcher(
            Class<S> shapeType,
            ValidatedResult<Model> result,
            String description,
            List<Pair<Predicate<S>, Function<S, String>>> assertions
    ) {
        this.shapeType = shapeType;
        this.result = result;
        this.description = description;
        this.assertions = assertions;
    }

    /**
     * Creates a builder for a given type of shape and validated model result.
     *
     * @param shapeType Shape that is expected to be validated.
     * @param result The validated model result.
     * @param <S> The type of shape being validated.
     * @return Returns a builder.
     */
    public static <S extends Shape> Builder<S> builderFor(Class<S> shapeType, ValidatedResult<Model> result) {
        return new Builder<>(shapeType, result);
    }

    /**
     * Creates a matcher that ensures the validated result contains a validation event that
     * matches the given event ID, severity, and contains a specific string in the error message.
     *
     * @param result Result to check for the shape ID.
     * @param eventId Event ID to find.
     * @param severity Severity to find.
     * @param messageContains Message contents to find.
     * @return Returns the created matcher.
     */
    public static ShapeMatcher<Shape> hasEvent(
            ValidatedResult<Model> result,
            String eventId,
            Severity severity,
            String messageContains
    ) {
        return builderFor(Shape.class, result).addEventAssertion(eventId, severity, messageContains).build();
    }

    /**
     * Creates a matcher that ensures a member is not marked with @default or @required.
     *
     * @param result The validated result to check.
     * @return Returns the created matcher.
     */
    public static ShapeMatcher<MemberShape> memberIsNullable(ValidatedResult<Model> result) {
        return ShapeMatcher.builderFor(MemberShape.class, result)
                .description("Member is marked with @required or @default trait")
                .addAssertion(member -> !member.hasTrait(RequiredTrait.class),
                        member -> "Member is marked with the @required trait")
                .addAssertion(member -> {
                    DefaultTrait trait = member.getTrait(DefaultTrait.class).orElse(null);
                    return trait == null || trait.toNode().isNullNode();
                },
                        member -> "Member is marked with the @default trait")
                .build();
    }

    private static boolean findEvent(
            ValidatedResult<Model> result,
            String eventId,
            ToShapeId id,
            Severity severity,
            String contains
    ) {
        for (ValidationEvent event : result.getValidationEvents()) {
            if (event.getShapeId().filter(sid -> sid.equals(id.toShapeId())).isPresent()
                    && event.getSeverity() == severity
                    && event.containsId(eventId)
                    && event.getMessage().contains(contains)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected final boolean matchesSafely(ShapeId id) {
        if (!result.getResult().isPresent()) {
            return false;
        }

        return result.getResult()
                .flatMap(model -> model.getShape(id))
                .filter(shapeType::isInstance)
                .map(shapeType::cast)
                .filter(s -> {
                    for (Pair<Predicate<S>, Function<S, String>> pair : assertions) {
                        if (!pair.left.test(s)) {
                            return false;
                        }
                    }
                    return true;
                })
                .isPresent();
    }

    @Override
    protected void describeMismatchSafely(ShapeId item, Description mismatchDescription) {
        if (!result.getResult().isPresent()) {
            mismatchDescription.appendText("Model is broken: " + result.getValidationEvents().toString());
            return;
        }

        Model model = result.getResult().get();
        Shape shape = model.getShape(item).orElse(null);

        if (shape == null) {
            mismatchDescription.appendText("Shape " + item + " not found in the model");
        } else if (!shapeType.isInstance(shape)) {
            mismatchDescription.appendText("Shape " + item + " was " + shape.getClass() + " not " + shapeType);
        } else {
            S s = shapeType.cast(shape);
            for (Pair<Predicate<S>, Function<S, String>> pair : assertions) {
                if (!pair.left.test(s)) {
                    mismatchDescription.appendText(pair.right.apply(s));
                    return;
                }
            }
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(this.description);
    }

    /**
     * A builder to create a ShapeMatcher.
     *
     * @param <S> Type of shape being matched.
     */
    public static final class Builder<S extends Shape> {
        private final Class<S> shapeType;
        private final ValidatedResult<Model> result;
        private final List<Pair<Predicate<S>, Function<S, String>>> assertions = new ArrayList<>();
        private String description = "ShapeMatcher assertion";

        Builder(Class<S> type, ValidatedResult<Model> result) {
            this.shapeType = type;
            this.result = result;
        }

        /**
         * Creates the matcher.
         *
         * @return Returns the matcher that can be used in assertions.
         */
        public ShapeMatcher<S> build() {
            return new ShapeMatcher<>(shapeType, result, description, assertions);
        }

        /**
         * Sets the description of the matcher.
         *
         * @param description Description to set.
         * @return Returns the builder.
         */
        public Builder<S> description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Adds an assertion to the matcher. It's expected to return true, otherwise the given
         * error message supplier is called with the shape that didn't match, and it's expected
         * to return an error message.
         *
         * @param test Predicate to test the shape against.
         * @param errorSupplier Function used to create an error message.
         * @return Returns the builder.
         */
        public Builder<S> addAssertion(Predicate<S> test, Function<S, String> errorSupplier) {
            assertions.add(Pair.of(test, errorSupplier));
            return this;
        }

        /**
         * Add an assertion that the shape ID given to the matcher has a corresponding event with the
         * given ID, severity, and that the message contains the given string.
         *
         * @param eventId Event ID to test.
         * @param severity Severity to expect.
         * @param messageContains Message contents to find.
         * @return Returns the builder.
         */
        public Builder<S> addEventAssertion(String eventId, Severity severity, String messageContains) {
            return addAssertion(shape -> findEvent(result, eventId, shape.getId(), severity, messageContains),
                    shape -> "No event matched ID " + eventId + " on shape " + shape.getId()
                            + " with severity " + severity + " containing a message of '"
                            + messageContains + "'. Found the following events: "
                            + result.getValidationEvents());
        }
    }
}
