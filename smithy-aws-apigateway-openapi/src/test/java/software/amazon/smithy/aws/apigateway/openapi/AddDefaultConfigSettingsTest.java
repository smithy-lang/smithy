package software.amazon.smithy.aws.apigateway.openapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodePointer;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.utils.IoUtils;

public class AddDefaultConfigSettingsTest {
    @Test
    public void addsDefaultConfigSettings() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("alphanumeric-only.json"))
                .assemble()
                .unwrap();

        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));
        ObjectNode result = OpenApiConverter.create()
                .config(config)
                .convertToNode(model);

        // Ensure that Foo_Baz became FooBaz.
        NodePointer pointer = NodePointer.parse("/components/schemas/FooBaz");
        assertThat(pointer.getValue(result), not(Optional.empty()));
    }

    @Test
    public void omitsDefaultTraits() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("omits-default-trait.smithy"))
                .assemble()
                .unwrap();

        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));
        ObjectNode result = OpenApiConverter.create()
                .config(config)
                .convertToNode(model);

        Node.assertEquals(result, Node.parse(IoUtils.readUtf8Resource(getClass(), "omits-default-trait.openapi.json")));
    }
}
