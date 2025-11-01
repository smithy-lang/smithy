package software.amazon.smithy.build.plugins;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.shapes.SmithyIdlModelSerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class IDLPlugin implements SmithyBuildPlugin {
    private static final String NAME = "idl";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void execute(PluginContext context) {
        Map<Path, String> serialized = SmithyIdlModelSerializer.builder()
                .basePath(context.getFileManifest().getBaseDir())
                .build()
                .serialize(context.getModel());
        try {
            for (Map.Entry<Path, String> entry : serialized.entrySet()) {
                Path path = entry.getKey();
                System.err.println(path);
                Files.write(path, entry.getValue().getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean requiresValidModel() {
        return false;
    }
}
