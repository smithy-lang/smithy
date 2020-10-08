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
