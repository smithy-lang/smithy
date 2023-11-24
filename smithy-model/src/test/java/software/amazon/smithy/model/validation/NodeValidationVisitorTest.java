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

package software.amazon.smithy.model.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.node.TimestampValidationStrategy;

public class NodeValidationVisitorTest {
    private static Model MODEL;

    @BeforeAll
    public static void onlyOnce() {
        MODEL = Model.assembler()
                .addImport(NodeValidationVisitorTest.class.getResource("node-validator.json"))
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        MODEL = null;
    }

    @ParameterizedTest
    @MethodSource("data")
    public void nodeValidationVisitorTest(String target, String value, String[] errors) {
        ShapeId targetId = ShapeId.from(target);
        Node nodeValue = Node.parse(value);
        NodeValidationVisitor visitor = NodeValidationVisitor.builder()
                .value(nodeValue)
                .model(MODEL)
                .build();
        List<ValidationEvent> events = MODEL.expectShape(targetId).accept(visitor);

        if (errors != null) {
            List<String> messages = events.stream().map(ValidationEvent::getMessage).collect(Collectors.toList());
            assertThat(messages, containsInAnyOrder(errors));
        } else if (!events.isEmpty()) {
            Assertions.fail("Did not expect any problems with the value, but found: "
                            + events.stream().map(Object::toString).collect(Collectors.joining("\n")));
        }
    }

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // Invalid shapes
                {"ns.foo#Service", "true", new String[] {"Encountered invalid shape type: service"}},
                {"ns.foo#Operation", "true", new String[] {"Encountered invalid shape type: operation"}},
                {"ns.foo#Resource", "true", new String[] {"Encountered invalid shape type: resource"}},

                // Booleans
                {"ns.foo#Boolean", "true", null},
                {"ns.foo#Boolean", "false", null},
                {"ns.foo#Boolean", "\"string\"", new String[] {
                        "Expected boolean value for boolean shape, `ns.foo#Boolean`; found string value, `string`"
                }},
                {"ns.foo#Boolean", "10", new String[] {
                        "Expected boolean value for boolean shape, `ns.foo#Boolean`; found number value, `10`"
                }},
                {"ns.foo#Boolean", "{}", new String[] {
                        "Expected boolean value for boolean shape, `ns.foo#Boolean`; found object value"
                }},
                {"ns.foo#Boolean", "[]", new String[] {
                        "Expected boolean value for boolean shape, `ns.foo#Boolean`; found array value"
                }},
                {"ns.foo#Boolean", "null", new String[] {
                        "Expected boolean value for boolean shape, `ns.foo#Boolean`; found null value"
                }},

                // Blobs
                {"ns.foo#Blob1", "\"\"", null},
                {"ns.foo#Blob1", "\"foo\"", null},
                {"ns.foo#Blob1", "true", new String[] {
                        "Expected string value for blob shape, `ns.foo#Blob1`; found boolean value, `true`"
                }},
                {"ns.foo#Blob2", "\"f\"", null},
                {"ns.foo#Blob2", "\"fooo\"", new String[] {"Value provided for `ns.foo#Blob2` must have no more than 3 bytes, but the provided value has 4 bytes"}},
                {"ns.foo#Blob2", "\"\"", new String[] {"Value provided for `ns.foo#Blob2` must have at least 1 bytes, but the provided value only has 0 bytes"}},

                // byte
                {"ns.foo#Byte", "10", null},
                {"ns.foo#Byte", "-256", new String[] {"byte value must be > -128, but found -256"}},
                {"ns.foo#Byte", "256", new String[] {"byte value must be < 127, but found 256"}},
                {"ns.foo#Byte", "true", new String[] {"Expected number value for byte shape, `ns.foo#Byte`; found boolean value, `true`"}},
                {"ns.foo#Byte", "21", new String[] {"Value provided for `ns.foo#Byte` must be less than or equal to 20, but found 21"}},
                {"ns.foo#Byte", "9", new String[] {"Value provided for `ns.foo#Byte` must be greater than or equal to 10, but found 9"}},
                {"ns.foo#Byte", "10.2", new String[] {"byte shapes must not have floating point values, but found `10.2` provided for `ns.foo#Byte`"}},

                // short
                {"ns.foo#Short", "10", null},
                {"ns.foo#Short", "-999999", new String[] {"short value must be > -32768, but found -999999"}},
                {"ns.foo#Short", "9999999", new String[] {"short value must be < 32767, but found 9999999"}},
                {"ns.foo#Short", "true", new String[] {"Expected number value for short shape, `ns.foo#Short`; found boolean value, `true`"}},
                {"ns.foo#Short", "21", new String[] {"Value provided for `ns.foo#Short` must be less than or equal to 20, but found 21"}},
                {"ns.foo#Short", "9", new String[] {"Value provided for `ns.foo#Short` must be greater than or equal to 10, but found 9"}},
                {"ns.foo#Short", "10.2", new String[] {"short shapes must not have floating point values, but found `10.2` provided for `ns.foo#Short`"}},

                // integer
                {"ns.foo#Integer", "10", null},
                {"ns.foo#Integer", "true", new String[] {"Expected number value for integer shape, `ns.foo#Integer`; found boolean value, `true`"}},
                {"ns.foo#Integer", "21", new String[] {"Value provided for `ns.foo#Integer` must be less than or equal to 20, but found 21"}},
                {"ns.foo#Integer", "9", new String[] {"Value provided for `ns.foo#Integer` must be greater than or equal to 10, but found 9"}},
                {"ns.foo#Integer", "10.2", new String[] {"integer shapes must not have floating point values, but found `10.2` provided for `ns.foo#Integer`"}},

                // long
                {"ns.foo#Long", "10", null},
                {"ns.foo#Long", "true", new String[] {"Expected number value for long shape, `ns.foo#Long`; found boolean value, `true`"}},
                {"ns.foo#Long", "21", new String[] {"Value provided for `ns.foo#Long` must be less than or equal to 20, but found 21"}},
                {"ns.foo#Long", "9", new String[] {"Value provided for `ns.foo#Long` must be greater than or equal to 10, but found 9"}},

                // float
                {"ns.foo#Float", "10", null},
                {"smithy.api#Float", "\"NaN\"", null},
                {"ns.foo#Float", "\"NaN\"", new String[] {"Value provided for `ns.foo#Float` must be a number because the `smithy.api#range` trait is applied, but found \"NaN\""}},
                {"smithy.api#Float", "\"Infinity\"", null},
                {"smithy.api#Float", "\"-Infinity\"", null},
                {"smithy.api#Float", "\"+Infinity\"", new String[] {"Value for `smithy.api#Float` must either be numeric or one of the following strings: [\"NaN\", \"Infinity\", \"-Infinity\"], but was \"+Infinity\""}},
                {"ns.foo#Float", "true", new String[] {"Expected number value for float shape, `ns.foo#Float`; found boolean value, `true`"}},
                {"ns.foo#Float", "21", new String[] {"Value provided for `ns.foo#Float` must be less than or equal to 20, but found 21"}},
                {"ns.foo#Float", "\"Infinity\"", new String[] {"Value provided for `ns.foo#Float` must be less than or equal to 20, but found \"Infinity\""}},
                {"ns.foo#Float", "9", new String[] {"Value provided for `ns.foo#Float` must be greater than or equal to 10, but found 9"}},
                {"ns.foo#Float", "\"-Infinity\"", new String[] {"Value provided for `ns.foo#Float` must be greater than or equal to 10, but found \"-Infinity\""}},

                // double
                {"ns.foo#Double", "10", null},
                {"smithy.api#Double", "\"NaN\"", null},
                {"ns.foo#Double", "\"NaN\"", new String[] {"Value provided for `ns.foo#Double` must be a number because the `smithy.api#range` trait is applied, but found \"NaN\""}},
                {"smithy.api#Double", "\"Infinity\"", null},
                {"smithy.api#Double", "\"-Infinity\"", null},
                {"smithy.api#Double", "\"+Infinity\"", new String[] {"Value for `smithy.api#Double` must either be numeric or one of the following strings: [\"NaN\", \"Infinity\", \"-Infinity\"], but was \"+Infinity\""}},
                {"ns.foo#Double", "true", new String[] {"Expected number value for double shape, `ns.foo#Double`; found boolean value, `true`"}},
                {"ns.foo#Double", "21", new String[] {"Value provided for `ns.foo#Double` must be less than or equal to 20, but found 21"}},
                {"ns.foo#Double", "\"Infinity\"", new String[] {"Value provided for `ns.foo#Double` must be less than or equal to 20, but found \"Infinity\""}},
                {"ns.foo#Double", "9", new String[] {"Value provided for `ns.foo#Double` must be greater than or equal to 10, but found 9"}},
                {"ns.foo#Double", "\"-Infinity\"", new String[] {"Value provided for `ns.foo#Double` must be greater than or equal to 10, but found \"-Infinity\""}},

                // bigInteger
                {"ns.foo#BigInteger", "10", null},
                {"ns.foo#BigInteger", "true", new String[] {"Expected number value for bigInteger shape, `ns.foo#BigInteger`; found boolean value, `true`"}},
                {"ns.foo#BigInteger", "21", new String[] {"Value provided for `ns.foo#BigInteger` must be less than or equal to 20, but found 21"}},
                {"ns.foo#BigInteger", "9", new String[] {"Value provided for `ns.foo#BigInteger` must be greater than or equal to 10, but found 9"}},

                // bigDecimal
                {"ns.foo#BigDecimal", "10", null},
                {"ns.foo#BigDecimal", "true", new String[] {"Expected number value for bigDecimal shape, `ns.foo#BigDecimal`; found boolean value, `true`"}},
                {"ns.foo#BigDecimal", "21", new String[] {"Value provided for `ns.foo#BigDecimal` must be less than or equal to 20, but found 21"}},
                {"ns.foo#BigDecimal", "9", new String[] {"Value provided for `ns.foo#BigDecimal` must be greater than or equal to 10, but found 9"}},

                // timestamp
                {"ns.foo#Timestamp", "\"1985-04-12T23:20:50.52Z\"", null},
                {"ns.foo#Timestamp", "1507837929", null},
                {"ns.foo#Timestamp", "1507837929.123", null},
                {"ns.foo#Timestamp", "true", new String[] {"Invalid boolean value provided for timestamp, `ns.foo#Timestamp`. Expected a number that contains epoch seconds with optional millisecond precision, or a string that contains an RFC 3339 formatted timestamp (e.g., \"1985-04-12T23:20:50.52Z\")"}},
                {"ns.foo#Timestamp", "\"2000-01-12T22:11:12\"", new String[] {"Invalid string value, `2000-01-12T22:11:12`, provided for timestamp, `ns.foo#Timestamp`. Expected an RFC 3339 formatted timestamp (e.g., \"1985-04-12T23:20:50.52Z\")"}},
                {"ns.foo#Timestamp", "\"2000-01-12T22:11:12+\"", new String[] {"Invalid string value, `2000-01-12T22:11:12+`, provided for timestamp, `ns.foo#Timestamp`. Expected an RFC 3339 formatted timestamp (e.g., \"1985-04-12T23:20:50.52Z\")"}},
                {"ns.foo#Timestamp", "\"200-01-12T22:11:12Z\"", new String[] {"Invalid string value, `200-01-12T22:11:12Z`, provided for timestamp, `ns.foo#Timestamp`. Expected an RFC 3339 formatted timestamp (e.g., \"1985-04-12T23:20:50.52Z\")"}},
                {"ns.foo#Timestamp", "\"2000-01-12T22:11:12+01:02\"", new String[] {"Invalid string value, `2000-01-12T22:11:12+01:02`, provided for timestamp, `ns.foo#Timestamp`. Expected an RFC 3339 formatted timestamp (e.g., \"1985-04-12T23:20:50.52Z\")"}},
                {"ns.foo#Timestamp", "\"2000-01-12T22:11:12-11:00\"", new String[] {"Invalid string value, `2000-01-12T22:11:12-11:00`, provided for timestamp, `ns.foo#Timestamp`. Expected an RFC 3339 formatted timestamp (e.g., \"1985-04-12T23:20:50.52Z\")"}},


                // string
                {"ns.foo#String1", "\"true\"", null},
                {"ns.foo#String2", "\"fooo\"", new String[] {"String value provided for `ns.foo#String2` must be <= 3 characters, but the provided value is 4 characters."}},
                {"ns.foo#String2", "\"\"", new String[] {"String value provided for `ns.foo#String2` must be >= 1 characters, but the provided value is only 0 characters."}},
                {"ns.foo#String2", "\"foo\"", null},
                {"ns.foo#String3", "\"qux\"", new String[] {"String value provided for `ns.foo#String3` must be one of the following values: `bar`, `foo`"}},
                {"ns.foo#String3", "\"foo\"", null},
                {"ns.foo#String3", "\"bar\"", null},
                {"ns.foo#String4", "\"ABC\"", null},
                {"ns.foo#String4", "\"abc\"", new String[] {"String value provided for `ns.foo#String4` must match regular expression: ^[A-Z]+$"}},

                // list
                {"ns.foo#List", "[\"a\"]", null},
                {"ns.foo#List", "[\"a\", \"b\"]", null},
                {"ns.foo#List", "[]", new String[] {"Value provided for `ns.foo#List` must have at least 1 elements, but the provided value only has 0 elements"}},
                {"ns.foo#List", "[\"a\", \"b\", \"c\"]", new String[] {"Value provided for `ns.foo#List` must have no more than 2 elements, but the provided value has 3 elements"}},
                {"ns.foo#List", "[10]", new String[] {"0: Expected string value for string shape, `ns.foo#String`; found number value, `10`"}},
                {"ns.foo#List", "10", new String[] {"Expected array value for list shape, `ns.foo#List`; found number value, `10`"}},

                // unique list
                {"ns.foo#UniqueList", "[\"a\"]", null},
                {"ns.foo#UniqueList", "[\"a\", \"b\"]", null},
                {"ns.foo#UniqueList", "[\"a\", \"a\"]", new String[] {"Value provided for `ns.foo#UniqueList` must have unique items, but the following items had multiple entries: [`a`]"}},
                {"ns.foo#UniqueList", "[\"a\", \"a\", \"a\"]", new String[] {"Value provided for `ns.foo#UniqueList` must have unique items, but the following items had multiple entries: [`a`]"}},
                {"ns.foo#UniqueList", "[\"a\", \"a\", \"b\", \"b\"]", new String[] {"Value provided for `ns.foo#UniqueList` must have unique items, but the following items had multiple entries: [`a`, `b`]"}},

                // map
                {"ns.foo#Map", "{\"a\":[\"b\"]}", null},
                {"ns.foo#Map", "{\"a\":[\"b\"], \"c\":[\"d\"]}", null},
                // Too many elements
                {"ns.foo#Map", "{\"a\":[\"b\"], \"c\":[\"d\"], \"e\":[\"f\"]}", new String[] {"Value provided for `ns.foo#Map` must have no more than 2 entries, but the provided value has 3 entries"}},
                // Not enough elements
                {"ns.foo#Map", "{}", new String[] {"Value provided for `ns.foo#Map` must have at least 1 entries, but the provided value only has 0 entries"}},
                // Too many characters in string.
                {"ns.foo#Map", "{\"abc\":[\"b\"], \"c\":[\"d\"]}", new String[] {"abc (map-key): String value provided for `ns.foo#KeyString` must be <= 2 characters, but the provided value is 3 characters."}},
                // Too many elements in nested list
                {"ns.foo#Map", "{\"a\":[\"b\", \"c\", \"d\", \"e\"]}", new String[] {"a: Value provided for `ns.foo#List` must have no more than 2 elements, but the provided value has 4 elements"}},

                // structure
                {"ns.foo#Structure", "{\"foo\": \"test\"}", null},
                {"ns.foo#Structure", "{\"foo\": \"test\", \"invalid\": true}", new String[] {"Invalid structure member `invalid` found for `ns.foo#Structure`"}},
                {"ns.foo#Structure", "{\"foo\": \"test\", \"baz\": \"baz\"}", null},
                {"ns.foo#Structure", "{\"foo\": \"test\", \"baz\": \"baz\", \"bar\": [\"a\", \"b\"], \"bam\": {\"foo\": \"test\"}}", null},
                {"ns.foo#Structure", "{\"baz\": \"test\"}", new String[] {"Missing required structure member `foo` for `ns.foo#Structure`"}},
                {"ns.foo#Structure", "{\"foo\": 10}", new String[] {"foo: Expected string value for string shape, `ns.foo#String`; found number value, `10`"}},
                {"ns.foo#Structure", "{\"foo\": \"test\", \"baz\": 10}", new String[] {"baz: Expected string value for string shape, `ns.foo#String`; found number value, `10`"}},
                {"ns.foo#Structure", "{\"foo\": \"test\", \"bam\": {}}", new String[] {"bam: Missing required structure member `foo` for `ns.foo#Structure`"}},
                {"ns.foo#Structure", "{\"foo\": \"test\", \"bam\": {\"foo\": 10}}", new String[] {"bam.foo: Expected string value for string shape, `ns.foo#String`; found number value, `10`"}},

                // taggged union
                {"ns.foo#TaggedUnion", "{\"foo\": \"test\"}", null},
                {"ns.foo#TaggedUnion", "{\"baz\": \"test\"}", null},
                {"ns.foo#TaggedUnion", "{\"foo\": \"test\", \"baz\": \"baz\"}", new String[] {"union values can contain a value for only a single member"}},
                {"ns.foo#TaggedUnion", "{\"foo\": 10}", new String[] {"foo: Expected string value for string shape, `ns.foo#String`; found number value, `10`"}},
                {"ns.foo#TaggedUnion", "{\"invalid\": true}", new String[] {"Invalid union member `invalid` found for `ns.foo#TaggedUnion`"}},

                // http-date
                {"ns.foo#HttpDate", "\"Tue, 29 Apr 2014 18:30:38 GMT\"", null},
                {"ns.foo#HttpDate", "\"Tuesday, 29 April 2014 18:30:38 GMT\"", new String[] {"Invalid value provided for http-date formatted timestamp. Expected a string value that matches the IMF-fixdate production of RFC 7231 section-7.1.1.1. Found: Tuesday, 29 April 2014 18:30:38 GMT"}},
                {"ns.foo#HttpDate", "\"Tue, 29 Apr 2014 18:30:38 PST\"", new String[] {"Invalid value provided for http-date formatted timestamp. Expected a string value that matches the IMF-fixdate production of RFC 7231 section-7.1.1.1. Found: Tue, 29 Apr 2014 18:30:38 PST"}},
                {"ns.foo#HttpDate", "11", new String[] {"Invalid value provided for http-date formatted timestamp. Expected a string value that matches the IMF-fixdate production of RFC 7231 section-7.1.1.1. Found: number"}},

                // date-time
                {"ns.foo#DateTime", "\"1985-04-12T23:20:50.52Z\"", null},
                {"ns.foo#DateTime", "1234", new String[] {"Expected a string value for a date-time timestamp (e.g., \"1985-04-12T23:20:50.52Z\")"}},
                {"ns.foo#DateTime", "\"1985-04-12\"", new String[] {"Invalid string value, `1985-04-12`, provided for timestamp, `ns.foo#DateTime`. Expected an RFC 3339 formatted timestamp (e.g., \"1985-04-12T23:20:50.52Z\")"}},
                {"ns.foo#DateTime", "\"Tuesday, 29 April 2014 18:30:38 GMT\"", new String[] {"Invalid string value, `Tuesday, 29 April 2014 18:30:38 GMT`, provided for timestamp, `ns.foo#DateTime`. Expected an RFC 3339 formatted timestamp (e.g., \"1985-04-12T23:20:50.52Z\")"}},
                {"ns.foo#DateTime", "\"1985-04-12T23:20:50.52-07:00\"", new String[] {"Invalid string value, `1985-04-12T23:20:50.52-07:00`, provided for timestamp, `ns.foo#DateTime`. Expected an RFC 3339 formatted timestamp (e.g., \"1985-04-12T23:20:50.52Z\")"}},

                // epoch seconds
                {"ns.foo#EpochSeconds", "123", null},
                {"ns.foo#EpochSeconds", "\"1985-04-12T23:20:50.52Z\"", new String[] {"Invalid string value provided for a timestamp with a `epoch-seconds` format."}},

                // timestamp member with format.
                {"ns.foo#TimestampList", "[\"1985-04-12T23:20:50.52Z\"]", null},
                {"ns.foo#TimestampList", "[\"1985-04-12T23:20:50.52-07:00\"]", new String[] {
                        "0: Invalid string value, `1985-04-12T23:20:50.52-07:00`, provided for timestamp, `ns.foo#TimestampList$member`. Expected an RFC 3339 formatted timestamp (e.g., \"1985-04-12T23:20:50.52Z\")"
                }},
                {"ns.foo#TimestampList", "[123]", new String[] {"0: Expected a string value for a date-time timestamp (e.g., \"1985-04-12T23:20:50.52Z\")"}},
                {"ns.foo#Structure4", "{\"httpDate\": 1234}", new String[] {"httpDate: Invalid value provided for http-date formatted timestamp. Expected a string value that matches the IMF-fixdate production of RFC 7231 section-7.1.1.1. Found: number"}},
                {"ns.foo#Structure4", "{\"httpDateTarget\": 1234}", new String[] {"httpDateTarget: Invalid value provided for http-date formatted timestamp. Expected a string value that matches the IMF-fixdate production of RFC 7231 section-7.1.1.1. Found: number"}},

                // timestamp member with no format.
                {"ns.foo#TimestampListNoFormatTrait", "[123]", null},

                // Member validation
                {"ns.foo#Structure2", "{\"a\": \"23 abc\"}", null},
                {"ns.foo#Structure2", "{\"a\": \"abc\"}", new String[] {"a: String value provided for `ns.foo#Structure2$a` must match regular expression: ^[0-9]"}},
                {"ns.foo#Structure2", "{\"b\": \"12345678910\"}", null},
                {"ns.foo#Structure2", "{\"b\": \"123\"}", new String[] {"b: String value provided for `ns.foo#Structure2$b` must be >= 10 characters, but the provided value is only 3 characters."}},
                {"ns.foo#Structure2", "{\"c\": 11}", null},
                {"ns.foo#Structure2", "{\"c\": 5}", new String[] {"c: Value provided for `ns.foo#Structure2$c` must be greater than or equal to 10, but found 5"}}
        });
    }

    @Test
    public void canSuccessfullyValidateTimestampsAsUnixTimestamps() {
        NodeValidationVisitor visitor = NodeValidationVisitor.builder()
                .value(Node.from(1234))
                .model(MODEL)
                .timestampValidationStrategy(TimestampValidationStrategy.EPOCH_SECONDS)
                .build();
        List<ValidationEvent> events = MODEL
                .expectShape(ShapeId.from("ns.foo#TimestampList$member"))
                .accept(visitor);

        assertThat(events, empty());
    }

    @Test
    public void canUnsuccessfullyValidateTimestampsAsUnixTimestamps() {
        NodeValidationVisitor visitor = NodeValidationVisitor.builder()
                .value(Node.from("foo"))
                .model(MODEL)
                .timestampValidationStrategy(TimestampValidationStrategy.EPOCH_SECONDS)
                .build();
        List<ValidationEvent> events = MODEL
                .expectShape(ShapeId.from("ns.foo#TimestampList$member"))
                .accept(visitor);

        assertThat(events, not(empty()));
    }

    @Test
    public void doesNotAllowNullByDefault() {
        NodeValidationVisitor visitor = NodeValidationVisitor.builder()
                .value(Node.nullNode())
                .model(MODEL)
                .build();
        List<ValidationEvent> events = MODEL
                .expectShape(ShapeId.from("smithy.api#String"))
                .accept(visitor);

        assertThat(events, not(empty()));
    }

    @Test
    public void canConfigureToSupportNull() {
        NodeValidationVisitor visitor = NodeValidationVisitor.builder()
                .value(Node.nullNode())
                .model(MODEL)
                .allowOptionalNull(true)
                .build();
        List<ValidationEvent> events = MODEL
                .expectShape(ShapeId.from("smithy.api#String"))
                .accept(visitor);

        assertThat(events, empty());
    }
}
