/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.protocols.traits;
 
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
 
public final class SmithyRpcV2Trait extends SmithyProtocolTrait {
 
    public static final ShapeId ID = ShapeId.from("smithy.protocols#smithyRpcV2");
 
    private SmithyRpcV2Trait(Builder builder) {
        super(ID, builder);
    }
 
    public static Builder builder() {
        return new Builder();
    }
 
    public static final class Builder extends SmithyProtocolTrait.Builder<SmithyRpcV2Trait, Builder> {
        private Builder() {}
 
        @Override
        public SmithyRpcV2Trait build() {
            return new SmithyRpcV2Trait(this);
        }
    }
 
    public static final class Provider extends AbstractTrait.Provider {
 
        public Provider() {
            super(ID);
        }
 
        @Override
        public SmithyRpcV2Trait createTrait(ShapeId shapeId, Node value) {
            return builder().sourceLocation(value).fromNode(value).build();
        }
    }
}
