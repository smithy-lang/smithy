package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.traits.BasicAnnotationTrait;
import com.example.traits.HttpCodeBigDecimalTrait;
import com.example.traits.HttpCodeBigIntegerTrait;
import com.example.traits.HttpCodeByteTrait;
import com.example.traits.HttpCodeDoubleTrait;
import com.example.traits.HttpCodeFloatTrait;
import com.example.traits.HttpCodeIntegerTrait;
import com.example.traits.HttpCodeLongTrait;
import com.example.traits.HttpCodeShortTrait;
import com.example.traits.JsonMetadataTrait;
import com.example.traits.ListMember;
import com.example.traits.MapValue;
import com.example.traits.NestedA;
import com.example.traits.NestedB;
import com.example.traits.NumberListTrait;
import com.example.traits.NumberSetTrait;
import com.example.traits.ResponseTypeIntTrait;
import com.example.traits.ResponseTypeTrait;
import com.example.traits.StringListTrait;
import com.example.traits.StringSetTrait;
import com.example.traits.StringStringMapTrait;
import com.example.traits.StringToStructMapTrait;
import com.example.traits.StringTrait;
import com.example.traits.StructWithMixinTrait;
import com.example.traits.StructureListTrait;
import com.example.traits.StructureListWithMixinMemberTrait;
import com.example.traits.StructureSetTrait;
import com.example.traits.StructureTrait;
import com.example.traits.SuitTrait;
import com.example.traits.names.SnakeCaseStructureTrait;
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
                Arguments.of(BasicAnnotationTrait.ID, Node.objectNode()),
                Arguments.of(HttpCodeBigDecimalTrait.ID, Node.from(1)),
                Arguments.of(HttpCodeBigIntegerTrait.ID, Node.from(1)),
                Arguments.of(HttpCodeByteTrait.ID, Node.from(1)),
                Arguments.of(JsonMetadataTrait.ID, Node.objectNodeBuilder()
                        .withMember("metadata", "woo")
                        .withMember("more", "yay")
                        .build()
                ),
                Arguments.of(HttpCodeDoubleTrait.ID, Node.from(1.2)),
                Arguments.of(ResponseTypeTrait.ID, Node.from("no")),
                Arguments.of(HttpCodeFloatTrait.ID, Node.from(1.2)),
                Arguments.of(HttpCodeIntegerTrait.ID, Node.from(1)),
                Arguments.of(ResponseTypeIntTrait.ID, Node.from(2)),
                Arguments.of(HttpCodeLongTrait.ID, Node.from(1L)),
                Arguments.of(NumberListTrait.ID, ArrayNode.fromNodes(
                        Node.from(1), Node.from(2), Node.from(3))
                ),
                Arguments.of(NumberSetTrait.ID, ArrayNode.fromNodes(
                        Node.from(1), Node.from(2), Node.from(3))
                ),
                Arguments.of(HttpCodeShortTrait.ID, Node.from(1)),
                Arguments.of(StringListTrait.ID, ArrayNode.fromStrings("a", "b", "c")),
                Arguments.of(StringSetTrait.ID, ArrayNode.fromStrings("a", "b", "c")),
                Arguments.of(StringStringMapTrait.ID, StringStringMapTrait.builder()
                        .putValues("a", "first").putValues("b", "other").build().toNode()
                ),
                Arguments.of(StringToStructMapTrait.ID, StringToStructMapTrait.builder()
                        .putValues("one", MapValue.builder().a("foo").b(2).build())
                        .putValues("two", MapValue.builder().a("bar").b(4).build())
                        .build().toNode()
                ),
                Arguments.of(StringTrait.ID, Node.from("SPORKZ SPOONS YAY! Utensils.")),
                Arguments.of(StructureListTrait.ID, ArrayNode.fromNodes(
                        ListMember.builder().a("first").b(1).c("other").build().toNode(),
                        ListMember.builder().a("second").b(2).c("more").build().toNode()
                )),
                Arguments.of(StructureSetTrait.ID, ArrayNode.fromNodes(
                        ListMember.builder().a("first").b(1).c("other").build().toNode(),
                        ListMember.builder().a("second").b(2).c("more").build().toNode()
                )),
                Arguments.of(StructureTrait.ID, StructureTrait.builder()
                        .fieldA("a")
                        .fieldB(true)
                        .fieldC(NestedA.builder()
                                .fieldN("nested")
                                .fieldQ(false)
                                .fieldZ(NestedB.B)
                                .build()
                        )
                        .fieldD(ListUtils.of("a", "b", "c"))
                        .fieldE(MapUtils.of("a", "one", "b", "two"))
                        .build().toNode()
                ),
                Arguments.of(SnakeCaseStructureTrait.ID, ObjectNode.builder()
                        .withMember("snake_case_member", "stuff").build()),
                Arguments.of(StructureListWithMixinMemberTrait.ID,
                        ArrayNode.fromNodes(ObjectNode.builder().withMember("a", "a").withMember("d", "d").build())),
                Arguments.of(StructWithMixinTrait.ID, StructWithMixinTrait.builder()
                        .d("d").build().toNode()),
                Arguments.of(SuitTrait.ID, Node.from("CLUBS"))
        );
    }

    @ParameterizedTest
    @MethodSource("createTraitTests")
    void createsTraitFromNode(ShapeId traitId, Node fromNode) {
        Trait trait = provider.createTrait(traitId, DUMMY_ID, fromNode).orElseThrow(RuntimeException::new);
        assertEquals(SourceLocation.NONE, trait.getSourceLocation());
        assertEquals(trait, provider.createTrait(traitId, DUMMY_ID, trait.toNode()).orElseThrow(RuntimeException::new));
    }
}
