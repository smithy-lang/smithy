package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.traits.HttpCodeBigDecimalTrait;
import java.math.BigDecimal;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

class BigDecimalTraitTest {
    @Test
    void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(Objects.requireNonNull(getClass().getResource("big-decimal-trait.smithy")))
                .assemble()
                .unwrap();
        Shape shape = result.expectShape(ShapeId.from("test.smithy.traitcodegen#myStruct"));
        HttpCodeBigDecimalTrait trait = shape.expectTrait(HttpCodeBigDecimalTrait.class);
        assertEquals(new BigDecimal("100.01"), trait.getValue());
    }

    @Test
    void createsTrait() {
        ShapeId id = ShapeId.from("ns.foo#foo");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Trait trait =
                provider.createTrait(HttpCodeBigDecimalTrait.ID, id, Node.from(1)).orElseThrow(RuntimeException::new);
        HttpCodeBigDecimalTrait annotation = (HttpCodeBigDecimalTrait) trait;
        assertEquals(SourceLocation.NONE, annotation.getSourceLocation());
        assertEquals(trait,
                provider.createTrait(HttpCodeBigDecimalTrait.ID, id, trait.toNode()).orElseThrow(RuntimeException::new));
    }
}
