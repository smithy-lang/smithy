/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.ShapeType;

public class AndSelectorTest {
    @Test
    public void loadsEmptyArray() {
        InternalSelector selector = AndSelector.of(Collections.emptyList());

        assertThat(selector, is(InternalSelector.IDENTITY));
    }

    @Test
    public void loadsSingleSelector() {
        ShapeTypeSelector shapeTypeSelector = new ShapeTypeSelector(ShapeType.STRING);
        InternalSelector selector = AndSelector.of(Collections.singletonList(shapeTypeSelector));

        assertThat(selector, is(shapeTypeSelector));
    }
}
