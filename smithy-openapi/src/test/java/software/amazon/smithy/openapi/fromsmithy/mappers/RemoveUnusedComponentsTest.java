package software.amazon.smithy.openapi.fromsmithy.mappers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.SecurityScheme;

public class RemoveUnusedComponentsTest {
    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(RemoveUnusedComponentsTest.class.getResource("small-service.smithy"))
                .discoverModels()
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

    @Test
    public void removesUnusedSchemes() {
        OpenApi result = OpenApiConverter.create()
                .addOpenApiMapper(new OpenApiMapper() {
                    @Override
                    public OpenApi after(Context context, OpenApi openapi) {
                        return openapi.toBuilder()
                                .components(openapi.getComponents().toBuilder()
                                        .putSecurityScheme("foo", SecurityScheme.builder().type("apiKey").build())
                                        .build())
                                .build();
                    }
                })
                .convert(model, ShapeId.from("smithy.example#Small"));

        Assertions.assertFalse(result.getComponents().getSecuritySchemes().keySet().contains("foo"));
    }
}
