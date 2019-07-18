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
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Binds an operation input member to a named identifier of the resource to
 * which the operation is bound.
 */
public final class ResourceIdentifierTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#resourceIdentifier");

    public ResourceIdentifierTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public ResourceIdentifierTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<ResourceIdentifierTrait> {
        public Provider() {
            super(ID, ResourceIdentifierTrait::new);
        }
    }
}
