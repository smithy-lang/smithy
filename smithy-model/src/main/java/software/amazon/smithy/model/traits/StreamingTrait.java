/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that the data stored in the shape is very large and should
 * not be stored in memory, or that the size of the data stored in the
 * shape is unknown at the start of a request.
 */
public final class StreamingTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#streaming");

    public StreamingTrait(ObjectNode node) {
        super(ID, node);
    }

    public StreamingTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<StreamingTrait> {
        public Provider() {
            super(ID, StreamingTrait::new);
        }
    }

    /**
     * Determines whether a given shape is an event stream.
     *
     * @param shape The shape to check.
     * @return True if the shape is a union and has the streaming trait.
     */
    public static boolean isEventStream(Shape shape) {
        return shape.isUnionShape() && shape.hasTrait(ID);
    }

    /**
     * Determines whether a given member targets an event stream.
     *
     * @param model The model containing the member and its target.
     * @param member The member whose target should be checked.
     * @return True if the member targets a union with the streaming trait.
     */
    public static boolean isEventStream(Model model, MemberShape member) {
        return isEventStream(model.expectShape(member.getTarget()));
    }
}
