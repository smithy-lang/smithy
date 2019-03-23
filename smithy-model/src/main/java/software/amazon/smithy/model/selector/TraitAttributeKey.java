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

package software.amazon.smithy.model.selector;

import java.util.List;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;

final class TraitAttributeKey implements AttributeSelector.KeyGetter {
    private static final NodeToString NODE_TO_STRING = new NodeToString();

    private final String trait;

    TraitAttributeKey(String trait) {
        this.trait = trait;
    }

    @Override
    public List<String> apply(Shape shape) {
        return shape.findTrait(trait)
                .map(Trait::toNode)
                .map(node -> List.of(node.accept(NODE_TO_STRING)))
                .orElse(List.of());
    }

    private static final class NodeToString extends NodeVisitor.Default<String> {
        @Override
        protected String getDefault(Node node) {
            return "";
        }

        @Override
        public String stringNode(StringNode node) {
            return node.getValue();
        }

        @Override
        public String numberNode(NumberNode node) {
            return node.getValue().toString();
        }

        @Override
        public String booleanNode(BooleanNode node) {
            return Boolean.toString(node.getValue());
        }
    }
}
