package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.example.traits.ListMember;
import com.example.traits.StructureListTraitTrait;
import java.util.List;
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

class StructureListTraitTest {
    @Test
    void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(Objects.requireNonNull(getClass().getResource("structure-list-trait.smithy")))
                .assemble()
                .unwrap();
        Shape shape = result.expectShape(ShapeId.from("test.smithy.traitcodegen#myStruct"));
        StructureListTraitTrait trait = shape.expectTrait(StructureListTraitTrait.class);
        List<ListMember> actual = trait.getValues();
        List<ListMember> expected = ListUtils.of(
                ListMember.builder().a("first").b(1).c("other").build(),
                ListMember.builder().a("second").b(2).c("more").build()
        );
        assertIterableEquals(expected, actual);
    }

    @Test
    void createsTrait() {
        ShapeId id = ShapeId.from("ns.foo#foo");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Node input = ArrayNode.fromNodes(
                ListMember.builder().a("first").b(1).c("other").build().toNode(),
                ListMember.builder().a("second").b(2).c("more").build().toNode()
        );
        Trait trait = provider.createTrait(StructureListTraitTrait.ID, id, input).orElseThrow(RuntimeException::new);
        StructureListTraitTrait annotation = (StructureListTraitTrait) trait;
        assertEquals(SourceLocation.NONE, annotation.getSourceLocation());
        assertEquals(trait,
                provider.createTrait(StructureListTraitTrait.ID, id, trait.toNode()).orElseThrow(RuntimeException::new));
    }
}
