/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
