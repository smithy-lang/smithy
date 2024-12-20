/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.mqtt.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

public final class MqttJsonTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.mqtt#mqttJson");

    public MqttJsonTrait(ObjectNode node) {
        super(ID, node);
    }

    public MqttJsonTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<MqttJsonTrait> {
        public Provider() {
            super(ID, MqttJsonTrait::new);
        }
    }
}
