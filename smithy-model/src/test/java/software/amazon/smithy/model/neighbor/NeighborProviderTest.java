package software.amazon.smithy.model.neighbor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.SensitiveTrait;

public class NeighborProviderTest {
    @Test
    public void canGetTraitRelationshipsFromStrings() {
        StringShape stringShape = StringShape.builder()
                .id(ShapeId.from("smithy.example#Foo"))
                .addTrait(new SensitiveTrait())
                .build();
        Model model = Model.assembler().addShape(stringShape).assemble().unwrap();

        NeighborProvider provider = NeighborProvider.of(model);
        provider = NeighborProvider.withTraitRelationships(model, provider);
        List<Relationship> relationships = provider.getNeighbors(stringShape);

        assertThat(relationships, hasSize(1));
        assertThat(relationships.get(0).getNeighborShapeId(), equalTo(SensitiveTrait.ID));
    }

    @Test
    public void canGetTraitRelationshipsFromShapeWithNoTraits() {
        StringShape stringShape = StringShape.builder()
                .id(ShapeId.from("smithy.example#Foo"))
                .build();
        Model model = Model.assembler().addShape(stringShape).assemble().unwrap();

        NeighborProvider provider = NeighborProvider.of(model);
        provider = NeighborProvider.withTraitRelationships(model, provider);
        List<Relationship> relationships = provider.getNeighbors(stringShape);

        assertThat(relationships, empty());
    }

    @ParameterizedTest
    @CsvSource({
            "One,Ref1",
            "Two,Ref2",
            "Three,Ref3",
            "Four,Ref4",
            "Five,Ref5",
            "Six,Ref6",
            "Seven,Ref7",
            "Eight,Ref8",
            "Nine,Ref9",
            "Ten,Ref10",
            "Eleven,Ref11",
            "Twelve,Ref12",
            "Thirteen,Ref13",
            "Fourteen,Ref14"
    })
    public void canGetIdRefRelationships(String shapeName, String referencedShapeName) {
        Model model = Model.assembler()
                .addImport(getClass().getResource("idref-neighbors.smithy"))
                .assemble()
                .unwrap();
        NeighborProvider provider = NeighborProvider.of(model);
        provider = NeighborProvider.withIdRefRelationships(model, provider);

        Shape shape = model.expectShape(ShapeId.fromParts("com.foo", shapeName));
        Shape ref = model.expectShape(ShapeId.fromParts("com.foo", referencedShapeName));
        List<Relationship> relationships = provider.getNeighbors(shape).stream()
                .filter(relationship -> relationship.getRelationshipType().equals(RelationshipType.ID_REF))
                .collect(Collectors.toList());

        assertThat(relationships, containsInAnyOrder(
                equalTo(Relationship.create(shape, RelationshipType.ID_REF, ref))));
    }

    @Test
    public void canGetIdRefRelationshipsThroughTraitDefs() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("idref-neighbors-in-trait-def.smithy"))
                .assemble()
                .unwrap();
        NeighborProvider provider = NeighborProvider.of(model);
        provider = NeighborProvider.withIdRefRelationships(model, provider);

        Shape shape = model.expectShape(ShapeId.from("com.foo#WithRefStructTrait"));
        Shape ref = model.expectShape(ShapeId.from("com.foo#OtherReferenced"));
        List<Relationship> relationships = provider.getNeighbors(shape).stream()
                .filter(relationship -> relationship.getRelationshipType().equals(RelationshipType.ID_REF))
                .collect(Collectors.toList());
        Shape shape1 = model.expectShape(ShapeId.from("com.foo#refStruct$other"));
        Shape ref1 = model.expectShape(ShapeId.from("com.foo#ReferencedInTraitDef"));
        List<Relationship> relationships1 = provider.getNeighbors(shape1).stream()
                .filter(relationship -> relationship.getRelationshipType().equals(RelationshipType.ID_REF))
                .collect(Collectors.toList());

        assertThat(relationships, containsInAnyOrder(Relationship.create(shape, RelationshipType.ID_REF, ref)));
        assertThat(relationships1, containsInAnyOrder(Relationship.create(shape1, RelationshipType.ID_REF, ref1)));
    }

    @Test
    public void canGetIdRefRelationshipsThroughMultipleLevelsOfIdRef() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("idref-neighbors-multiple-levels.smithy"))
                .assemble()
                .unwrap();
        NeighborProvider provider = NeighborProvider.of(model);
        provider = NeighborProvider.withIdRefRelationships(model, provider);

        Shape shape = model.expectShape(ShapeId.from("com.foo#WithIdRef"));
        Shape ref = model.expectShape(ShapeId.from("com.foo#Referenced"));
        List<Relationship> relationships = provider.getNeighbors(shape).stream()
                .filter(relationship -> relationship.getRelationshipType().equals(RelationshipType.ID_REF))
                .collect(Collectors.toList());
        Shape shape1 = model.expectShape(ShapeId.from("com.foo#ConnectedThroughReferenced"));
        Shape ref1 = model.expectShape(ShapeId.from("com.foo#AnotherReferenced"));
        List<Relationship> relationships1 = provider.getNeighbors(shape1).stream()
                .filter(relationship -> relationship.getRelationshipType().equals(RelationshipType.ID_REF))
                .collect(Collectors.toList());

        assertThat(relationships, containsInAnyOrder(Relationship.create(shape, RelationshipType.ID_REF, ref)));
        assertThat(relationships1, containsInAnyOrder(Relationship.create(shape1, RelationshipType.ID_REF, ref1)));
    }
}
