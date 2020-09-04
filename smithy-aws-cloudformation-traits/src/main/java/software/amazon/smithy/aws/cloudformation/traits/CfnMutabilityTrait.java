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
 * Indicates an explicit CloudFormation mutability of the structure member
 * when part of a CloudFormation resource.
 */
public final class CfnMutabilityTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("aws.cloudformation#cfnMutability");

    public CfnMutabilityTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public static final class Provider extends StringTrait.Provider<CfnMutabilityTrait> {
        public Provider() {
            super(ID, CfnMutabilityTrait::new);
        }
    }

    public boolean isFullyMutable() {
        return getValue().equals("full");
    }

    public boolean isCreate() {
        return getValue().equals("create");
    }

    public boolean isCreateAndRead() {
        return getValue().equals("create-and-read");
    }

    public boolean isRead() {
        return getValue().equals("read");
    }

    public boolean isWrite() {
        return getValue().equals("write");
    }
}
