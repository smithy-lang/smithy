/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        Path importsFile = Paths.get(filename.toString().replace(".smithy", ".imports.json"));
        Path syntaxOnlyFile = Paths.get(filename.toString().replace(".smithy", ".syntax-only"));
        if (!Files.exists(formattedFile)) {
            formattedFile = filename;
        }

        // Ensure that the tests can be parsed by smithy-model too.
        // Syntax-only cases cover future IDL versions not yet supported by semantic assembly.
        if (!Files.exists(syntaxOnlyFile)) {
            assemble(filename, importsFile);
            if (!formattedFile.equals(filename)) {
                assemble(formattedFile, importsFile);
            }
        }

        String model = IoUtils.readUtf8File(filename);
        IdlTokenizer tokenizer = IdlTokenizer.create(filename.toString(), model);
        TokenTree tree = TokenTree.of(tokenizer);
        String formatted = Formatter.format(tree, 120);
        String expected = IoUtils.readUtf8File(formattedFile);

        assertEquals(expected, formatted);

        // Formatting must be idempotent: re-formatting the expected output produces the same output.
        IdlTokenizer reTokenizer = IdlTokenizer.create(formattedFile.toString(), expected);
        TokenTree reTree = TokenTree.of(reTokenizer);
        String reformatted = Formatter.format(reTree, 120);
        assertEquals(expected, reformatted, "Formatter is not idempotent for " + formattedFile);
    }

    private void assemble(Path model, Path imports) {
        ModelAssembler assembler = Model.assembler()
                .addImport(model)
                .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                .disableValidation();
        if (Files.exists(imports)) {
            assembler.addImport(imports);
        }
        assembler.assemble().unwrap();
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
