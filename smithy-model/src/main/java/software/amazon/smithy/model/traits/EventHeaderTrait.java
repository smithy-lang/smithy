/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Marks a structure member of an event as an event header.
 *
 * <p>This trait can targets members of a structure marked with the event
 * trait that targets blob, boolean, integer, long, string, or timestamp.
 */
public final class EventHeaderTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#eventHeader");

    public EventHeaderTrait(ObjectNode node) {
        super(ID, node);
    }

    public EventHeaderTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<EventHeaderTrait> {
        public Provider() {
            super(ID, EventHeaderTrait::new);
        }
    }
}
