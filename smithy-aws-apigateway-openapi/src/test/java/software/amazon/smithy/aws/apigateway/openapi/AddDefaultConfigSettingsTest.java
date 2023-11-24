package software.amazon.smithy.aws.apigateway.openapi;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.utils.IoUtils;

public class AddDefaultConfigSettingsTest {
    @Test
    public void defaultsTo2023_08_11() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("default-config-settings.smithy"))
                .assemble()
                .unwrap();

        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));
        config.setUseIntegerType(true);
        ObjectNode result = OpenApiConverter.create()
                .config(config)
                .convertToNode(model);

        Node.assertEquals(result, Node.parse(IoUtils.readUtf8Resource(getClass(), "2023-08-11.openapi.json")));
    }

    @Test
    public void usesVersion2023_08_11() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("default-config-settings.smithy"))
                .assemble()
                .unwrap();

        OpenApiConfig openApiConfig = new OpenApiConfig();
        openApiConfig.setService(ShapeId.from("example.smithy#MyService"));
        openApiConfig.setUseIntegerType(true);

        ApiGatewayConfig config = new ApiGatewayConfig();
        NodeMapper mapper = new NodeMapper();
        config.setApiGatewayDefaults(ApiGatewayDefaults.VERSION_2023_08_11);
        openApiConfig.setExtensions(mapper.serialize(config).expectObjectNode());
        ObjectNode result = OpenApiConverter.create()
                .config(openApiConfig)
                .convertToNode(model);

        Node.assertEquals(result, Node.parse(IoUtils.readUtf8Resource(getClass(), "2023-08-11.openapi.json")));
    }

    @Test
    public void canDisableDefaults() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("default-config-settings.smithy"))
                .assemble()
                .unwrap();

        OpenApiConfig openApiConfig = new OpenApiConfig();
        openApiConfig.setService(ShapeId.from("example.smithy#MyService"));
        openApiConfig.setUseIntegerType(true);

        ApiGatewayConfig config = new ApiGatewayConfig();
        NodeMapper mapper = new NodeMapper();
        config.setApiGatewayDefaults(ApiGatewayDefaults.DISABLED);
        openApiConfig.setExtensions(mapper.serialize(config).expectObjectNode());
        ObjectNode result = OpenApiConverter.create()
                .config(openApiConfig)
                .convertToNode(model);

        Node.assertEquals(result, Node.parse(IoUtils.readUtf8Resource(getClass(), "disabled-defaults.openapi.json")));
    }
}
