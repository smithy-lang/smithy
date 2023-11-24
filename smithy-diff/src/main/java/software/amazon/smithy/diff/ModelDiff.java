/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationEventDecorator;
import software.amazon.smithy.model.validation.suppressions.ModelBasedEventDecorator;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Computes the difference between two models and any problems that might
 * occur due to those differences.
 */
public final class ModelDiff {

    private ModelDiff() {}

    /**
     * Creates a new ModelDiff.Builder that provides in-depth diff analysis.
     *
     * @return Returns the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Evaluates the differences between two models.
     *
     * <p>Use {@link Builder} directly to get access to additional information.
     *
     * @param oldModel Previous version of the model.
     * @param newModel New model to compare.
     * @return Returns the computed validation events.
     */
    public static List<ValidationEvent> compare(Model oldModel, Model newModel) {
        return compare(ModelDiff.class.getClassLoader(), oldModel, newModel);
    }

    /**
     * Evaluates the differences between two models.
     *
     * <p>Use {@link Builder} directly to get access to additional information.
     *
     * @param classLoader ClassLoader used to find {@link DiffEvaluator} service providers.
     * @param oldModel Previous version of the model.
     * @param newModel New model to compare.
     * @return Returns the computed validation events.
     */
    public static List<ValidationEvent> compare(ClassLoader classLoader, Model oldModel, Model newModel) {
        return builder()
                .oldModel(oldModel)
                .newModel(newModel)
                .classLoader(classLoader)
                .compare()
                .getDiffEvents();
    }

    /**
     * The result of comparing two Smithy models.
     */
    public static final class Result {
        private final Differences differences;
        private final List<ValidationEvent> diffEvents;
        private final List<ValidationEvent> oldModelEvents;
        private final List<ValidationEvent> newModelEvents;

        public Result(
                Differences differences,
                List<ValidationEvent> diffEvents,
                List<ValidationEvent> oldModelEvents,
                List<ValidationEvent> newModelEvents
        ) {
            this.differences = Objects.requireNonNull(differences);
            this.diffEvents = Objects.requireNonNull(diffEvents);
            this.oldModelEvents = Objects.requireNonNull(oldModelEvents);
            this.newModelEvents = Objects.requireNonNull(newModelEvents);
        }

        /**
         * Gets a queryable set of differences between two models.
         *
         * @return Returns the differences.
         */
        public Differences getDifferences() {
            return differences;
        }

        /**
         * Gets the diff analysis as a list of {@link ValidationEvent}s.
         *
         * @return Returns the diff validation events.
         */
        public List<ValidationEvent> getDiffEvents() {
            return diffEvents;
        }

        /**
         * Gets the validation events emitted when validating the old model.
         *
         * @return Returns the old model's validation events.
         */
        public List<ValidationEvent> getOldModelEvents() {
            return oldModelEvents;
        }

        /**
         * Gets the validation events emitted when validating the new model.
         *
         * @return Returns the new model's validation events.
         */
        public List<ValidationEvent> getNewModelEvents() {
            return newModelEvents;
        }

        /**
         * Gets the validation events that were present in the old model but
         * are no longer an issue in the new model.
         *
         * @return Returns the resolved validation events.
         */
        public Set<ValidationEvent> determineResolvedEvents() {
            Set<ValidationEvent> events = new TreeSet<>(getOldModelEvents());
            events.removeAll(getNewModelEvents());
            return events;
        }

        /**
         * Gets the validation events that were introduced by whatever changes
         * were made to the new model.
         *
         * @return Returns the validation events introduced by the new model.
         */
        public Set<ValidationEvent> determineIntroducedEvents() {
            Set<ValidationEvent> events = new TreeSet<>(getNewModelEvents());
            events.removeAll(getOldModelEvents());
            return events;
        }

        /**
         * Determines if the diff events contain any DANGER or ERROR events.
         *
         * @return Returns true if this diff has breaking changes.
         */
        public boolean isDiffBreaking() {
            for (ValidationEvent event : getDiffEvents()) {
                if (event.getSeverity() == Severity.ERROR || event.getSeverity() == Severity.DANGER) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof Result)) {
                return false;
            }
            Result result = (Result) o;
            return getDifferences().equals(result.getDifferences())
                   && getDiffEvents().equals(result.getDiffEvents())
                   && getOldModelEvents().equals(result.getOldModelEvents())
                   && getNewModelEvents().equals(result.getNewModelEvents());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getDifferences(), getDiffEvents(), getOldModelEvents(), getNewModelEvents());
        }
    }

    /**
     * Builder used to construct a diff of two Smithy models.
     */
    public static final class Builder {
        private Model oldModel;
        private Model newModel;
        private List<ValidationEvent> oldModelEvents = Collections.emptyList();
        private List<ValidationEvent> newModelEvents = Collections.emptyList();
        private ClassLoader classLoader = ModelDiff.class.getClassLoader();

        private Builder() {}

        /**
         * Sets the ClassLoader used to find {@link DiffEvaluator} service
         * providers.
         *
         * @param classLoader ClassLoader to use.
         * @return Returns the builder.
         */
        public Builder classLoader(ClassLoader classLoader) {
            this.classLoader = Objects.requireNonNull(classLoader);
            return this;
        }

        /**
         * Sets the old model to compare against.
         *
         * @param oldModel Old version of a model.
         * @return Returns the builder.
         */
        public Builder oldModel(Model oldModel) {
            this.oldModel = Objects.requireNonNull(oldModel);
            return this;
        }

        /**
         * Sets the new model to compare against.
         *
         * @param newModel New version of a model.
         * @return Returns the builder.
         */
        public Builder newModel(Model newModel) {
            this.newModel = Objects.requireNonNull(newModel);
            return this;
        }

        /**
         * Sets the old model to compare against along with the validation
         * events encountered while loading the model.
         *
         * @param oldModel Old version of a model with events.
         * @return Returns the builder.
         */
        public Builder oldModel(ValidatedResult<Model> oldModel) {
            this.oldModel = oldModel.getResult()
                    .orElseThrow(() -> new IllegalArgumentException("No old model present in ValidatedResult"));
            this.oldModelEvents = oldModel.getValidationEvents();
            return this;
        }

        /**
         * Sets the new model to compare against along with the validation
         * events encountered while loading the model.
         *
         * @param newModel New version of a model with events.
         * @return Returns the builder.
         */
        public Builder newModel(ValidatedResult<Model> newModel) {
            this.newModel = newModel.getResult()
                    .orElseThrow(() -> new IllegalArgumentException("No new model present in ValidatedResult"));
            this.newModelEvents = newModel.getValidationEvents();
            return this;
        }

        /**
         * Performs the diff of the old and new models.
         *
         * @return Returns the diff {@link Result}.
         * @throws IllegalStateException if {@code oldModel} and {@code newModel} are not set.
         */
        public Result compare() {
            SmithyBuilder.requiredState("oldModel", oldModel);
            SmithyBuilder.requiredState("newModel", newModel);

            List<DiffEvaluator> evaluators = new ArrayList<>();
            ServiceLoader.load(DiffEvaluator.class, classLoader).forEach(evaluators::add);
            Differences differences = Differences.detect(oldModel, newModel);

            // Applies suppressions and elevates event severities.
            ValidationEventDecorator decoratorResult = new ModelBasedEventDecorator()
                    .createDecorator(newModel)
                    .getResult()
                    .orElse(ValidationEventDecorator.IDENTITY);

            List<ValidationEvent> diffEvents = evaluators.parallelStream()
                    .flatMap(evaluator -> evaluator.evaluate(differences).stream())
                    // No need to call canDecorate first since that method will always return true in any code path.
                    .map(decoratorResult::decorate)
                    .collect(Collectors.toList());

            return new Result(differences, diffEvents, oldModelEvents, newModelEvents);
        }
    }
}
