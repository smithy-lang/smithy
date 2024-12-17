/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Marks a structure member of an event as the event payload.
 *
 * This trait can targets members of a structure marked with the event trait
 * that targets a blob or structure.
 */
public final class EventPayloadTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#eventPayload");

    public EventPayloadTrait(ObjectNode node) {
        super(ID, node);
    }

    public EventPayloadTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<EventPayloadTrait> {
        public Provider() {
            super(ID, EventPayloadTrait::new);
        }
    }
}
