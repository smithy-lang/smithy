/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
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
