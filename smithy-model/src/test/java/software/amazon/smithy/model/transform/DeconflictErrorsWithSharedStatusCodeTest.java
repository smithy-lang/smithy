package software.amazon.smithy.model.transform;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

public class DeconflictErrorsWithSharedStatusCodeTest {
    @Test
    public void deconflictErrorsWithSharedStatusCodes() {
        Model input = Model.assembler()
                .addImport(getClass().getResource("conflicting-errors.smithy"))
                .assemble()
                .unwrap();
        Model output = Model.assembler()
                .addImport(getClass().getResource("deconflicted-errors.smithy"))
                .assemble()
                .unwrap();

        ModelTransformer transformer = ModelTransformer.create();

        ServiceShape service = input.expectShape(ShapeId.from("smithy.example#MyService"), ServiceShape.class);

        Model result = transformer.deconflictErrorsWithSharedStatusCode(input, service);

        Node actual = ModelSerializer.builder().build().serialize(result);
        Node expected = ModelSerializer.builder().build().serialize(output);
        Node.assertEquals(actual, expected);
    }

    @Test
    public void throwsWhenHeadersConflict() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("conflicting-errors-with-conflicting-headers.smithy"))
                .assemble()
                .unwrap();

        ModelTransformer transformer = ModelTransformer.create();

        ServiceShape service = model.expectShape(ShapeId.from("smithy.example#MyService"), ServiceShape.class);
        assertThrows(ModelTransformException.class,
                () -> transformer.deconflictErrorsWithSharedStatusCode(model, service));
    }
}
