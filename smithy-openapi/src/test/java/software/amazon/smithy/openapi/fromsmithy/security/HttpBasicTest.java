package software.amazon.smithy.openapi.fromsmithy.security;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.LoaderUtils;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;

public class HttpBasicTest {
    @Test
    public void addsHttpBasicAuth() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("http-basic-security.json"))
                .assemble()
                .unwrap();
        OpenApi result = OpenApiConverter.create().convert(model, ShapeId.from("smithy.example#Service"));
        Node expectedNode = Node.parse(LoaderUtils.readInputStream(
                getClass().getResourceAsStream("http-basic-security.openapi.json"), "UTF-8"));

        Node.assertEquals(result, expectedNode);
    }
}
