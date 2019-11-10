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

package software.amazon.smithy.codegen.core;

import java.util.List;

/**
 * A container for {@link SymbolDependency} objects.
 */
public interface SymbolDependencyContainer {
    /**
     * Gets the list of dependencies that this object introduces.
     *
     * <p>A dependency is a dependency on another package that a Symbol
     * or type requires. It is quite different from a reference since a
     * reference only refers to a symbol; a reference provides no context
     * as to whether or not a dependency is required or the dependency's
     * coordinates.
     *
     * @return Returns the dependencies.
     */
    List<SymbolDependency> getDependencies();
}
