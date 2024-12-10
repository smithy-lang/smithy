/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.utils.SetUtils;

public class DisableMapperTest {
    @Test
    public void removesDisabledKeywords() {
        StringShape shape = StringShape.builder().id("smithy.example#String").build();
        Schema.Builder builder = Schema.builder().type("string").format("foo");
        JsonSchemaConfig config = new JsonSchemaConfig();
        config.setDisableFeatures(SetUtils.of("format"));
        Schema schema = new DisableMapper().updateSchema(shape, builder, config).build();

        assertThat(schema.getType().get(), equalTo("string"));
        assertFalse(schema.getFormat().isPresent());
    }
}
