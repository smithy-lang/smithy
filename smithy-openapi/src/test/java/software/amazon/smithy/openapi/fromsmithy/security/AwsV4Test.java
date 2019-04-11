package software.amazon.smithy.openapi.fromsmithy.security;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.LoaderUtils;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;

public class AwsV4Test {
    @Test
    public void addsAwsV4() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("awsv4-security.json"))
                .assemble()
                .unwrap();
        OpenApi result = OpenApiConverter.create().convert(model, ShapeId.from("smithy.example#Service"));
        Node expectedNode = Node.parse(LoaderUtils.readInputStream(
                getClass().getResourceAsStream("awsv4-security.openapi.json"), "UTF-8"));

        Node.assertEquals(result, expectedNode);
    }
}
