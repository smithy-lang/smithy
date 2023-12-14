package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.example.traits.StringStringMapTrait;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.utils.MapUtils;

class StringStringMapTraitTest {
    @Test
    void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(Objects.requireNonNull(getClass().getResource("string-string-map-trait.smithy")))
                .assemble()
                .unwrap();
        Shape shape = result.expectShape(ShapeId.from("test.smithy.traitcodegen#myStruct"));
        StringStringMapTrait trait = shape.expectTrait(StringStringMapTrait.class);
        assertIterableEquals(MapUtils.of("a", "stuff", "b", "other", "c", "more!").entrySet(),
                trait.getValues().entrySet());
    }

    @Test
    void createsTrait() {
        ShapeId id = ShapeId.from("ns.foo#foo");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Node node = StringStringMapTrait.builder()
                .putValues("a", "first")
                .putValues("b", "other").build().toNode();
        Trait trait = provider.createTrait(StringStringMapTrait.ID, id, node).orElseThrow(RuntimeException::new);
        StringStringMapTrait annotation = (StringStringMapTrait) trait;
        assertEquals(SourceLocation.NONE, annotation.getSourceLocation());
        assertEquals(trait,
                provider.createTrait(StringStringMapTrait.ID, id, trait.toNode()).orElseThrow(RuntimeException::new));
    }
}
