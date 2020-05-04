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
