package software.amazon.smithy.rulesengine.traits;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;

public final class ContextParamTraitTest {

    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("traits-test-model.smithy"))
                .assemble()
                .unwrap();

        StructureShape structureShape = result.expectShape(ShapeId.from("smithy.example#GetThingInput"),
                StructureShape.class);

        MemberShape buzz = structureShape.getMember("buzz").get();
        ContextParamTrait trait = buzz.getTrait(ContextParamTrait.class).get();
        assertEquals(trait.getName(), "stringBaz");

        MemberShape fuzz = structureShape.getMember("fuzz").get();
        trait = fuzz.getTrait(ContextParamTrait.class).get();
        assertEquals(trait.getName(), "boolBaz");
    }
}
