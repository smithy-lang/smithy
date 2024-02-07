package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.traits.JsonMetadataTrait;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

class DocumentTraitTest {
    @Test
    void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(Objects.requireNonNull(getClass().getResource("document-trait.smithy")))
                .assemble()
                .unwrap();
        Shape shape = result.expectShape(ShapeId.from("test.smithy.traitcodegen#myStruct"));
        JsonMetadataTrait trait = shape.expectTrait(JsonMetadataTrait.class);
        Node expected = Node.objectNodeBuilder()
                .withMember("metadata", "woo")
                .withMember("more", "yay")
                .build();
        assertEquals(expected, trait.toNode());
    }

    @Test
    void createsTrait() {
        ShapeId id = ShapeId.from("ns.foo#foo");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Node expected = Node.objectNodeBuilder()
                .withMember("metadata", "woo")
                .withMember("more", "yay")
                .build();
        Trait trait =
                provider.createTrait(JsonMetadataTrait.ID, id, expected).orElseThrow(RuntimeException::new);
        JsonMetadataTrait annotation = (JsonMetadataTrait) trait;
        assertEquals(SourceLocation.NONE, annotation.getSourceLocation());
        assertEquals(trait,
                provider.createTrait(JsonMetadataTrait.ID, id, trait.toNode()).orElseThrow(RuntimeException::new));
    }
}
