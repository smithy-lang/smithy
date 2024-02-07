package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.example.traits.NumberListTraitTrait;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.utils.ListUtils;

class NumberListTraitTest {
    @Test
    void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(Objects.requireNonNull(getClass().getResource("number-list-trait.smithy")))
                .assemble()
                .unwrap();
        Shape shape = result.expectShape(ShapeId.from("test.smithy.traitcodegen#myStruct"));
        NumberListTraitTrait trait = shape.expectTrait(NumberListTraitTrait.class);
        assertIterableEquals(ListUtils.of(1, 2, 3, 4, 5), trait.getValues());
    }

    @Test
    void createsTrait() {
        ShapeId id = ShapeId.from("ns.foo#foo");
        TraitFactory provider = TraitFactory.createServiceFactory();
        ArrayNode input = ArrayNode.fromNodes(Node.from(1), Node.from(2), Node.from(3));
        Trait trait = provider.createTrait(NumberListTraitTrait.ID, id, input).orElseThrow(RuntimeException::new);
        NumberListTraitTrait annotation = (NumberListTraitTrait) trait;
        assertEquals(SourceLocation.NONE, annotation.getSourceLocation());
        assertEquals(trait,
                provider.createTrait(NumberListTraitTrait.ID, id, trait.toNode()).orElseThrow(RuntimeException::new));
    }
}
