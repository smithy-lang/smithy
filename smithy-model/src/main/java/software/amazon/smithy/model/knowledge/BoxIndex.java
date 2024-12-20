/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ToShapeId;

/**
 * An index that checks if a shape can be set to null.
 *
 * <p>This index is deprecated in favor of {@link NullableIndex}.
 */
@Deprecated
public final class BoxIndex extends NullableIndex {

    public BoxIndex(Model model) {
        super(model);
    }

    public static BoxIndex of(Model model) {
        return model.getKnowledge(BoxIndex.class, BoxIndex::new);
    }

    @Deprecated
    public boolean isBoxed(ToShapeId shape) {
        return isNullable(shape);
    }
}
