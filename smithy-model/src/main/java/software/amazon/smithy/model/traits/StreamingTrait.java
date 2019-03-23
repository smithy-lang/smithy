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

package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.SourceLocation;

/**
 * Indicates that the the data stored in the shape is very large and should
 * not be stored in memory, or that the size of the data stored in the
 * shape is unknown at the start of a request.
 *
 * TODO: Ensure that a streaming blob is also the payload of HTTP bindings.
 * TODO: Ensure that there is only one streaming blob per operation in/out.
 */
public final class StreamingTrait extends BooleanTrait {
    private static final String TRAIT = "smithy.api#streaming";

    public StreamingTrait(SourceLocation sourceLocation) {
        super(TRAIT, sourceLocation);
    }

    public StreamingTrait() {
        this(SourceLocation.NONE);
    }

    public static TraitService provider() {
        return TraitService.createAnnotationProvider(TRAIT, StreamingTrait::new);
    }
}
