/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.mqtt.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

/**
 * Binds a member to an MQTT label using the member name.
 */
public final class TopicLabelTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.mqtt#topicLabel");

    public TopicLabelTrait(ObjectNode node) {
        super(ID, node);
    }

    public TopicLabelTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<TopicLabelTrait> {
        public Provider() {
            super(ID, TopicLabelTrait::new);
        }
    }
}
