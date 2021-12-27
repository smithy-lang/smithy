package software.amazon.smithy.model.knowledge;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.transform.ModelTransformer;

public class PreMixinIndexTest {

    @Test
    public void testUnroll() {
        Model withMixins = Model.assembler()
                .addImport(OperationIndexTest.class.getResource("premixin.smithy"))
                .assemble()
                .unwrap();

        PreMixinIndex index = PreMixinIndex.of(withMixins);
        Set<Shape> updatedShapes = new HashSet<>();
        withMixins.shapes().forEach(shape -> updatedShapes.add(index.getPreMixinShape(shape)));

        Model actual = ModelTransformer.create().replaceShapes(withMixins, updatedShapes);

        Model expected = Model.assembler()
                .addImport(OperationIndexTest.class.getResource("premixin-unrolled.smithy"))
                .assemble()
                .unwrap();

        if (!actual.equals(expected)) {
            ModelSerializer serializer = ModelSerializer.builder().build();
            ObjectNode actualNode = serializer.serialize(actual);
            ObjectNode expectedNode = serializer.serialize(expected);

            Node.assertEquals(actualNode, expectedNode);
        }
    }

}
