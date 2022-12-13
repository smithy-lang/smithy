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

package software.amazon.smithy.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * A container for CodeSection.Interceptors registered with AbstractCodeWriter.
 */
@SmithyInternalApi
final class CodeInterceptorContainer<W extends AbstractCodeWriter<W>> {

    private final List<CodeInterceptor<CodeSection, W>> interceptors = new ArrayList<>();
    private final CodeInterceptorContainer<W> parent;

    CodeInterceptorContainer() {
        this(null);
    }

    CodeInterceptorContainer(CodeInterceptorContainer<W> parent) {
        this.parent = parent;
    }

    @SuppressWarnings("unchecked")
    void putInterceptor(CodeInterceptor<? extends CodeSection, W> interceptor) {
        interceptors.add((CodeInterceptor<CodeSection, W>) interceptor);
    }

    /**
     * Gets a list of interceptors that match the given type and for which the
     * result of {@link CodeInterceptor#isIntercepted(CodeSection)} returns true
     * when given {@code forSection}.
     *
     * @param forSection The section that is being intercepted.
     * @param <S> The type of section being intercepted.
     * @return Returns the list of matching interceptors.
     */
    <S extends CodeSection> List<CodeInterceptor<CodeSection, W>> get(S forSection) {
        // Add in parent interceptors.
        List<CodeInterceptor<CodeSection, W>> result = parent == null
                ? new ArrayList<>()
                : parent.get(forSection);
        // Merge in local interceptors.
        for (CodeInterceptor<CodeSection, W> interceptor : interceptors) {
            // Add the interceptor only if it's the right type.
            if (interceptor.sectionType().isInstance(forSection)) {
                // Only add if the filter passes.
                if (interceptor.isIntercepted(forSection)) {
                    result.add(interceptor);
                }
            }
        }

        return result;
    }
}
