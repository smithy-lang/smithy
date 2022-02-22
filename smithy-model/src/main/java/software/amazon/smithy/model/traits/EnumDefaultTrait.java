/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.traits;

import java.util.Collections;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that an enum member is the default value member.
 *
 * On an {@link IntEnumShape} this implies the enum value is 0. On an
 * {@link  EnumShape} this implies the enum value is an empty string.
 */
public class EnumDefaultTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#enumDefault");

    public EnumDefaultTrait(ObjectNode node) {
        super(ID, node);
    }

    public EnumDefaultTrait(SourceLocation location) {
        this(new ObjectNode(Collections.emptyMap(), location));
    }

    public EnumDefaultTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<EnumDefaultTrait> {
        public Provider() {
            super(ID, EnumDefaultTrait::new);
        }
    }
}
