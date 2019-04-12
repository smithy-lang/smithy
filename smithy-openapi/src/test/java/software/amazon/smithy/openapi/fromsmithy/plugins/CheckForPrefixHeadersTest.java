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

public class CheckForPrefixHeadersTest {
    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(RemoveUnusedComponentsTest.class.getResource("prefix-headers.smithy"))
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void canIgnorePrefixHeaders() {
        OpenApiConverter.create()
                .putSetting(OpenApiConstants.ON_HTTP_PREFIX_HEADERS, OpenApiConstants.ON_HTTP_PREFIX_HEADERS_WARN)
                .convert(model, ShapeId.from("smithy.example#PrefixHeaders"));
    }

    @Test
    public void throwsOnPrefixHeadersByDefault() {
        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            OpenApiConverter.create().convert(model, ShapeId.from("smithy.example#PrefixHeaders"));
        });

        Assertions.assertTrue(thrown.getMessage().contains("httpPrefixHeaders"));
    }
}
