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

package software.amazon.smithy.jsonschema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;

public class SchemaTest {
    /**
     * This is a basic integration test that makes sure each disable setting
     * can be called and doesn't break the builder.
     */
    @Test
    public void canRemoveSettings() {
        Schema.Builder builder = Schema.builder();
        Set<String> values = SetUtils.of(
                "const",
                "default",
                "enum",
                "multipleOf",
                "maximum",
                "exclusiveMaximum",
                "minimum",
                "exclusiveMinimum",
                "maxLength",
                "minLength",
                "pattern",
                "items",
                "maxItems",
                "minItems",
                "uniqueItems",
                "properties",
                "additionalProperties",
                "required",
                "maxProperties",
                "minProperties",
                "propertyNames",
                "allOf",
                "anyOf",
                "oneOf",
                "not",
                "title",
                "description",
                "format",
                "readOnly",
                "writeOnly",
                "comment",
                "contentEncoding",
                "contentMediaType",
                "examples"
        );

        for (String value : values) {
            builder.disableProperty(value);
        }

        builder.build();
    }

    @Test
    public void basicEqualityTest() {
        Schema a = Schema.builder().title("foo").build();
        Schema b = Schema.builder().title("foo").build();
        Schema c = Schema.builder().type("string").build();

        assertThat(a, equalTo(b));
        assertThat(a, equalTo(a));
        assertThat(b, equalTo(a));
        assertThat(c, not(equalTo(a)));
    }

    @Test
    public void getsAllOfSelector() {
        Schema subschema = Schema.builder().type("string").build();
        Schema schema = Schema.builder().allOf(Collections.singletonList(subschema)).build();

        assertThat(schema.selectSchema("allOf", "0"), equalTo(Optional.of(subschema)));
        assertThat(schema.selectSchema("allOf"), equalTo(Optional.empty()));
    }

    @Test
    public void getsOneOfSelector() {
        Schema subschema = Schema.builder().type("string").build();
        Schema schema = Schema.builder().oneOf(Collections.singletonList(subschema)).build();

        assertThat(schema.selectSchema("oneOf", "0"), equalTo(Optional.of(subschema)));
        assertThat(schema.selectSchema("oneOf"), equalTo(Optional.empty()));
    }

    @Test
    public void getsAnyOfSelector() {
        Schema subschema = Schema.builder().type("string").build();
        Schema schema = Schema.builder().anyOf(Collections.singletonList(subschema)).build();

        assertThat(schema.selectSchema("anyOf", "0"), equalTo(Optional.of(subschema)));
        assertThat(schema.selectSchema("anyOf"), equalTo(Optional.empty()));
    }

    @Test
    public void throwsWhenInvalidPositionGiven() {
        Schema subschema = Schema.builder().type("string").build();
        Schema schema = Schema.builder().anyOf(Collections.singletonList(subschema)).build();

        Assertions.assertThrows(SmithyJsonSchemaException.class, () -> {
            schema.selectSchema("anyOf", "foo");
        });
    }

    @Test
    public void getsPropertyNamesSelector() {
        Schema propertyNames = Schema.builder().type("string").build();
        Schema schema = Schema.builder().propertyNames(propertyNames).build();

        assertThat(schema.selectSchema("propertyNames"), equalTo(Optional.of(propertyNames)));
    }

    @Test
    public void getsItemsSelector() {
        Schema items = Schema.builder().type("string").build();
        Schema schema = Schema.builder().items(items).build();

        assertThat(schema.selectSchema("items"), equalTo(Optional.of(items)));
    }

    @Test
    public void doesNotThrowOnInvalidPropertySelector() {
        Schema schema = Schema.builder().putProperty("foo", Schema.builder().type("string").build()).build();

        assertThat(schema.selectSchema("properties"), equalTo(Optional.empty()));
    }

    @Test
    public void getsAdditionalPropertiesSelector() {
        Schema items = Schema.builder().type("string").build();
        Schema schema = Schema.builder().not(items).build();

        assertThat(schema.selectSchema("not"), equalTo(Optional.of(items)));
    }

    @Test
    public void getsNotSelector() {
        Schema items = Schema.builder().type("string").build();
        Schema schema = Schema.builder().additionalProperties(items).build();

        assertThat(schema.selectSchema("additionalProperties"), equalTo(Optional.of(items)));
    }

    @Test
    public void ignoresUnsupportedProperties() {
        Schema schema = Schema.builder().build();

        assertThat(schema.selectSchema("foof", "doof"), equalTo(Optional.empty()));
    }

    @Test
    public void ignoresNegativeNumericPositions() {
        Schema subschema = Schema.builder().type("string").build();
        Schema schema = Schema.builder().allOf(Collections.singletonList(subschema)).build();

        assertThat(schema.selectSchema("allOf", "-1"), equalTo(Optional.empty()));
    }

    @Test
    public void maintainsPropertyOrder() {
        Schema schema = Schema.builder()
                .putProperty("foo", Schema.builder().build())
                .putProperty("bar", Schema.builder().build())
                .build();

        assertThat(schema.getProperties().keySet(), contains("foo", "bar"));
    }

    @Test
    public void removingPropertiesRemovesRequiredPropertiesToo() {
        Schema schema = Schema.builder()
                .removeProperty("notThere")
                .required(null)
                .putProperty("foo", Schema.builder().build())
                .putProperty("bar", Schema.builder().build())
                .required(ListUtils.of("foo", "bar"))
                .removeProperty("foo")
                .build();

        assertThat(schema.getProperties().keySet(), contains("bar"));
        assertThat(schema.getRequired(), contains("bar"));
    }

    @Test
    public void mergesEnumValuesWhenConvertingToNode() {
        Schema schema = Schema.builder()
                .enumValues(ListUtils.of("foo", "bar"))
                .intEnumValues(ListUtils.of(1, 2))
                .build();
        ArrayNode node = schema.toNode().asObjectNode().get().expectArrayMember("enum");
        assertThat(node.getElements(), containsInAnyOrder(
                Node.from("foo"),
                Node.from("bar"),
                Node.from(1),
                Node.from(2)));
    }
}
