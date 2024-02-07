package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.traits.StringTraitTrait;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

class StringTraitTest {
    @Test
    void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(Objects.requireNonNull(getClass().getResource("string-trait.smithy")))
                .assemble()
                .unwrap();
        Shape shape = result.expectShape(ShapeId.from("test.smithy.traitcodegen#myStruct$fieldA"));
        StringTraitTrait trait = shape.expectTrait(StringTraitTrait.class);
        assertEquals(trait.getValue(), "Testing String Trait");
    }

    @Test
    void createsTrait() {
        ShapeId id = ShapeId.from("ns.foo#foo");
        Node node = Node.from("SPORKZ SPOONS YAY! Utensils.");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Trait trait = provider.createTrait(StringTraitTrait.ID, id, node).orElseThrow(RuntimeException::new);
        StringTraitTrait annotation = (StringTraitTrait) trait;
        assertEquals(SourceLocation.NONE, annotation.getSourceLocation());
        assertEquals("SPORKZ SPOONS YAY! Utensils.", annotation.getValue());
        assertEquals(trait,
                provider.createTrait(StringTraitTrait.ID, id, trait.toNode()).orElseThrow(RuntimeException::new));
    }
}
