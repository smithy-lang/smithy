/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.ServiceLoader;

/**
 * Creates {@link ValidationEventDecorator} instances.
 */
public interface ValidationEventDecoratorFactory {
    /**
     * Returns a list of decorators.
     *
     * @return Returns the loaded decorators.
     */
    List<ValidationEventDecorator> loadDecorators();

    /**
     * Creates a {@code ValidationEventDecoratorFactory} that uses the given collection of
     * {@code ValidationEventDecorator}.
     *
     * @param services List of TraitService provider instances.
     * @return Returns the created TraitFactory.
     */
    static ValidationEventDecoratorFactory createServiceFactory(Iterable<ValidationEventDecorator> services) {
        List<ValidationEventDecorator> decorators = new ArrayList<>();
        services.forEach(decorators::add);
        List<ValidationEventDecorator> unmodifiableDecorators = Collections.unmodifiableList(decorators);
        return () -> unmodifiableDecorators;
    }

    /**
     * Creates a {@code ValidationEventDecoratorFactory} that discovers {@code ValidationEventDecorator} providers using
     * the given ClassLoader.
     *
     * @param classLoader Class loader used to find TraitService providers.
     * @return Returns the created ValidationEventDecoratorFactory.
     */
    static ValidationEventDecoratorFactory createServiceFactory(ClassLoader classLoader) {
        return createServiceFactory(ServiceLoader.load(ValidationEventDecorator.class, classLoader));
    }

}
