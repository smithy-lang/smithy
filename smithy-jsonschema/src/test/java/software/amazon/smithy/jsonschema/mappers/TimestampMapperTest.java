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

package software.amazon.smithy.jsonschema.mappers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.jsonschema.JsonSchemaConstants;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

public class TimestampMapperTest {
    @Test
    public void convertsDateTimeToStringAndDateTimeFormat() {
        var shape = TimestampShape.builder()
                .id("smithy.example#Timestamp")
                .addTrait(new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME))
                .build();
        var schema = new TimestampMapper().updateSchema(shape, Schema.builder(), Node.objectNode()).build();

        assertThat(schema.getType().get(), equalTo("string"));
        assertThat(schema.getFormat().get(), equalTo("date-time"));
    }

    @Test
    public void convertsHttpDateToString() {
        var shape = TimestampShape.builder()
                .id("smithy.example#Timestamp")
                .addTrait(new TimestampFormatTrait(TimestampFormatTrait.HTTP_DATE))
                .build();
        var schema = new TimestampMapper().updateSchema(shape, Schema.builder(), Node.objectNode()).build();

        assertThat(schema.getType().get(), equalTo("string"));
        assertTrue(schema.getFormat().isEmpty());
    }

    @Test
    public void convertsEpochSecondsToNumber() {
        var shape = TimestampShape.builder()
                .id("smithy.example#Timestamp")
                .addTrait(new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS))
                .build();
        var schema = new TimestampMapper().updateSchema(shape, Schema.builder(), Node.objectNode()).build();

        assertThat(schema.getType().get(), equalTo("number"));
        assertTrue(schema.getFormat().isEmpty());
    }

    @Test
    public void convertsEpochUnknownToNumber() {
        var shape = TimestampShape.builder()
                .id("smithy.example#Timestamp")
                .addTrait(new TimestampFormatTrait("epoch-millis"))
                .build();
        var schema = new TimestampMapper().updateSchema(shape, Schema.builder(), Node.objectNode()).build();

        assertThat(schema.getType().get(), equalTo("number"));
        assertTrue(schema.getFormat().isEmpty());
    }

    @Test
    public void supportsDefaultTimestampFormat() {
        var shape = TimestampShape.builder().id("smithy.example#Timestamp").build();
        var config = Node.objectNodeBuilder()
                .withMember(JsonSchemaConstants.SMITHY_DEFAULT_TIMESTAMP_FORMAT, TimestampFormatTrait.DATE_TIME)
                .build();
        var schema = new TimestampMapper().updateSchema(shape, Schema.builder(), config).build();

        assertThat(schema.getType().get(), equalTo("string"));
        assertThat(schema.getFormat().get(), equalTo("date-time"));
    }

    @Test
    public void assumesDateTimeStringWhenNoFormatOrDefaultPresent() {
        var shape = TimestampShape.builder()
                .id("smithy.example#Timestamp")
                .build();
        var schema = new TimestampMapper().updateSchema(shape, Schema.builder(), Node.objectNode()).build();

        assertThat(schema.getType().get(), equalTo("string"));
        assertThat(schema.getFormat().get(), equalTo("date-time"));
    }
}
