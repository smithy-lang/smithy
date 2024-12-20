/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.Collection;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Plugin service used with ModelTransformer.
 *
 * <p>Implementations of this service are found via SPI. Each time
 * a shape is removed from a model, {@link #onRemove} is called and
 * given an opportunity to create and return a newly transformed model.
 */
public interface ModelTransformerPlugin {
    /**
     * The method that is invoked each time shapes are removed from a model.
     *
     * @param transformer Transformer used to replace/remove shapes from the model.
     * @param removed The of shapes that were removed from the {@code model}.
     * @param model Model that has been altered to remove {@code removed}.
     * @return Returns a transformed version of the passed in model.
     */
    default Model onRemove(ModelTransformer transformer, Collection<Shape> removed, Model model) {
        return model;
    }
}
