package software.amazon.smithy.syntax;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.IdlTokenizer;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.utils.IoUtils;

// A parameterized test that finds models in corpus, parses them, skipping files that end with ".formatted.smithy".
// If there is an x.formatted.smithy file, then ensure the model when formatted is equal to the formatted version.
// If there is no formatted version, then ensure that the model when formatted is equal to itself.
public class ParseAndFormatTest {

    private static final String CORPUS_DIR = "formatter";

    @ParameterizedTest(name = "{0}")
    @MethodSource("tests")
    public void testRunner(Path filename) {
        Path formattedFile = Paths.get(filename.toString().replace(".smithy", ".formatted.smithy"));
        if (!Files.exists(formattedFile)) {
            formattedFile = filename;
        }

        // Ensure that the tests can be parsed by smithy-model too.
        Model.assembler()
                .addImport(filename)
                .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                .disableValidation()
                .assemble()
                .unwrap();
        if (!formattedFile.equals(filename)) {
            Model.assembler()
                    .addImport(formattedFile)
                    .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                    .disableValidation()
                    .assemble()
                    .unwrap();
        }

        String model = IoUtils.readUtf8File(filename);
        IdlTokenizer tokenizer = IdlTokenizer.create(filename.toString(), model);
        TokenTree tree = TokenTree.of(tokenizer);
        String formatted = Formatter.format(tree, 120);
        String expected = IoUtils.readUtf8File(formattedFile);

        assertThat(formatted, equalTo(expected));
    }

    public static List<Path> tests() throws Exception {
        List<Path> paths = new ArrayList<>();

        try (Stream<Path> files = Files.walk(Paths.get(ParseAndFormatTest.class.getResource(CORPUS_DIR).toURI()))) {
            files
                    .filter(Files::isRegularFile)
                    .filter(file -> {
                        String filename = file.toString();
                        return filename.endsWith(".smithy") && !filename.endsWith(".formatted.smithy");
                    })
                    .forEach(paths::add);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return paths;
    }
}
