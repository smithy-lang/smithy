package software.amazon.smithy.jsonschema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.IoUtils;

public class SupportNonNumericFloatsTest {
    @Test
    public void addsNonNumericFloatSupport() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("non-numeric-floats.json"))
                .assemble()
                .unwrap();

        JsonSchemaConfig config = new JsonSchemaConfig();
        config.setSupportNonNumericFloats(true);
        SchemaDocument result = JsonSchemaConverter.builder()
                .config(config)
                .model(model)
                .build()
                .convert();
        assertThat(result.getDefinitions().keySet(), not(empty()));

        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("non-numeric-floats.jsonschema.json")));
        Node.assertEquals(result, expectedNode);
    }
}
