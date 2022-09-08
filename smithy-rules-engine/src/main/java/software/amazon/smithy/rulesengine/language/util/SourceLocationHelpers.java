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

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class SourceLocationHelpers {
    private SourceLocationHelpers() {
    }

    /**
     * Returns the source code location of the caller. This is very helpful when determining where rules generated
     * from Java-based builders originated.
     *
     * <p>Library-local and JDK stack frames are discarded. If no relevant location can be found,
     * `SourceLocation.none()` is returned.
     *
     * @return The SourceLocation.
     */
    public static SourceLocation javaLocation() {
        for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
            if (isStackTraceRelevant(e)) {
                if (e.getFileName() == null) {
                    return SourceLocation.none();
                }
                return new SourceLocation(e.getFileName(), e.getLineNumber(), 0);
            }
        }
        return SourceLocation.none();
    }

    /**
     * Prints a source location in "stack trace form": `filename:linenumber`.
     * @param sourceLocation Source location to print
     * @return formatted source location
     */
    public static String stackTraceForm(SourceLocation sourceLocation) {
        if (sourceLocation == SourceLocation.NONE) {
            return "N/A";
        }
        StringBuilder sb = new StringBuilder();
        if (sourceLocation.getFilename() != null) {
            sb.append(sourceLocation.getFilename());
        }
        if (sourceLocation.getLine() != 0) {
            sb.append(":").append(sourceLocation.getLine());
        }
        /* column is ignored in stack trace form */
        return sb.toString();
    }

    /**
     * Tests if the given {@code StackTraceElement} is relevant for a comment
     * used when writing debug information before calls to write.
     *
     * <p>The implementation filters out all methods in "java.*",
     * and `software.amazon.smithy.rulesengine`
     *
     * @param e StackTraceElement to test.
     * @return Returns true if this element should be in a comment.
     */
    static boolean isStackTraceRelevant(StackTraceElement e) {
        String normalized = e.getClassName().replace("$", ".");
        return !normalized.startsWith("java.")
                && !normalized.startsWith("jdk.")
                && !normalized.startsWith(SourceLocationHelpers.class.getCanonicalName())
                && !normalized.startsWith("software.amazon.smithy.rulesengine");
    }
}
