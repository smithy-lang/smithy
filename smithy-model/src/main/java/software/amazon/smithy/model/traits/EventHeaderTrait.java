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
 * Marks a structure member of an event as an event header.
 *
 * <p>This trait can targets members of a structure marked with the event
 * trait that targets blob, boolean, integer, long, string, or timestamp.
 */
public final class EventHeaderTrait extends BooleanTrait {
    public static final String NAME = "smithy.api#eventHeader";

    public EventHeaderTrait(SourceLocation sourceLocation) {
        super(NAME, sourceLocation);
    }

    public EventHeaderTrait() {
        this(SourceLocation.NONE);
    }

    public static final class Provider extends BooleanTrait.Provider<EventHeaderTrait> {
        public Provider() {
            super(NAME, EventHeaderTrait::new);
        }
    }
}
