/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
