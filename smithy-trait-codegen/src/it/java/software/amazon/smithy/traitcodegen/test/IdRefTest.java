package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.traits.IdRefListTrait;
import com.example.traits.IdRefMapTrait;
import com.example.traits.IdRefStringTrait;
import com.example.traits.IdRefStructTrait;
import com.example.traits.IdRefStructWithNestedIdsTrait;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;

class IdRefTest {
    private static final ShapeId TARGET_ONE = ShapeId.from("test.smithy.traitcodegen#IdRefTarget1");
    private static final ShapeId TARGET_TWO = ShapeId.from("test.smithy.traitcodegen#IdRefTarget2");

    @Test
    void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(Objects.requireNonNull(getClass().getResource("id-ref.smithy")))
                .assemble()
                .unwrap();
        Shape shape = result.expectShape(ShapeId.from("test.smithy.traitcodegen#myStruct"));

        // Single string representing an IdRef
        IdRefStringTrait idRefStringTrait = shape.expectTrait(IdRefStringTrait.class);
        assertEquals(TARGET_ONE, idRefStringTrait.getValue());

        // List of ShapeIDs
        IdRefListTrait idRefListTrait = shape.expectTrait(IdRefListTrait.class);
        assertIterableEquals(ListUtils.of(TARGET_ONE, TARGET_TWO), idRefListTrait.getValues());

        // Map of ShapeIDs
        IdRefMapTrait idRefMapTrait = shape.expectTrait(IdRefMapTrait.class);
        assertEquals(2, idRefMapTrait.getValues().size());
        assertEquals(TARGET_ONE, idRefMapTrait.getValues().get("a"));
        assertEquals(TARGET_TWO, idRefMapTrait.getValues().get("b"));

        // Shape ID as member of a structure
        IdRefStructTrait idRefStructTrait = shape.expectTrait(IdRefStructTrait.class);
        assertTrue(idRefStructTrait.getFieldA().isPresent());
        assertEquals(TARGET_ONE, idRefStructTrait.getFieldA().get());

        IdRefStructWithNestedIdsTrait idRefStructWithNestedIds = shape.expectTrait(IdRefStructWithNestedIdsTrait.class);
        assertEquals(TARGET_ONE, idRefStructWithNestedIds.getIdRefHolder().getId());
        assertIterableEquals(ListUtils.of(TARGET_ONE, TARGET_TWO), idRefStructWithNestedIds.getIdList());
        assertEquals(TARGET_ONE, idRefStructWithNestedIds.getIdMap().get("a"));
        assertEquals(TARGET_TWO, idRefStructWithNestedIds.getIdMap().get("b"));
    }
}
