/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

public class TimestampMapperTest {
    @Test
    public void convertsDateTimeToStringAndDateTimeFormat() {
        TimestampShape shape = TimestampShape.builder()
                .id("smithy.example#Timestamp")
                .addTrait(new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME))
                .build();
        Schema schema = new TimestampMapper().updateSchema(shape, Schema.builder(), new JsonSchemaConfig()).build();

        assertThat(schema.getType().get(), equalTo("string"));
        assertThat(schema.getFormat().get(), equalTo("date-time"));
    }

    @Test
    public void convertsHttpDateToString() {
        TimestampShape shape = TimestampShape.builder()
                .id("smithy.example#Timestamp")
                .addTrait(new TimestampFormatTrait(TimestampFormatTrait.HTTP_DATE))
                .build();
        Schema schema = new TimestampMapper().updateSchema(shape, Schema.builder(), new JsonSchemaConfig()).build();

        assertThat(schema.getType().get(), equalTo("string"));
        assertFalse(schema.getFormat().isPresent());
    }

    @Test
    public void convertsEpochSecondsToNumber() {
        TimestampShape shape = TimestampShape.builder()
                .id("smithy.example#Timestamp")
                .addTrait(new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS))
                .build();
        Schema schema = new TimestampMapper().updateSchema(shape, Schema.builder(), new JsonSchemaConfig()).build();

        assertThat(schema.getType().get(), equalTo("number"));
        assertFalse(schema.getFormat().isPresent());
    }

    @Test
    public void convertsEpochUnknownToNumber() {
        TimestampShape shape = TimestampShape.builder()
                .id("smithy.example#Timestamp")
                .addTrait(new TimestampFormatTrait("epoch-millis"))
                .build();
        Schema schema = new TimestampMapper().updateSchema(shape, Schema.builder(), new JsonSchemaConfig()).build();

        assertThat(schema.getType().get(), equalTo("number"));
        assertFalse(schema.getFormat().isPresent());
    }

    @Test
    public void supportsDefaultTimestampFormat() {
        TimestampShape shape = TimestampShape.builder().id("smithy.example#Timestamp").build();
        JsonSchemaConfig config = new JsonSchemaConfig();
        config.setDefaultTimestampFormat(TimestampFormatTrait.Format.DATE_TIME);
        Schema schema = new TimestampMapper().updateSchema(shape, Schema.builder(), config).build();

        assertThat(schema.getType().get(), equalTo("string"));
        assertThat(schema.getFormat().get(), equalTo("date-time"));
    }

    @Test
    public void assumesDateTimeStringWhenNoFormatOrDefaultPresent() {
        TimestampShape shape = TimestampShape.builder()
                .id("smithy.example#Timestamp")
                .build();
        Schema schema = new TimestampMapper().updateSchema(shape, Schema.builder(), new JsonSchemaConfig()).build();

        assertThat(schema.getType().get(), equalTo("string"));
        assertThat(schema.getFormat().get(), equalTo("date-time"));
    }
}
