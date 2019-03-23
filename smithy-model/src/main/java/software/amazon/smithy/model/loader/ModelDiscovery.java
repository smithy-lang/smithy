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

package software.amazon.smithy.model.loader;

import java.net.URL;
import java.util.List;

/**
 * Provides Smithy models to merge into a ModelAssembler when
 * {@link ModelAssembler#discoverModels} is called.
 *
 * <p>{@code ModelDiscovery} service providers are discovered via
 * Java SPI.
 */
public interface ModelDiscovery {
    /**
     * Returns the URL locations of models that can be discovered by a
     * {@link ModelAssembler} when {@link ModelAssembler#discoverModels}
     * is called.
     *
     * <pre>
     * {@code
     * public final class MyModel implements ModelDiscovery {
     *     {@literal @}Override
     *     public List&lt;URL&gt; getModels() {
     *         return List.of(getClass().getResource("resource/path/to/model.smithy"));
     *     }
     * }
     * }
     * </pre>
     *
     * @return Returns the list of model locations.
     */
    List<URL> getModels();
}
