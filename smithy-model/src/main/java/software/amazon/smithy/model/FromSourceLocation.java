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
}
