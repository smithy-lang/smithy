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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Creates built-in {@code Validator} instances and {@code Validator}
 * instances loaded by name.
 */
public interface ValidatorFactory {
    /**
     * Returns a list of built-in validators that are always applied to a
     * Model regardless of if they are explicitly configured in the metadata
     * of a model. This is in contrast to validators returns from calling
     * {@link #createValidator}, which are only applied to a model if they
     * are declared in the metadata section of a model.
     *
     * @return Returns the loaded validators.
     */
    List<Validator> loadBuiltinValidators();

    /**
     * Creates and configures a Validator by name.
     *
     * @param name Name of the validator to create.
     * @param configuration Configuration data used to create the validator.
     * @return Returns the created validator wrapped in an Optional.
     * @throws SourceException on configuration error.
     * @throws RuntimeException if an error occurs while creating.
     */
    Optional<Validator> createValidator(String name, ObjectNode configuration);

    /**
     * Creates a ValidatorFactory that uses a collection of built-in validators
     * and a collection of ValidatorService instances for configurable validators.
     *
     * <p>The validators and services passed to this method are copied into a
     * List before creating the actual factory to avoid holding on to
     * {@code ServiceLoader} VM-wide instances that potentially utilize a
     * Thread's context ClassLoader.
     *
     * @param validators Built-in validators to provide from the factory.
     * @param services ValidatorService instances to use to create validators.
     * @return Returns the created ValidatorFactory.
     */
    static ValidatorFactory createServiceFactory(Iterable<Validator> validators, Iterable<ValidatorService> services) {
        List<ValidatorService> serviceList = new ArrayList<>();
        services.forEach(serviceList::add);
        List<Validator> validatorsList = new ArrayList<>();
        validators.forEach(validatorsList::add);

        return new ValidatorFactory() {
            @Override
            public List<Validator> loadBuiltinValidators() {
                return Collections.unmodifiableList(validatorsList);
            }

            @Override
            public Optional<Validator> createValidator(String name, ObjectNode configuration) {
                return serviceList.stream()
                        .filter(service -> service.getName().equals(name))
                        .map(service -> service.createValidator(configuration))
                        .findFirst();
            }
        };
    }

    /**
     * Creates a ValidatorFactory that discovers service providers using
     * the given ClassLoader.
     *
     * @param classLoader Class loader used to find ValidatorProviders.
     * @return Returns the created factory.
     */
    static ValidatorFactory createServiceFactory(ClassLoader classLoader) {
        return createServiceFactory(
                ServiceLoader.load(Validator.class, classLoader),
                ServiceLoader.load(ValidatorService.class, classLoader));
    }
}
