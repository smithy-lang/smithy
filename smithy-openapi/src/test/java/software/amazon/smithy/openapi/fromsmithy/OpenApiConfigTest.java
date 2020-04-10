package software.amazon.smithy.openapi.fromsmithy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.OpenApiException;

public class OpenApiConfigTest {
    @Test
    public void throwsOnDisableProperties() {
        Node disableTest = Node.objectNode().withMember("disable.additionalProperties", Node.from(true));

        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            OpenApiConfig.fromNode(disableTest);
        });

        assertThat(thrown.getMessage(), containsString("disableFeatures"));
    }

    @Test
    public void throwsOnOpenApiUseProperties() {
        Node openApiUseTest = Node.objectNode().withMember("openapi.use.xml", Node.from(true));

        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            OpenApiConfig.fromNode(openApiUseTest);
        });

        assertThat(thrown.getMessage(), containsString("disableFeatures"));
    }

    @Test
    public void convertsExplicitlyMappedProperties() {
        Node mappedTest = Node.objectNode()
                .withMember("openapi.tags", Node.from(true))
                .withMember("openapi.ignoreUnsupportedTraits", Node.from(true));
        OpenApiConfig config = OpenApiConfig.fromNode(mappedTest);

        assertThat(config.getTags(), equalTo(true));
        assertThat(config.getIgnoreUnsupportedTraits(), equalTo(true));
    }

    @Test
    public void putsAdditionalPropertiesInExtensions() {
        Node mappedTest = Node.objectNode()
                .withMember("tags", true)
                .withMember("apiGatewayType", "REST");
        OpenApiConfig config = OpenApiConfig.fromNode(mappedTest);

        assertThat(config.getTags(), equalTo(true));
        assertThat(config.getExtensions().getStringMap(), hasKey("apiGatewayType"));
    }
}
