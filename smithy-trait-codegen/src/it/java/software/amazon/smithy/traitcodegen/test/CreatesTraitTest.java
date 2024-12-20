/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.traits.StringTrait;
import com.example.traits.defaults.StructDefaultsTrait;
import com.example.traits.documents.DocumentTrait;
import com.example.traits.documents.StructWithNestedDocumentTrait;
import com.example.traits.enums.EnumListMemberTrait;
import com.example.traits.enums.IntEnumTrait;
import com.example.traits.enums.StringEnumTrait;
import com.example.traits.enums.SuitTrait;
import com.example.traits.lists.DocumentListTrait;
import com.example.traits.lists.ListMember;
import com.example.traits.lists.NumberListTrait;
import com.example.traits.lists.StringListTrait;
import com.example.traits.lists.StructureListTrait;
import com.example.traits.maps.MapValue;
import com.example.traits.maps.StringDocumentMapTrait;
import com.example.traits.maps.StringStringMapTrait;
import com.example.traits.maps.StringToStructMapTrait;
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
import com.example.traits.timestamps.TimestampTrait;
import com.example.traits.uniqueitems.NumberSetTrait;
import com.example.traits.uniqueitems.SetMember;
import com.example.traits.uniqueitems.StringSetTrait;
import com.example.traits.uniqueitems.StructureSetTrait;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

public class CreatesTraitTest {
    private static final ShapeId DUMMY_ID = ShapeId.from("ns.foo#foo");
    private final TraitFactory provider = TraitFactory.createServiceFactory();

    static Stream<Arguments> createTraitTests() {
        return Stream.of(
                // Document traits
                Arguments.of(DocumentTrait.ID,
                        Node.objectNodeBuilder()
                                .withMember("metadata", "woo")
                                .withMember("more", "yay")
                                .build()),
                Arguments.of(StructWithNestedDocumentTrait.ID,
                        ObjectNode.objectNodeBuilder()
                                .withMember("doc",
                                        ObjectNode.builder()
                                                .withMember("foo", "bar")
                                                .withMember("fizz", "buzz")
                                                .build())
                                .build()),
                // Enums
                Arguments.of(StringEnumTrait.ID, Node.from("no")),
                Arguments.of(IntEnumTrait.ID, Node.from(2)),
                Arguments.of(SuitTrait.ID, Node.from("clubs")),
                Arguments.of(EnumListMemberTrait.ID,
                        ObjectNode.objectNodeBuilder()
                                .withMember("value", ArrayNode.fromStrings("some", "none", "some"))
                                .build()),
                // Lists
                Arguments.of(NumberListTrait.ID,
                        ArrayNode.fromNodes(
                                Node.from(1),
                                Node.from(2),
                                Node.from(3))),
                Arguments.of(StringListTrait.ID, ArrayNode.fromStrings("a", "b", "c")),
                Arguments.of(StructureListTrait.ID,
                        ArrayNode.fromNodes(
                                ListMember.builder().a("first").b(1).c("other").build().toNode(),
                                ListMember.builder().a("second").b(2).c("more").build().toNode())),
                Arguments.of(DocumentListTrait.ID,
                        ArrayNode.fromNodes(
                                ObjectNode.builder().withMember("a", "b").build(),
                                ObjectNode.builder().withMember("c", "d").withMember("e", "f").build())),
                // Maps
                Arguments.of(StringStringMapTrait.ID,
                        StringStringMapTrait.builder()
                                .putValues("a", "first")
                                .putValues("b", "other")
                                .build()
                                .toNode()),
                Arguments.of(StringToStructMapTrait.ID,
                        StringToStructMapTrait.builder()
                                .putValues("one", MapValue.builder().a("foo").b(2).build())
                                .putValues("two", MapValue.builder().a("bar").b(4).build())
                                .build()
                                .toNode()),
                Arguments.of(StringDocumentMapTrait.ID,
                        StringDocumentMapTrait.builder()
                                .putValues("a", ObjectNode.builder().withMember("a", "a").build().toNode())
                                .putValues("b", ObjectNode.builder().withMember("b", "b").build().toNode())
                                .putValues("string", Node.from("stuff"))
                                .putValues("number", Node.from(1))
                                .build()
                                .toNode()),
                // Mixins
                Arguments.of(StructureListWithMixinMemberTrait.ID,
                        ArrayNode.fromNodes(ObjectNode.builder().withMember("a", "a").withMember("d", "d").build())),
                Arguments.of(StructWithMixinTrait.ID,
                        StructWithMixinTrait.builder()
                                .d("d")
                                .build()
                                .toNode()),
                // Naming Conflicts
                Arguments.of(SnakeCaseStructureTrait.ID,
                        ObjectNode.builder()
                                .withMember("snake_case_member", "stuff")
                                .build()),
                // Numbers
                Arguments.of(BigDecimalTrait.ID, Node.from(1)),
                Arguments.of(BigIntegerTrait.ID, Node.from(1)),
                Arguments.of(ByteTrait.ID, Node.from(1)),
                Arguments.of(DoubleTrait.ID, Node.from(1.2)),
                Arguments.of(FloatTrait.ID, Node.from(1.2)),
                Arguments.of(IntegerTrait.ID, Node.from(1)),
                Arguments.of(LongTrait.ID, Node.from(1L)),
                Arguments.of(ShortTrait.ID, Node.from(1)),
                // Structures
                Arguments.of(BasicAnnotationTrait.ID, Node.objectNode()),
                Arguments.of(StructureTrait.ID,
                        StructureTrait.builder()
                                .fieldA("a")
                                .fieldB(true)
                                .fieldC(NestedA.builder()
                                        .fieldN("nested")
                                        .fieldQ(false)
                                        .fieldZ(NestedB.B)
                                        .build())
                                .fieldD(ListUtils.of("a", "b", "c"))
                                .fieldE(MapUtils.of("a", "one", "b", "two"))
                                .build()
                                .toNode()),
                // Timestamps
                Arguments.of(TimestampTrait.ID, Node.from("1985-04-12T23:20:50.52Z")),
                Arguments.of(DateTimeTimestampTrait.ID, Node.from("1985-04-12T23:20:50.52Z")),
                Arguments.of(HttpDateTimestampTrait.ID, Node.from("Tue, 29 Apr 2014 18:30:38 GMT")),
                Arguments.of(EpochSecondsTimestampTrait.ID, Node.from(1515531081.123)),
                // Unique Items (sets)
                Arguments.of(NumberSetTrait.ID,
                        ArrayNode.fromNodes(
                                Node.from(1),
                                Node.from(2),
                                Node.from(3))),
                Arguments.of(StringSetTrait.ID, ArrayNode.fromStrings("a", "b", "c")),
                Arguments.of(StructureSetTrait.ID,
                        ArrayNode.fromNodes(
                                SetMember.builder().a("first").b(1).c("other").build().toNode(),
                                SetMember.builder().a("second").b(2).c("more").build().toNode())),
                // Strings
                Arguments.of(StringTrait.ID, Node.from("SPORKZ SPOONS YAY! Utensils.")),
                // Defaults
                Arguments.of(StructDefaultsTrait.ID, Node.objectNode()));
    }

    @ParameterizedTest
    @MethodSource("createTraitTests")
    void createsTraitFromNode(ShapeId traitId, Node fromNode) {
        Trait trait = provider.createTrait(traitId, DUMMY_ID, fromNode).orElseThrow(RuntimeException::new);
        assertEquals(SourceLocation.NONE, trait.getSourceLocation());
        assertEquals(trait, provider.createTrait(traitId, DUMMY_ID, trait.toNode()).orElseThrow(RuntimeException::new));
    }
}
