/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.protocols;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringTrait;

/**
 * Indicates the serialized name of a structure member when that structure is
 * serialized for the input of an EC2 operation.
 */
public final class Ec2QueryNameTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("aws.protocols#ec2QueryName");

    public Ec2QueryNameTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public Ec2QueryNameTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<Ec2QueryNameTrait> {
        public Provider() {
            super(ID, Ec2QueryNameTrait::new);
        }
    }
}
