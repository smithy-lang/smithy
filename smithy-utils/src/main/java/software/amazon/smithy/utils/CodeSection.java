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

/**
 * Interface used for strongly typed {@link AbstractCodeWriter} section values.
 */
public interface CodeSection {
    /**
     * Gets the name of the section.
     *
     * <p>This class will return the canonical class of the implementing
     * class by default.
     *
     * @return Returns the section name.
     */
    default String sectionName() {
        return getClass().getCanonicalName();
    }

    /**
     * Creates a CodeSection that returns the given {@code sectionName}.
     *
     * @param sectionName Section name to provide when {@code sectionName} is called on the created CodeSection.
     * @return Returns CodeSection that uses the provided name.
     */
    static CodeSection forName(String sectionName) {
        return new CodeSection() {
            @Override
            public String sectionName() {
                return sectionName;
            }
        };
    }
}
