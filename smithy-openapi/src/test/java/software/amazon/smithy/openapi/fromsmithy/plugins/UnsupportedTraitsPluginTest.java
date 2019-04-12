package software.amazon.smithy.openapi.fromsmithy.plugins;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;

public class UnsupportedTraitsPluginTest {
    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(UnsupportedTraitsPluginTest.class.getResource("streaming-service.smithy"))
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void logsWhenUnsupportedTraitsAreFound() {
        OpenApiConverter.create()
                .putSetting(OpenApiConstants.IGNORE_UNSUPPORTED_TRAIT, true)
                .convert(model, ShapeId.from("smithy.example#Streaming"));
    }

    @Test
    public void throwsWhenUnsupportedTraitsAreFound() {
        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            OpenApiConverter.create().convert(model, ShapeId.from("smithy.example#Streaming"));
        });

        Assertions.assertTrue(thrown.getMessage().contains("streaming"));
    }
}
