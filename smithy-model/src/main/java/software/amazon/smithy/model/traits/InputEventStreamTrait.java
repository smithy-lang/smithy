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
 * Trait implementation of inputEventStream.
 */
public final class InputEventStreamTrait extends StringTrait {
    private static final String TRAIT = "smithy.api#inputEventStream";

    public InputEventStreamTrait(String value, SourceLocation sourceLocation) {
        super(TRAIT, value, sourceLocation);
    }

    public InputEventStreamTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<InputEventStreamTrait> {
        public Provider() {
            super(TRAIT, InputEventStreamTrait::new);
        }
    }
}
