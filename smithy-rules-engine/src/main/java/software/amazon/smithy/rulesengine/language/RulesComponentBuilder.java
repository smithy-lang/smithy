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

package software.amazon.smithy.rulesengine.language;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Builder which also tracks sourceLocation of inputs. If no source location is provided, the source location defaults
 * to the Java file/line number of the caller.
 * @param <B> Type of the builder
 * @param <T> Type of the type that is built
 */
@SmithyUnstableApi
public abstract class RulesComponentBuilder<B extends RulesComponentBuilder<?, ?>, T>
        implements SmithyBuilder<T>, FromSourceLocation {

    private SourceLocation sourceLocation;

    public RulesComponentBuilder(FromSourceLocation sourceLocation) {
        if (sourceLocation.getSourceLocation() == SourceLocation.NONE) {
            this.sourceLocation = javaLocation();
        } else {
            this.sourceLocation = sourceLocation.getSourceLocation();
        }
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @SuppressWarnings("unchecked")
    public B sourceLocation(FromSourceLocation fromSourceLocation) {
        this.sourceLocation = fromSourceLocation.getSourceLocation();
        return (B) this;
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
                       && !normalized.startsWith(RulesComponentBuilder.class.getCanonicalName())
                       && !normalized.startsWith("software.amazon.smithy.rulesengine");
    }
}
