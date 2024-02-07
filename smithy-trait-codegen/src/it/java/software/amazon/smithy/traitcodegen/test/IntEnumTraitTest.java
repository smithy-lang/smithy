package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.traits.ResponseTypeIntTrait;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

class IntEnumTraitTest {
    @Test
    void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(Objects.requireNonNull(getClass().getResource("int-enum-trait.smithy")))
                .assemble()
                .unwrap();
        Shape shape = result.expectShape(ShapeId.from("test.smithy.traitcodegen#myStruct"));
        ResponseTypeIntTrait trait = shape.expectTrait(ResponseTypeIntTrait.class);
        assertTrue(trait.isYes());
        assertEquals(1, trait.getValue());
    }

    @Test
    void createsTrait() {
        ShapeId id = ShapeId.from("ns.foo#foo");
        Node node = Node.from(2);
        TraitFactory provider = TraitFactory.createServiceFactory();
        Trait trait = provider.createTrait(ResponseTypeIntTrait.ID, id, node).orElseThrow(RuntimeException::new);
        ResponseTypeIntTrait enumTrait = (ResponseTypeIntTrait) trait;
        assertEquals(SourceLocation.NONE, enumTrait.getSourceLocation());
        assertEquals(2, enumTrait.getValue());
        assertEquals(trait,
                provider.createTrait(ResponseTypeIntTrait.ID, id, trait.toNode()).orElseThrow(RuntimeException::new));
    }
}
