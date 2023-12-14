package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.traits.NestedA;
import com.example.traits.NestedB;
import com.example.traits.StructureTraitTrait;
import java.math.BigDecimal;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

class StructureTraitTest {
    @Test
    void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(Objects.requireNonNull(getClass().getResource("struct-trait.smithy")))
                .assemble()
                .unwrap();
        Shape shape = result.expectShape(ShapeId.from("test.smithy.traitcodegen#myStruct"));
        StructureTraitTrait trait = shape.expectTrait(StructureTraitTrait.class);
        StructureTraitTrait expectedTrait = StructureTraitTrait.builder()
                .fieldA("first")
                .fieldB(false)
                .fieldC(NestedA.builder()
                        .fieldN("nested")
                        .fieldQ(true)
                        .fieldZ(NestedB.A)
                        .build()
                ).fieldD(ListUtils.of("a", "b", "c"))
                .fieldE(MapUtils.of("a", "one", "b", "two"))
                .fieldF(new BigDecimal("100.01"))
                .build();
        assertEquals(expectedTrait, trait);
    }

    @Test
    void createsTrait() {
        ShapeId id = ShapeId.from("ns.foo#foo");
        TraitFactory provider = TraitFactory.createServiceFactory();
        StructureTraitTrait struct = StructureTraitTrait.builder()
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
                .build();
        Trait trait = provider.createTrait(StructureTraitTrait.ID, id, struct.toNode())
                .orElseThrow(RuntimeException::new);
        StructureTraitTrait annotation = (StructureTraitTrait) trait;
        assertEquals(SourceLocation.NONE, annotation.getSourceLocation());
        assertEquals(trait, provider.createTrait(StructureTraitTrait.ID, id, trait.toNode())
                .orElseThrow(RuntimeException::new));
    }
}
