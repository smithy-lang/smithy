/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.ShapeId;

public class MemberCursor implements ShapeCursor {

    private final ModelIndex index;
    private final ShapeId id;

    public MemberCursor(ModelIndex index, ShapeId id) {
        this.index = index;
        this.id = id;
    }

    @Override
    public ShapeId toShapeId() {
        return id;
    }

    @Override
    public ModelIndex index() {
        return index;
    }

    // Mixin members???
    public List<ShapeCursor> getMixins() {
        List<ShapeCursor> mixins = new ArrayList<>();

        for (ShapeCursor mixin : getContainer().getMixins()) {
            Map<String, MemberCursor> mixinMembers = mixin.getMembers();
            if (mixinMembers.containsKey(getMemberName())) {
                mixins.add(mixinMembers.get(getMemberName()));
            }
        }

        return mixins;
    }

    public String getMemberName() {
        return id.getMember().orElseThrow(() -> new IllegalStateException("No member name: " + toShapeId()));
    }

    public ShapeId getTargetId() {
        return Objects.requireNonNull(getFirstEdge(RelationshipType.MEMBER_TARGET).target());
    }

    public ShapeCursor getTarget() {
        return index.getShape(getTargetId());
    }

    public ShapeId getContainerId() {
        return id.withoutMember();
    }

    public ShapeCursor getContainer() {
        return index.getShape(getContainerId());
    }
}
