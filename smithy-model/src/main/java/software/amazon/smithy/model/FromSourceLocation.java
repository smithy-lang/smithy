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
