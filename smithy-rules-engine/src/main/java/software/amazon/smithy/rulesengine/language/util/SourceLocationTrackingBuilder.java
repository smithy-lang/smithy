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

import static software.amazon.smithy.rulesengine.language.util.SourceLocationUtils.javaLocation;

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
public abstract class SourceLocationTrackingBuilder<B extends SourceLocationTrackingBuilder<?, ?>, T>
        implements SmithyBuilder<T>, FromSourceLocation {

    protected SourceLocation sourceLocation;

    public SourceLocationTrackingBuilder(FromSourceLocation sourceLocation) {
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

}
