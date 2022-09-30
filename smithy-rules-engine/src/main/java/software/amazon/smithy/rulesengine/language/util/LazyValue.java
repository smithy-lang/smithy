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

package software.amazon.smithy.rulesengine.language.util;

import java.util.function.Supplier;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A lazy initialized value that is initialized once when retrieved.
 * Not thread safe.
 *
 * @param <T> the type of the lazy initalized value.
 */
@SmithyUnstableApi
public final class LazyValue<T> {
    private final Supplier<T> initializer;
    private T value;
    private boolean initialized;

    private LazyValue(Builder<T> builder) {
        this.initializer = builder.initializer;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public T value() {
        if (!initialized) {
            value = initializer.get();
            initialized = true;
        }
        return value;
    }

    public static class Builder<T> {
        private Supplier<T> initializer;

        public Builder<T> initializer(Supplier<T> initializer) {
            this.initializer = initializer;
            return this;
        }

        public LazyValue<T> build() {
            return new LazyValue<>(this);
        }
    }
}
