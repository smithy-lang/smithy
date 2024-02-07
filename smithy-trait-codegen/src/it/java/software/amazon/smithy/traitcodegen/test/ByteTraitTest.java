package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.traits.HttpCodeByteTrait;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

class ByteTraitTest {
    @Test
    void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(Objects.requireNonNull(getClass().getResource("byte-trait.smithy")))
                .assemble()
                .unwrap();
        Shape shape = result.expectShape(ShapeId.from("test.smithy.traitcodegen#myStruct"));
        HttpCodeByteTrait trait = shape.expectTrait(HttpCodeByteTrait.class);
        byte expected = 1;
        assertEquals(expected, trait.getValue());
    }

    @Test
    void createsTrait() {
        ShapeId id = ShapeId.from("ns.foo#foo");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Trait trait = provider.createTrait(HttpCodeByteTrait.ID, id, Node.from(1)).orElseThrow(RuntimeException::new);
        HttpCodeByteTrait annotation = (HttpCodeByteTrait) trait;
        assertEquals(SourceLocation.NONE, annotation.getSourceLocation());
        assertEquals(trait,
                provider.createTrait(HttpCodeByteTrait.ID, id, trait.toNode()).orElseThrow(RuntimeException::new));
    }
}
