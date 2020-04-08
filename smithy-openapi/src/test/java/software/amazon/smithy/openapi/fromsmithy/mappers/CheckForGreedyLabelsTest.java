package software.amazon.smithy.openapi.fromsmithy.mappers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;

public class CheckForGreedyLabelsTest {
    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(RemoveUnusedComponentsTest.class.getResource("greedy-labels.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void logsInsteadOfThrows() {
        OpenApiConverter.create().convert(model, ShapeId.from("smithy.example#Greedy"));
    }

    @Test
    public void keepsUnusedSchemas() {
        OpenApiConfig config = new OpenApiConfig();
        config.setForbidGreedyLabels(true);

        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            OpenApiConverter.create()
                    .config(config)
                    .convert(model, ShapeId.from("smithy.example#Greedy"));
        });

        Assertions.assertTrue(thrown.getMessage().contains("greedy"));
    }
}
