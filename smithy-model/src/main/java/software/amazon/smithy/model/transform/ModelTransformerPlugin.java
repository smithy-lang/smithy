/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

    /**
     * Defines the sort order of the plugin, a value from -128 to 127.
     *
     * <p>Plugins are applied according to this sort order. Lower values
     * are executed before higher values (for example, -128 comes before 0,
     * 0 comes before 127). Plugins default to 0.
     *
     * @return Returns the sort order, defaulting to 0.
     */
    default byte order() {
        return 0;
    }
}
