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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;

final class TraitAttributeKey implements AttributeSelector.KeyGetter {

    private static final NodeToString NODE_TO_STRING = new NodeToString();
    private final String traitName;
    private final List<String> traitPath;

    TraitAttributeKey(String traitName, List<String> traitPath) {
        this.traitName = traitName;
        this.traitPath = traitPath;
    }

    TraitAttributeKey(String traitName) {
        this(traitName, Collections.emptyList());
    }

    @Override
    public List<String> apply(Shape shape) {
        Trait trait = shape.findTrait(traitName).orElse(null);

        if (trait == null) {
            // An empty list means the trait does not exist.
            return Collections.emptyList();
        } else if (traitPath.isEmpty()) {
            // A list with a value of null means it exists but isn't comparable.
            // A list with a non-null values means it exists and is comparable.
            return Collections.singletonList(trait.toNode().accept(NODE_TO_STRING));
        } else {
            // Path into the trait to see if a value exists / is comparable.
            List<String> result = new ArrayList<>();
            evaluateNode(trait.toNode(), 0, result);
            return result;
        }
    }

    private void evaluateNode(Node node, int pathPosition, List<String> result) {
        // Terminal state attempts to take the current value and
        // add it to the result. This is executes when pathing into
        // object node values.
        if (pathPosition >= traitPath.size()) {
            result.add(node.accept(NODE_TO_STRING));
            return;
        }

        String path = traitPath.get(pathPosition);
        if (node.isObjectNode()) {
            ObjectNode value = node.expectObjectNode();
            if (path.equals("(keys)")) {
                projectedEvaluate(value.getMembers().keySet(), pathPosition + 1, result);
            } else if (path.equals("(values)")) {
                projectedEvaluate(value.getMembers().values(), pathPosition + 1, result);
            } else if (value.getMember(path).isPresent()) {
                evaluateNode(value.expectMember(path), pathPosition + 1, result);
            }
        } else if (node.isArrayNode()) {
            // The only valid path after an array is (values).
            if (path.equals("(values)")) {
                projectedEvaluate(node.expectArrayNode().getElements(), pathPosition + 1, result);
            }
        }
    }

    private void projectedEvaluate(Collection<? extends Node> nodes, int pathPosition, List<String> result) {
        // If projecting on the last path item (i.e., an expression that ends
        // with (values)), then populate the result set with the evaluated values.
        if (pathPosition == traitPath.size()) {
            // Note that empty lists do not appear in the result set. Do not
            // project lists if you need to match on empty lists.
            for (Node element : nodes) {
                result.add(element.accept(NODE_TO_STRING));
            }
        } else {
            // Continue projecting and evaluating values inside of the trait.
            for (Node element : nodes) {
                evaluateNode(element, pathPosition, result);
            }
        }
    }

    // Only strings, booleans, and numbers are converted to
    // comparable strings. All other values become null, meaning
    // that the value is present, but not actually comparable.
    private static final class NodeToString extends NodeVisitor.Default<String> {
        @Override
        protected String getDefault(Node node) {
            return null;
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
