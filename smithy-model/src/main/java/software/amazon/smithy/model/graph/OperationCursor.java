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
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.UnitTypeTrait;

public class OperationCursor implements ShapeCursor {
    private final ModelIndex index;
    private final ShapeId id;

    public OperationCursor(ModelIndex index, ShapeId id) {
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

    public ShapeId getInputId() {
        ModelIndex.Edge e = getFirstEdge(RelationshipType.INPUT);
        return e == null ? UnitTypeTrait.UNIT : e.target();
    }

    public StructureCursor getInput() {
        return index.getStructure(getInputId());
    }

    public ShapeId getOutputId() {
        ModelIndex.Edge e = getFirstEdge(RelationshipType.OUTPUT);
        return e == null ? UnitTypeTrait.UNIT : e.target();
    }

    public StructureCursor getOutput() {
        return index.getStructure(getOutputId());
    }

    public List<ShapeId> getErrorIds() {
        List<ShapeId> errors = new ArrayList<>();
        for (ModelIndex.Edge e : getEdges()) {
            if (e.type() == RelationshipType.ERROR) {
                errors.add(e.target());
            }
        }
        return errors;
    }

    public List<StructureCursor> getErrors() {
        List<StructureCursor> errors = new ArrayList<>();
        getErrorIds().forEach(e -> errors.add(index.getStructure(e)));
        return errors;
    }
}
