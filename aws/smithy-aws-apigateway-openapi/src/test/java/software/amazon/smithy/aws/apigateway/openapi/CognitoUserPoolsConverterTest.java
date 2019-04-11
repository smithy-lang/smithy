package software.amazon.smithy.aws.apigateway.openapi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.LoaderUtils;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;

public class CognitoUserPoolsConverterTest {
    @Test
    public void addsAwsV4() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("cognito-user-pools-security.json"))
                .assemble()
                .unwrap();
        OpenApi result = OpenApiConverter.create().convert(model, ShapeId.from("smithy.example#Service"));
        Node expectedNode = Node.parse(LoaderUtils.readInputStream(
                getClass().getResourceAsStream("cognito-user-pools-security.openapi.json"), "UTF-8"));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void requiresProviderArns() {
        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            Model model = Model.assembler(getClass().getClassLoader())
                    .discoverModels(getClass().getClassLoader())
                    .addImport(getClass().getResource("invalid-cognito-user-pools-security.json"))
                    .assemble()
                    .unwrap();
            OpenApiConverter.create().convert(model, ShapeId.from("smithy.example#Service"));
        });

        Assertions.assertTrue(thrown.getMessage().contains("Missing required"));
    }
}
