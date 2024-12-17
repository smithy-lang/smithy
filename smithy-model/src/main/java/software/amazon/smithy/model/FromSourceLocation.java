/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model;

/**
 * A value that can be traced back to a {@link SourceLocation}.
 */
public interface FromSourceLocation {

    /**
     * Gets the source location of a value.
     *
     * @return Returns the source location of the value.
     */
    default SourceLocation getSourceLocation() {
        return SourceLocation.none();
    }

    /**
     * Compares two FromSourceLocations.
     *
     * @param s1 the first FromSourceLocation to compare.
     * @param s2 the second FromSourceLocation to compare.
     * @return the value 0 if s1 == s2; a value less than 0 if s1 &lt; s2; and a value greater than 0 if s1 &gt; s2.
     */
    static int compare(FromSourceLocation s1, FromSourceLocation s2) {
        return s1.getSourceLocation().compareTo(s2.getSourceLocation());
    }
}
