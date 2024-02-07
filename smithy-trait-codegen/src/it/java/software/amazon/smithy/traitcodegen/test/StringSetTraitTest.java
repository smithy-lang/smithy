package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.traits.StringSetTraitTrait;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.utils.SetUtils;

class StringSetTraitTest {
    @Test
    void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(Objects.requireNonNull(getClass().getResource("string-set-trait.smithy")))
                .assemble()
                .unwrap();
        Shape shape = result.expectShape(ShapeId.from("test.smithy.traitcodegen#myStruct"));
        StringSetTraitTrait trait = shape.expectTrait(StringSetTraitTrait.class);
        assertEquals(SetUtils.of("a", "b", "c", "d"), trait.getValues());
    }

    @Test
    void createsTrait() {
        ShapeId id = ShapeId.from("ns.foo#foo");
        TraitFactory provider = TraitFactory.createServiceFactory();
        ArrayNode input = ArrayNode.fromStrings("a", "b", "c");
        Trait trait = provider.createTrait(StringSetTraitTrait.ID, id, input).orElseThrow(RuntimeException::new);
        StringSetTraitTrait annotation = (StringSetTraitTrait) trait;
        assertEquals(SourceLocation.NONE, annotation.getSourceLocation());
        assertEquals(trait,
                provider.createTrait(StringSetTraitTrait.ID, id, trait.toNode()).orElseThrow(RuntimeException::new));
    }
}
