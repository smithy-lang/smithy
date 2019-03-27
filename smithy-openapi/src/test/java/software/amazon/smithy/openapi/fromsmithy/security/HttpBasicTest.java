package software.amazon.smithy.openapi.fromsmithy.security;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.LoaderUtils;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;

public class HttpBasicTest {
    @Test
    public void addsHttpBasicAuth() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("http-basic-security.json"))
                .assemble()
                .unwrap();
        var result = OpenApiConverter.create().convert(model, ShapeId.from("smithy.example#Service"));
        var expectedNode = Node.parse(LoaderUtils.readInputStream(
                getClass().getResourceAsStream("http-basic-security.openapi.json"), "UTF-8"));

        Node.assertEquals(result, expectedNode);
    }
}
