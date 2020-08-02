/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.loader;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;


/**
 * A model file used for models that do not need forward reference
 * resolution (e.g., the JSON AST, manually loaded Nodes, pre-made
 * {@link AbstractShapeBuilder} objects, etc).
 *
 * @see AstModelLoader
 */
final class FullyResolvedModelFile extends AbstractMutableModelFile {

    /**
     * @param traitFactory Factory used to create traits when merging traits.
     */
    FullyResolvedModelFile(TraitFactory traitFactory) {
        super(traitFactory);
    }

    /**
     * Create a {@code FullyResolvedModelFile} from already built shapes.
     *
     * @param traitFactory Factory used to create traits when merging traits.
     * @param shapes Shapes to convert into builders and treat as a ModelFile.
     * @return Returns the create {@code FullyResolvedModelFile} containing the shapes.
     */
    static FullyResolvedModelFile fromShapes(TraitFactory traitFactory, Collection<Shape> shapes) {
        FullyResolvedModelFile modelFile = new FullyResolvedModelFile(traitFactory);
        for (Shape shape : shapes) {
            // Convert the shape to a builder and remove all the traits.
            // These traits are added to the trait container so that they
            // can be merged correctly with any other model.
            AbstractShapeBuilder<?, ?> builder = Shape.shapeToBuilder(shape).clearTraits();
            modelFile.onShape(builder);
            // Add the traits that were present on the shape.
            for (Trait trait : shape.getAllTraits().values()) {
                modelFile.onTrait(shape.getId(), trait);
            }
        }
        return modelFile;
    }

    @Override
    public TraitContainer resolveShapes(Set<ShapeId> shapeIds, Function<ShapeId, ShapeType> typeProvider) {
        return traitContainer;
    }
}
