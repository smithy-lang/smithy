/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation;

import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Represents a service provider for configurable Validators that appear
 * in Smithy models.
 */
public interface ValidatorService {
    /**
     * Gets the name of the validator.
     *
     * <p>This name is used to find a matching validator configured in the
     * Smithy model and match it to an implementation.
     *
     * @return Returns the name of the validator it creates.
     */
    String getName();

    /**
     * Creates a validator using configuration.
     *
     * @param configuration Validator configuration.
     * @return Returns the created validator.
     */
    Validator createValidator(ObjectNode configuration);

    /**
     * Determines the name of a validator based on a class name.
     *
     * <p>This method returns the simple name of a class and strips off
     * "Validator" from the end of the name if present.
     *
     * @param clazz Class to determine the validator name of.
     * @return Returns the validator name based on heuristics.
     */
    static String determineValidatorName(Class<?> clazz) {
        String name = clazz.getSimpleName();
        return name.endsWith("Validator") ? name.substring(0, name.length() - "Validator".length()) : name;
    }

    /**
     * Provides a simple abstraction for creating validator service subclasses.
     */
    abstract class Provider implements ValidatorService {
        private String name;
        private Function<ObjectNode, ? extends Validator> provider;

        public Provider(String name, Function<ObjectNode, ? extends Validator> provider) {
            this.name = name;
            this.provider = provider;
        }

        public <T extends Validator> Provider(Class<T> klass, Function<ObjectNode, T> provider) {
            this(determineValidatorName(klass), provider);
        }

        public <T extends Validator> Provider(Class<T> klass, Supplier<T> supplier) {
            this(determineValidatorName(klass), c -> supplier.get());
        }

        @Override
        public final String getName() {
            return name;
        }

        @Override
        public final Validator createValidator(ObjectNode configuration) {
            return provider.apply(configuration);
        }
    }
}
