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
