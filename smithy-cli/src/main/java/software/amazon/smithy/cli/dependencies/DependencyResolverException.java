/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
