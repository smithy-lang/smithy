package software.amazon.smithy.umlgen;

import java.util.logging.Logger;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.codegen.core.directed.CodegenDirector;

public class SmithyUmlPlugin implements SmithyBuildPlugin {
    private static final Logger LOGGER = Logger.getLogger(SmithyUmlPlugin.class.getName());

    private final CodegenDirector<PumlWriter, Smithy2PumlIntegration, Smithy2PumlContext, Smithy2PumlSettings>

    @Override
    public String getName() {
        return "smithy-uml";
    }

    @Override
    public void execute(PluginContext pluginContext) {
        var runner = new CodegenDirector<PumlWriter, Smithy2PumlIntegration, Smithy2PumlContext, Smithy2PumlSettings>();
        LOGGER.info("Initializing Smithy-UML plugin...");
        var settings = Smithy2PumlSettings.from(pluginContext.getSettings());
        runner.directedCodegen(new Smith2PumlDirectedCodeGen());
        runner.integrationClass(Smithy2PumlIntegration.class);
        runner.fileManifest(pluginContext.getFileManifest());
        runner.model(pluginContext.getModel());
        runner.settings(settings);
        runner.service(settings.serviceId());
        runner.performDefaultCodegenTransforms();
        runner.createDedicatedInputsAndOutputs();
        LOGGER.info("Executing Smithy2PUML plugin...");
        runner.run();
        LOGGER.info("Smithy2PUML plugin executed successfully.");
    }
}
