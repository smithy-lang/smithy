package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.traits.MapValue;
import com.example.traits.StringToStructMapTrait;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

class StringToStructMapTraitTest {
    @Test
    void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(Objects.requireNonNull(getClass().getResource("string-struct-map-trait.smithy")))
                .assemble()
                .unwrap();
        Shape shape = result.expectShape(ShapeId.from("test.smithy.traitcodegen#myStruct"));
        StringToStructMapTrait trait = shape.expectTrait(StringToStructMapTrait.class);

        MapValue one = trait.getValues().get("one");
        MapValue two = trait.getValues().get("two");
        assertEquals("foo", one.getA().orElseThrow(RuntimeException::new));
        assertEquals(2, one.getB().orElseThrow(RuntimeException::new));
        assertEquals("bar", two.getA().orElseThrow(RuntimeException::new));
        assertEquals(4, two.getB().orElseThrow(RuntimeException::new));
    }

    @Test
    void createsTrait() {
        ShapeId id = ShapeId.from("ns.foo#foo");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Node node = StringToStructMapTrait.builder()
                .putValues("one", MapValue.builder().a("foo").b(2).build())
                .putValues("two", MapValue.builder().a("bar").b(4).build())
                .build().toNode();
        Trait trait = provider.createTrait(StringToStructMapTrait.ID, id, node).orElseThrow(RuntimeException::new);
        StringToStructMapTrait annotation = (StringToStructMapTrait) trait;
        assertEquals(SourceLocation.NONE, annotation.getSourceLocation());
        assertEquals(trait,
                provider.createTrait(StringToStructMapTrait.ID, id, trait.toNode()).orElseThrow(RuntimeException::new));
    }
}
