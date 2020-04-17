package software.amazon.smithy.model.neighbor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
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
}
