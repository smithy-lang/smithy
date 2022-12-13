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

package software.amazon.smithy.utils;

/**
 * A mutable object that can be used to create an immutable object of type T.
 *
 * @param <T> the type that the builder will build.
 */
public interface SmithyBuilder<T> {

    /**
     * Creates an immutable object that is created from the properties
     * that have been set on the builder.
     *
     * @return an instance of T
     */
    T build();

    /**
     * Convenience method for ensuring that a value was set on a builder,
     * and if not, throws an IllegalStateException with a useful message.
     *
     * @param method Method that needs to be called to set this value.
     * @param value Value to check.
     * @param <T> Type of value being checked.
     * @return Returns the value.
     */
    static <T> T requiredState(String method, T value) {
        if (value == null) {
            StringBuilder message = new StringBuilder(method).append(" was not set on the builder");

            // Include the builder class that could not be built.
            StackTraceElement[] elements = Thread.currentThread().getStackTrace();
            if (elements.length >= 2) {
                String builder = elements[2].getClassName();
                message.append(" (builder class is probably ").append(builder).append(')');
            }

            throw new IllegalStateException(message.toString());
        }
        return value;
    }
}
