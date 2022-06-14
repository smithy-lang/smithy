/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.cloudformation.traits;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringTrait;

/**
 * A CFN default value is the value that is set for a property when no other value is provided.
 **/

public final class CfnDefaultValueTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("aws.cloudformation#cfnDefaultValue");

    public CfnDefaultValueTrait(String value, SourceLocation sourceLocation) { super(ID, value, sourceLocation); }

    public CfnDefaultValueTrait(String value) { this(value, SourceLocation.NONE); }

    public static final class Provider extends StringTrait.Provider<CfnDefaultValueTrait> {
        public Provider() { super(ID, CfnDefaultValueTrait::new); }
    }
}
