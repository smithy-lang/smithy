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
     * Creates a {@code ValidatorService} using the given name and factory function.
     *
     * @param name Name of the validator that this service provides.
     * @param creator Factory function used to create the {@link Validator}.
     * @param <V> Type of validator being created.
     * @return Returns the created {@code ValidatorService}.
     */
    static <V extends Validator> ValidatorService createProvider(String name, Function<ObjectNode, V> creator) {
        return new ValidatorService() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Validator createValidator(ObjectNode configuration) {
                return creator.apply(configuration);
            }
        };
    }

    /**
     * Creates a {@code ValidatorService} using the given class and factory function.
     *
     * @param clazz Class from which to derive a validator name.
     * @param creator Factory function used to create the {@link Validator}.
     * @param <V> Type of validator being created.
     * @return Returns the created {@code ValidatorService}.
     * @see #determineValidatorName
     */
    static <V extends Validator> ValidatorService createProvider(Class<V> clazz, Function<ObjectNode, V> creator) {
        return createProvider(determineValidatorName(clazz), creator);
    }

    /**
     * Creates a {@code ValidatorService} using the given class and supplier.
     *
     * <p>This kind of validator doesn't use configuration.
     *
     * @param clazz Class from which to derive a validator name.
     * @param creator Factory function used to create the {@link Validator}.
     * @param <V> Type of validator being created.
     * @return Returns the created {@code ValidatorService}.
     * @see #determineValidatorName
     */
    static <V extends Validator> ValidatorService createSimpleProvider(Class<V> clazz, Supplier<V> creator) {
        return createProvider(determineValidatorName(clazz), configuration -> creator.get());
    }
}
