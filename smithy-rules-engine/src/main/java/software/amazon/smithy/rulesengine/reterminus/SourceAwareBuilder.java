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

package software.amazon.smithy.rulesengine.reterminus;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.utils.SmithyBuilder;

public abstract class SourceAwareBuilder<B extends SourceAwareBuilder<?, ?>, T>
        implements SmithyBuilder<T>, FromSourceLocation {

    protected SourceLocation sourceLocation;

    public SourceAwareBuilder(FromSourceLocation sourceLocation) {
        if (sourceLocation.getSourceLocation() == SourceLocation.NONE) {
            this.sourceLocation = javaLocation();
        } else {
            this.sourceLocation = sourceLocation.getSourceLocation();
        }
    }

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
     * Tests if the given {@code StackTraceElement} is relevant for a comment
     * used when writing debug information before calls to write.
     *
     * <p>The default implementation filters out all methods in "java.*",
     * AbstractCodeWriter, software.amazon.smithy.utils.SymbolWriter,
     * SimpleCodeWriter, and methods of the implementing subclass of
     * AbstractCodeWriter. This method can be overridden to further filter
     * stack frames as needed.
     *
     * @param e StackTraceElement to test.
     * @return Returns true if this element should be in a comment.
     */
    static boolean isStackTraceRelevant(StackTraceElement e) {
        String normalized = e.getClassName().replace("$", ".");
        return !normalized.startsWith("java.")
               && !normalized.startsWith("jdk.")
               // Ignore writes made by AbstractCodeWriter or AbstractCodeWriter$State.
               && !normalized.startsWith(SourceAwareBuilder.class.getCanonicalName())
               //&& !normalized.startsWith(getClass().getCanonicalName())
               && !normalized.startsWith("software.amazon.smithy.rulesengine");
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @SuppressWarnings("unchecked")
    public B sourceLocation(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
        return (B) this;
    }

}
