/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.mqtt.traits;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringTrait;

/**
 * smithy.mqtt#subscribe trait.
 */
public final class SubscribeTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("smithy.mqtt#subscribe");
    private final Topic topic;

    public SubscribeTrait(String topic, SourceLocation sourceLocation) {
        super(ID, topic, sourceLocation);
        this.topic = Topic.parse(topic);
    }

    public SubscribeTrait(String topic) {
        this(topic, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<SubscribeTrait> {
        public Provider() {
            super(ID, SubscribeTrait::new);
        }
    }

    /**
     * Gets the parsed topic of the trait.
     *
     * @return Returns the topic.
     */
    public Topic getTopic() {
        return topic;
    }
}
