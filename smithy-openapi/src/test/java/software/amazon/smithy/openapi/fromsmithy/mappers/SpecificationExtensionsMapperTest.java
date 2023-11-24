package software.amazon.smithy.openapi.fromsmithy.mappers;

import java.io.InputStream;
import java.net.URL;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.utils.IoUtils;

public class SpecificationExtensionsMapperTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "inlined-type-target",
            "structure-target",
            "operation-target",
            "service-target"
    })
    public void checkMapping(String name) {
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));

        Node actual = OpenApiConverter
                        .create()
                        .config(config)
                        .convertToNode(getSpecificationExtensionTraits(name));
        Node expected = getExpectedOpenAPI(name);

        Node.assertEquals(actual, expected);
    }

    private static Model getSpecificationExtensionTraits(String name) {
        return Model.assembler()
                .addImport(getResource(name + ".smithy"))
                .addImport(getResource("specification-extension-traits.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
    }

    private static Node getExpectedOpenAPI(String name) {
        return Node.parse(IoUtils.toUtf8String(getResourceAsStream(name + ".openapi.json")));
    }

    private static URL getResource(String name) {
        return SpecificationExtensionsMapperTest.class.getResource("specificationextensions/" + name);
    }

    private static InputStream getResourceAsStream(String name) {
        return SpecificationExtensionsMapperTest.class.getResourceAsStream("specificationextensions/" + name);
    }
}
