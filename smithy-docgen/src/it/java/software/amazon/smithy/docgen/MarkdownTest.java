package software.amazon.smithy.docgen;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.docgen.core.SmithyDocPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;

public class MarkdownTest {
    @Test
    public void test() {
        MockManifest manifest = new MockManifest();
        Model model = Model.assembler()
                .addImport(getClass().getResource("main.smithy"))
                .discoverModels(getClass().getClassLoader())
                .assemble()
                .unwrap();
        PluginContext context = PluginContext.builder()
                .fileManifest(manifest)
                .model(model)
                .settings(Node.objectNodeBuilder()
                        .withMember("service", "com.example#DocumentedService")
                        .withMember("format", "markdown")
                        .withMember("references", Node.objectNodeBuilder()
                                .withMember("com.example#ExternalResource", "https://aws.amazon.com")
                                .build())
                        .build())
                .build();

        SmithyBuildPlugin plugin = new SmithyDocPlugin();
        plugin.execute(context);
    }
}
