package software.amazon.smithy.build.plugins;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.SmithyIdlModelSerializer;
import software.amazon.smithy.utils.FunctionalUtils;

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
        boolean includePrelude = context.getSettings().getBooleanMemberOrDefault("includePreludeShapes");
        SmithyIdlModelSerializer.Builder builder = SmithyIdlModelSerializer.builder()
            .basePath(context.getFileManifest().getBaseDir());
        if (includePrelude) {
            builder.serializePrelude();
        }
        Map<Path, String> serialized = builder
                .build()
                .serialize(context.getModel());
        try {
            Files.createDirectories(context.getFileManifest().getBaseDir());
            for (Map.Entry<Path, String> entry : serialized.entrySet()) {
                Path path = entry.getKey();
                context.getFileManifest().writeFile(path, entry.getValue());
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
