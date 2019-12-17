package software.amazon.smithy.model.knowledge;

import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.BoxTrait;

public class BoxIndexTest {

    @ParameterizedTest
    @MethodSource("data")
    public void checksIfBoxed(Model model, String shapeId, boolean isBoxed) {
        BoxIndex index = model.getKnowledge(BoxIndex.class);
        ShapeId targetId = ShapeId.from(shapeId);

        if (isBoxed && !index.isBoxed(targetId)) {
            Assertions.fail("Did not expect shape to be determined as boxed: " + targetId);
        } else if (!isBoxed && index.isBoxed(targetId)) {
            Assertions.fail("Expected shape to be determined as boxed: " + targetId);
        }
    }

    public static Collection<Object[]> data() {
        MemberShape boxedListMember = MemberShape.builder()
                .id("smithy.test#BoxedList$member")
                .target("smithy.api#PrimitiveBoolean")
                .addTrait(new BoxTrait())
                .build();
        ListShape boxedList = ListShape.builder()
                .id("smithy.test#BoxedList")
                .member(boxedListMember)
                .build();

        MemberShape primitiveMember = MemberShape.builder()
                .id("smithy.test#PrimitiveList$member")
                .target("smithy.api#PrimitiveBoolean")
                .build();
        ListShape primitiveList = ListShape.builder()
                .id("smithy.test#PrimitiveList")
                .member(primitiveMember)
                .build();

        Model model = Model.assembler()
                .addShape(primitiveList)
                .addShape(boxedList)
                .assemble()
                .unwrap();

        return Arrays.asList(new Object[][]{
                {model, "smithy.api#String", true},
                {model, "smithy.api#Blob", true},
                {model, "smithy.api#Boolean", true},
                {model, "smithy.api#Timestamp", true},
                {model, "smithy.api#Byte", true},
                {model, "smithy.api#Short", true},
                {model, "smithy.api#Integer", true},
                {model, "smithy.api#Long", true},
                {model, "smithy.api#Float", true},
                {model, "smithy.api#Double", true},
                {model, "smithy.api#BigInteger", true},
                {model, "smithy.api#BigDecimal", true},

                {model, "smithy.api#PrimitiveByte", false},
                {model, "smithy.api#PrimitiveShort", false},
                {model, "smithy.api#PrimitiveInteger", false},
                {model, "smithy.api#PrimitiveLong", false},
                {model, "smithy.api#PrimitiveFloat", false},
                {model, "smithy.api#PrimitiveDouble", false},
                {model, "smithy.api#PrimitiveBoolean", false},

                {model, primitiveList.getId().toString(), true},
                {model, primitiveList.getMember().getId().toString(), false},
                {model, boxedList.getId().toString(), true},
                {model, boxedList.getMember().getId().toString(), true},
        });
    }
}
