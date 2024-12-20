/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.example.traits.StringTrait;
import com.example.traits.defaults.StructDefaultsTrait;
import com.example.traits.documents.DocumentTrait;
import com.example.traits.documents.StructWithNestedDocumentTrait;
import com.example.traits.enums.EnumListMemberTrait;
import com.example.traits.enums.IntEnumTrait;
import com.example.traits.enums.SomeEnum;
import com.example.traits.enums.StringEnumTrait;
import com.example.traits.enums.SuitTrait;
import com.example.traits.idref.IdRefListTrait;
import com.example.traits.idref.IdRefMapTrait;
import com.example.traits.idref.IdRefStringTrait;
import com.example.traits.idref.IdRefStructTrait;
import com.example.traits.idref.IdRefStructWithNestedIdsTrait;
import com.example.traits.idref.NestedIdRefHolder;
import com.example.traits.lists.DocumentListTrait;
import com.example.traits.lists.ListMember;
import com.example.traits.lists.NumberListTrait;
import com.example.traits.lists.StructureListTrait;
import com.example.traits.maps.MapValue;
import com.example.traits.maps.StringDocumentMapTrait;
import com.example.traits.maps.StringStringMapTrait;
import com.example.traits.maps.StringToStructMapTrait;
import com.example.traits.mixins.ListMemberWithMixin;
import com.example.traits.mixins.StructWithMixinTrait;
import com.example.traits.mixins.StructureListWithMixinMemberTrait;
import com.example.traits.names.SnakeCaseStructureTrait;
import com.example.traits.numbers.BigDecimalTrait;
import com.example.traits.numbers.BigIntegerTrait;
import com.example.traits.numbers.ByteTrait;
import com.example.traits.numbers.DoubleTrait;
import com.example.traits.numbers.FloatTrait;
import com.example.traits.numbers.IntegerTrait;
import com.example.traits.numbers.LongTrait;
import com.example.traits.numbers.ShortTrait;
import com.example.traits.structures.BasicAnnotationTrait;
import com.example.traits.structures.NestedA;
import com.example.traits.structures.NestedB;
import com.example.traits.structures.StructureTrait;
import com.example.traits.timestamps.DateTimeTimestampTrait;
import com.example.traits.timestamps.EpochSecondsTimestampTrait;
import com.example.traits.timestamps.HttpDateTimestampTrait;
import com.example.traits.timestamps.StructWithNestedTimestampsTrait;
import com.example.traits.timestamps.TimestampTrait;
import com.example.traits.uniqueitems.NumberSetTrait;
import com.example.traits.uniqueitems.SetMember;
import com.example.traits.uniqueitems.StringSetTrait;
import com.example.traits.uniqueitems.StructureSetTrait;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringListTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SetUtils;

public class LoadsFromModelTest {
    private static final ShapeId ID = ShapeId.from("test.smithy.traitcodegen#myStruct");
    private static final ShapeId TARGET_ONE = ShapeId.from("test.smithy.traitcodegen#IdRefTarget1");
    private static final ShapeId TARGET_TWO = ShapeId.from("test.smithy.traitcodegen#IdRefTarget2");

    static Stream<Arguments> loadsModelTests() {
        return Stream.of(
                // Document types
                Arguments.of("documents/document-trait.smithy",
                        DocumentTrait.class,
                        MapUtils.of("getValue",
                                Node.objectNodeBuilder()
                                        .withMember("metadata", "woo")
                                        .withMember("more", "yay")
                                        .build())),
                Arguments.of("documents/struct-with-nested-document.smithy",
                        StructWithNestedDocumentTrait.class,
                        MapUtils.of("getDoc",
                                Optional.of(ObjectNode.builder()
                                        .withMember("foo", "bar")
                                        .withMember("fizz", "buzz")
                                        .build()))),
                // Enums
                Arguments.of("enums/enum-trait.smithy",
                        StringEnumTrait.class,
                        MapUtils.of("getValue", "yes", "getEnumValue", StringEnumTrait.StringEnum.YES)),
                Arguments.of("enums/int-enum-trait.smithy",
                        IntEnumTrait.class,
                        MapUtils.of("getValue", 1, "getEnumValue", IntEnumTrait.IntEnum.YES)),
                Arguments.of("enums/string-enum-compatibility.smithy",
                        SuitTrait.class,
                        MapUtils.of("getEnumValue", SuitTrait.Suit.CLUB, "getValue", "club")),
                Arguments.of("enums/enum-list-member-trait.smithy",
                        EnumListMemberTrait.class,
                        MapUtils.of("getValue",
                                Optional.of(ListUtils.of(SomeEnum.SOME, SomeEnum.NONE, SomeEnum.SOME)))),
                // Id Refs
                Arguments.of("idref/idref-string.smithy",
                        IdRefStringTrait.class,
                        MapUtils.of("getValue", TARGET_ONE)),
                Arguments.of("idref/idref-list.smithy",
                        IdRefListTrait.class,
                        MapUtils.of("getValues", ListUtils.of(TARGET_ONE, TARGET_TWO))),
                Arguments.of("idref/idref-map.smithy",
                        IdRefMapTrait.class,
                        MapUtils.of("getValues", MapUtils.of("a", TARGET_ONE, "b", TARGET_TWO))),
                Arguments.of("idref/idref-struct.smithy",
                        IdRefStructTrait.class,
                        MapUtils.of("getFieldA", Optional.of(TARGET_ONE))),
                Arguments.of("idref/idref-struct-with-nested-refs.smithy",
                        IdRefStructWithNestedIdsTrait.class,
                        MapUtils.of("getIdRefHolder",
                                NestedIdRefHolder.builder().id(TARGET_ONE).build(),
                                "getIdList",
                                Optional.of(ListUtils.of(TARGET_ONE, TARGET_TWO)),
                                "getIdMap",
                                Optional.of(MapUtils.of("a", TARGET_ONE, "b", TARGET_TWO)))),
                // Lists
                Arguments.of("lists/number-list-trait.smithy",
                        NumberListTrait.class,
                        MapUtils.of("getValues", ListUtils.of(1, 2, 3, 4, 5))),
                Arguments.of("lists/string-list-trait.smithy",
                        StringListTrait.class,
                        MapUtils.of("getValues", ListUtils.of("a", "b", "c", "d"))),
                Arguments.of("lists/struct-list-trait.smithy",
                        StructureListTrait.class,
                        MapUtils.of("getValues",
                                ListUtils.of(
                                        ListMember.builder().a("first").b(1).c("other").build(),
                                        ListMember.builder().a("second").b(2).c("more").build()))),
                Arguments.of("lists/document-list-trait.smithy",
                        DocumentListTrait.class,
                        MapUtils.of("getValues",
                                ListUtils.of(
                                        ObjectNode.builder().withMember("a", "a").build().toNode(),
                                        ObjectNode.builder().withMember("b", "b").withMember("c", 1).build().toNode(),
                                        Node.from("string")))),
                // Maps
                Arguments.of("maps/string-string-map-trait.smithy",
                        StringStringMapTrait.class,
                        MapUtils.of("getValues",
                                MapUtils.of("a",
                                        "stuff",
                                        "b",
                                        "other",
                                        "c",
                                        "more!"))),
                Arguments.of("maps/string-to-struct-map-trait.smithy",
                        StringToStructMapTrait.class,
                        MapUtils.of("getValues",
                                MapUtils.of(
                                        "one",
                                        MapValue.builder().a("foo").b(2).build(),
                                        "two",
                                        MapValue.builder().a("bar").b(4).build()))),
                Arguments.of("maps/string-to-document-map-trait.smithy",
                        StringDocumentMapTrait.class,
                        MapUtils.of("getValues",
                                MapUtils.of(
                                        "a",
                                        ObjectNode.builder().withMember("a", "a").build().toNode(),
                                        "b",
                                        ObjectNode.builder().withMember("b", "b").withMember("c", 1).build().toNode(),
                                        "c",
                                        Node.from("stuff"),
                                        "d",
                                        Node.from(1)))),
                // Mixins
                Arguments.of("mixins/struct-with-mixin-member.smithy",
                        StructureListWithMixinMemberTrait.class,
                        MapUtils.of("getValues",
                                ListUtils.of(
                                        ListMemberWithMixin.builder()
                                                .a("first")
                                                .b(1)
                                                .c("other")
                                                .d("mixed-in")
                                                .build(),
                                        ListMemberWithMixin.builder()
                                                .a("second")
                                                .b(2)
                                                .c("more")
                                                .d("mixins are cool")
                                                .build()))),
                Arguments.of("mixins/struct-with-only-mixin-member.smithy",
                        StructWithMixinTrait.class,
                        MapUtils.of("getD", "mixed-in")),
                // Naming conflicts
                Arguments.of("names/snake-case-struct.smithy",
                        SnakeCaseStructureTrait.class,
                        MapUtils.of("getSnakeCaseMember", Optional.of("stuff"))),
                // Numbers
                Arguments.of("numbers/big-decimal-trait.smithy",
                        BigDecimalTrait.class,
                        MapUtils.of("getValue", new BigDecimal("100.01"))),
                Arguments.of("numbers/big-integer-trait.smithy",
                        BigIntegerTrait.class,
                        MapUtils.of("getValue", new BigInteger("100"))),
                Arguments.of("numbers/byte-trait.smithy",
                        ByteTrait.class,
                        MapUtils.of("getValue", (byte) 1)),
                Arguments.of("numbers/double-trait.smithy",
                        DoubleTrait.class,
                        MapUtils.of("getValue", 100.01)),
                Arguments.of("numbers/float-trait.smithy",
                        FloatTrait.class,
                        MapUtils.of("getValue", 1.1F)),
                Arguments.of("numbers/integer-trait.smithy",
                        IntegerTrait.class,
                        MapUtils.of("getValue", 1)),
                Arguments.of("numbers/long-trait.smithy",
                        LongTrait.class,
                        MapUtils.of("getValue", 1L)),
                Arguments.of("numbers/short-trait.smithy",
                        ShortTrait.class,
                        MapUtils.of("getValue", (short) 1)),
                // Structures
                Arguments.of("structures/annotation-trait.smithy",
                        BasicAnnotationTrait.class,
                        Collections.emptyMap()),
                Arguments.of("structures/struct-trait.smithy",
                        StructureTrait.class,
                        MapUtils.of(
                                "getFieldA",
                                "first",
                                "getFieldB",
                                Optional.of(false),
                                "getFieldC",
                                Optional.of(NestedA.builder()
                                        .fieldN("nested")
                                        .fieldQ(true)
                                        .fieldZ(NestedB.A)
                                        .build()),
                                "getFieldD",
                                Optional.of(ListUtils.of("a", "b", "c")),
                                "getFieldDOrEmpty",
                                ListUtils.of("a", "b", "c"),
                                "getFieldE",
                                Optional.of(MapUtils.of("a", "one", "b", "two")),
                                "getFieldEOrEmpty",
                                MapUtils.of("a", "one", "b", "two"),
                                "getFieldF",
                                Optional.of(new BigDecimal("100.01")),
                                "getFieldG",
                                Optional.of(new BigInteger("100")))),
                Arguments.of("structures/struct-with-non-existent-collections.smithy",
                        StructureTrait.class,
                        MapUtils.of(
                                "getFieldA",
                                "first",
                                "getFieldB",
                                Optional.of(false),
                                "getFieldC",
                                Optional.of(NestedA.builder()
                                        .fieldN("nested")
                                        .fieldQ(true)
                                        .fieldZ(NestedB.A)
                                        .build()),
                                "getFieldD",
                                Optional.empty()),
                        "getFieldDOrEmpty",
                        null,
                        "getFieldE",
                        Optional.empty(),
                        "getFieldEOrEmpty",
                        null),
                // Timestamps
                Arguments.of("timestamps/struct-with-nested-timestamps.smithy",
                        StructWithNestedTimestampsTrait.class,
                        MapUtils.of("getBaseTime",
                                Instant.parse("1985-04-12T23:20:50.52Z"),
                                "getDateTime",
                                Instant.parse("1985-04-12T23:20:50.52Z"),
                                "getHttpDate",
                                Instant.from(
                                        DateTimeFormatter.RFC_1123_DATE_TIME.parse("Tue, 29 Apr 2014 18:30:38 GMT")),
                                "getEpochSeconds",
                                Instant.ofEpochSecond((long) 1515531081.123))),
                Arguments.of("timestamps/timestamp-trait-date-time.smithy",
                        TimestampTrait.class,
                        MapUtils.of("getValue", Instant.parse("1985-04-12T23:20:50.52Z"))),
                Arguments.of("timestamps/timestamp-trait-epoch-sec.smithy",
                        TimestampTrait.class,
                        MapUtils.of("getValue", Instant.ofEpochSecond((long) 1515531081.123))),
                Arguments.of("timestamps/date-time-format-timestamp-trait.smithy",
                        DateTimeTimestampTrait.class,
                        MapUtils.of("getValue", Instant.parse("1985-04-12T23:20:50.52Z"))),
                Arguments.of("timestamps/http-date-format-timestamp-trait.smithy",
                        HttpDateTimestampTrait.class,
                        MapUtils.of("getValue",
                                Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME
                                        .parse("Tue, 29 Apr 2014 18:30:38 GMT")))),
                Arguments.of("timestamps/epoch-seconds-format-timestamp-trait.smithy",
                        EpochSecondsTimestampTrait.class,
                        MapUtils.of("getValue", Instant.ofEpochSecond((long) 1515531081.123))),
                // Uniques items (sets)
                Arguments.of("uniqueitems/number-set-trait.smithy",
                        NumberSetTrait.class,
                        MapUtils.of("getValues", SetUtils.of(1, 2, 3, 4))),
                Arguments.of("uniqueitems/string-set-trait.smithy",
                        StringSetTrait.class,
                        MapUtils.of("getValues", SetUtils.of("a", "b", "c", "d"))),
                Arguments.of("uniqueitems/struct-set-trait.smithy",
                        StructureSetTrait.class,
                        MapUtils.of("getValues",
                                ListUtils.of(
                                        SetMember.builder().a("first").b(1).c("other").build(),
                                        SetMember.builder().a("second").b(2).c("more").build()))),
                // Strings
                Arguments.of("string-trait.smithy",
                        StringTrait.class,
                        MapUtils.of("getValue", "Testing String Trait")),
                // Defaults
                Arguments.of("defaults/defaults.smithy",
                        StructDefaultsTrait.class,
                        MapUtils.of("getDefaultList", ListUtils.of())),
                Arguments.of("defaults/defaults.smithy",
                        StructDefaultsTrait.class,
                        MapUtils.of("getDefaultMap", MapUtils.of())),
                Arguments.of("defaults/defaults.smithy",
                        StructDefaultsTrait.class,
                        MapUtils.of("getDefaultBoolean", true)),
                Arguments.of("defaults/defaults.smithy",
                        StructDefaultsTrait.class,
                        MapUtils.of("getDefaultString", "default")),
                Arguments.of("defaults/defaults.smithy",
                        StructDefaultsTrait.class,
                        MapUtils.of("getDefaultByte", (byte) 1)),
                Arguments.of("defaults/defaults.smithy",
                        StructDefaultsTrait.class,
                        MapUtils.of("getDefaultShort", (short) 1)),
                Arguments.of("defaults/defaults.smithy",
                        StructDefaultsTrait.class,
                        MapUtils.of("getDefaultInt", 1)),
                Arguments.of("defaults/defaults.smithy",
                        StructDefaultsTrait.class,
                        MapUtils.of("getDefaultLong", 1L)),
                Arguments.of("defaults/defaults.smithy",
                        StructDefaultsTrait.class,
                        MapUtils.of("getDefaultFloat", 2.2F)),
                Arguments.of("defaults/defaults.smithy",
                        StructDefaultsTrait.class,
                        MapUtils.of("getDefaultDouble", 1.1)),
                Arguments.of("defaults/defaults.smithy",
                        StructDefaultsTrait.class,
                        MapUtils.of("getDefaultBigInt", new BigInteger("100"))),
                Arguments.of("defaults/defaults.smithy",
                        StructDefaultsTrait.class,
                        MapUtils.of("getDefaultBigDecimal", new BigDecimal("100.01"))),
                Arguments.of("defaults/defaults.smithy",
                        StructDefaultsTrait.class,
                        MapUtils.of("getDefaultTimestamp", Instant.parse("1985-04-12T23:20:50.52Z"))));
    }

    @ParameterizedTest
    @MethodSource("loadsModelTests")
    <T extends Trait> void executeTests(
            String resourceFile,
            Class<T> traitClass,
            Map<String, Object> valueChecks
    ) {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(Objects.requireNonNull(getClass().getResource(resourceFile)))
                .assemble()
                .unwrap();
        T trait = result.expectShape(ID).expectTrait(traitClass);
        valueChecks.forEach((k, v) -> checkValue(traitClass, trait, k, v));
    }

    <T extends Trait> void checkValue(Class<T> traitClass, T trait, String accessor, Object expected) {
        try {
            Object value = traitClass.getMethod(accessor).invoke(trait);
            // Float values need a delta specified for equals checks
            if (value instanceof Float) {
                assertEquals((Float) expected,
                        (Float) value,
                        0.0001,
                        "Value of accessor `" + accessor + "` invalid for " + trait);
            } else if (value instanceof Iterable) {
                assertIterableEquals((Iterable<?>) expected, (Iterable<?>) value);
            } else {
                assertEquals(expected,
                        value,
                        "Value of accessor `" + accessor
                                + "` invalid for " + trait);
            }

        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Failed to invoke accessor " + accessor + " for " + trait, e);
        }
    }
}
