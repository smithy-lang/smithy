/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
