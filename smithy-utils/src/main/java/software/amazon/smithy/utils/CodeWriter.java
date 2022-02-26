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
 * Helper class for generating code.
 *
 * <p>This class is a general purpose implementation of {@link AbstractCodeWriter}.
 */
public class CodeWriter extends AbstractCodeWriter<CodeWriter> {
    /**
     * Creates a default instance of a CodeWriter that uses "\n" for newlines,
     * flattens multiple successive blank lines into a single blank line,
     * and adds a trailing new line if needed when converting the CodeWriter
     * to a string.
     *
     * @return Returns the created and configured CodeWriter.
     */
    public static CodeWriter createDefault() {
        return new CodeWriter().trimTrailingSpaces();
    }
}
