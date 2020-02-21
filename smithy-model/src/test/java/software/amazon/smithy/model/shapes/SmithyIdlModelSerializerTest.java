package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.utils.IoUtils;

public class SmithyIdlModelSerializerTest {
    @TestFactory
    public Stream<DynamicTest> generateTests() throws IOException {
        return Files.list(Paths.get(
                SmithyIdlModelSerializer.class.getResource("idl-serialization/cases").getPath()))
                .map(path -> DynamicTest.dynamicTest(path.getFileName().toString(), () -> testConversion(path)));
    }

    public void testConversion(Path path) {
        Model model = Model.assembler().addImport(path).assemble().unwrap();
        SmithyIdlModelSerializer serializer = SmithyIdlModelSerializer.builder().build();
        Map<Path, String> serialized = serializer.serialize(model);

        if (serialized.size() != 1) {
            throw new RuntimeException("Exactly one smithy file should be output for generated tests.");
        }

        String serializedString = serialized.entrySet().iterator().next().getValue();
        assertThat(serializedString, equalTo(IoUtils.readUtf8File(path)));
    }

    @Test
    public void multipleNamespacesGenerateMultipleFiles() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("idl-serialization/multiple-namespaces/input.json"))
                .assemble()
                .unwrap();
        SmithyIdlModelSerializer serializer = SmithyIdlModelSerializer.builder().build();
        Map<Path, String> serialized = serializer.serialize(model);

        Path outputDir = Paths.get(getClass().getResource("idl-serialization/multiple-namespaces/output").getFile());
        serialized.forEach((path, generated) -> {
            Path expectedPath = outputDir.resolve(path);
            assertThat(generated, equalTo(IoUtils.readUtf8File(expectedPath)));
        });
    }
}
