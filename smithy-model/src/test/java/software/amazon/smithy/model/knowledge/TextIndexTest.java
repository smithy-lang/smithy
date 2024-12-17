/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;

public class TextIndexTest {

    @Test
    public void handlesSyntheticTraits() {
        Model model = Model.assembler()
                .addImport(OperationIndexTest.class.getResource("text-index.smithy"))
                .assemble()
                .unwrap();

        TextIndex index = TextIndex.of(model);
        assertThat(index.getTextInstances(), hasSize(5));
    }
}
