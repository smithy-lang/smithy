/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.dependencies;

/**
 * Exception encountered while attempting to resolve dependencies.
 */
public final class DependencyResolverException extends RuntimeException {
    public DependencyResolverException(String message) {
        super(message);
    }

    public DependencyResolverException(Throwable previous) {
        this(previous.getMessage(), previous);
    }

    public DependencyResolverException(String message, Throwable previous) {
        super(message, previous);
    }
}
