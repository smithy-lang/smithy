package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.traits.ResponseTypeTrait;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class EnumTraitTest {
    @Test
    void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(Objects.requireNonNull(getClass().getResource("enum-trait.smithy")))
                .assemble()
                .unwrap();
        Shape shape = result.expectShape(ShapeId.from("test.smithy.traitcodegen#myStruct"));
        ResponseTypeTrait trait = shape.expectTrait(ResponseTypeTrait.class);
        assertTrue(trait.isYes());
        assertEquals("yes", trait.getValue());
    }

    @Test
    void createsTrait() {
        ShapeId id = ShapeId.from("ns.foo#foo");
        Node node = Node.from("no");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Trait trait = provider.createTrait(ResponseTypeTrait.ID, id, node).orElseThrow(RuntimeException::new);
        ResponseTypeTrait enumTrait = (ResponseTypeTrait) trait;
        assertEquals(SourceLocation.NONE, enumTrait.getSourceLocation());
        assertEquals("no", enumTrait.getValue());
        assertEquals(trait,
                provider.createTrait(ResponseTypeTrait.ID, id, trait.toNode()).orElseThrow(RuntimeException::new));
    }
}
