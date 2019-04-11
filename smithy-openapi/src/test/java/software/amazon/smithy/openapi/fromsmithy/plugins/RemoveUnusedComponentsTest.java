package software.amazon.smithy.openapi.fromsmithy.plugins;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;

public class RemoveUnusedComponentsTest {
    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(RemoveUnusedComponentsTest.class.getResource("small-service.smithy"))
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void removesUnusedSchemas() {
        OpenApi result = OpenApiConverter.create().convert(model, ShapeId.from("smithy.example#Small"));

        Assertions.assertTrue(result.getComponents().getSchemas().isEmpty());
    }

    @Test
    public void keepsUnusedSchemas() {
        OpenApi result = OpenApiConverter.create()
                .putSetting(OpenApiConstants.OPENAPI_KEEP_UNUSED_COMPONENTS, true)
                .convert(model, ShapeId.from("smithy.example#Small"));

        // The input structure remains in the output even though it's unreferenced.
        Assertions.assertFalse(result.getComponents().getSchemas().isEmpty());
    }
}
