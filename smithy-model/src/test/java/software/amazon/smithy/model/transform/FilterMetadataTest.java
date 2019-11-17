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

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;

public class FilterMetadataTest {
    @Test
    public void filtersMetadata() {
        Model model = Model.builder()
                .putMetadataProperty("foo", Node.from("string"))
                .putMetadataProperty("baz", Node.from(1))
                .putMetadataProperty("lorem", Node.from(true))
                .build();
        ModelTransformer transformer = ModelTransformer.create();
        // Remove boolean and number metadata key-value pairs.
        Model result = transformer.filterMetadata(model, (k, v) -> !v.isBooleanNode() && !v.isNumberNode());

        assertThat(result.getMetadata().get("foo"), Matchers.equalTo(Node.from("string")));
        assertThat(result.getMetadata().get("baz"), Matchers.nullValue());
        assertThat(result.getMetadata().get("lorem"), Matchers.nullValue());
    }
}
