/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.model.transform;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;

public class RemoveInvalidDefaultsTest {
    @Test
    public void removeInvalidDefaultsBasedOnRangeTrait() {
        Model input = Model.assembler()
                .addImport(getClass().getResource("bad-defaults-range-trait.smithy"))
                .assemble()
                .unwrap();
        Model output = Model.assembler()
                .addImport(getClass().getResource("bad-defaults-range-trait.fixed.smithy"))
                .assemble()
                .unwrap();

        ModelTransformer transformer = ModelTransformer.create();

        Model result = transformer.removeInvalidDefaults(input);

        Node actual = ModelSerializer.builder().build().serialize(result);
        Node expected = ModelSerializer.builder().build().serialize(output);
        Node.assertEquals(actual, expected);
    }
}
