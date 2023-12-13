package software.amazon.smithy.processor.test;

import java.nio.file.Paths;
import java.util.logging.Logger;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

public class TestBuildPlugin implements SmithyBuildPlugin {
    private static final Logger LOGGER = Logger.getLogger(TestBuildPlugin.class.getName());
    private static final String META_INF_LOCATION = "META-INF/services/";

    @Override
    public String getName() {
        return "test-plugin";
    }

    @Override
    public void execute(PluginContext context) {
        Settings settings = Settings.from(context.getSettings());
        FileManifest fileManifest = context.getFileManifest();

        String pathifiedNamespace = settings.namespace.replace(".", "/");

        // Write an empty java class
        fileManifest.writeFile(pathifiedNamespace + "/Empty.java",
                getEmptyClass(settings.namespace));

        // Write a META-INF metadata file
        fileManifest.writeFile(
                Paths.get(META_INF_LOCATION + "com.example.spi.impl.Example").normalize(),
                settings.namespace + "." + "Empty"
        );

        // Write a non-java file we expect to be ignored
        fileManifest.writeFile(pathifiedNamespace + "/Ignored.ignored",
                "ignore me. I dont compile!");

        // Require that expected smithy shapes were correctly loaded
        context.getModel().expectShape(ShapeId.from("com.testing.smithy#StructyMcStructFace"));
    }

    public static String getEmptyClass(String namespace) {
        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(namespace).append(";").append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("public final class Empty {").append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("}").append(System.lineSeparator());
        return builder.toString();
    }


    private static final class Settings {
        private final String namespace;

        private Settings(String namespace) {
            this.namespace = namespace;
        }

        static Settings from(ObjectNode node) {
            return new Settings(node.expectStringMember("namespace").getValue());
        }

        public String getNamespace() {
            return namespace;
        }
    }
}
