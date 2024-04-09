/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

/**
 * A trait that indicates that a service has handwritten endpoint rules.
 *
 * Services marked with this trait have handwritten endpoint rules that
 * extend or replace their standard generated endpoint rules through an external mechanism.
 * This trait marks the presence of handwritten rules, which are added to the model by a transformer,
 * but does not specify their behavior.
 */
public final class RuleBasedEndpointsTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("aws.endpoints#rulesBasedEndpoints");

    public RuleBasedEndpointsTrait(ObjectNode node) {
        super(ID, node);
    }

    public RuleBasedEndpointsTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<RuleBasedEndpointsTrait> {
        public Provider() {
            super(ID, RuleBasedEndpointsTrait::new);
        }
    }
}
