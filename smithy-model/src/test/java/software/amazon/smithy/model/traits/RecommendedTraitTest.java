package software.amazon.smithy.model.traits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.RecommendedTrait.Provider;

public class RecommendedTraitTest {
    @Test
    public void providerProperlySetsReason() {
        String reason = "just because";
        ObjectNode node = Node.objectNode().withMember("reason", reason);
        Provider provider = new Provider();
        RecommendedTrait result = provider.createTrait(ShapeId.from("ns.example#Foo$bar"), node);
        assertTrue(result.getReason().isPresent());
        assertEquals(result.getReason().get(), reason);
    }
}
