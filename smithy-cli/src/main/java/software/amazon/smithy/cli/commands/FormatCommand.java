/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.ColorBuffer;
import software.amazon.smithy.cli.ColorTheme;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.model.loader.IdlTokenizer;
import software.amazon.smithy.syntax.Formatter;
import software.amazon.smithy.syntax.TokenTree;
import software.amazon.smithy.utils.IoUtils;

final class FormatCommand implements Command {

    private final String parentCommandName;

    FormatCommand(String parentCommandName) {
        this.parentCommandName = parentCommandName;
    }

    @Override
    public String getName() {
        return "format";
    }

    @Override
    public String getSummary() {
        return "Formats Smithy IDL models.";
    }

    private static final class Options implements ArgumentReceiver {
        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.positional("<MODEL>",
                    "A single `.smithy` model file or a directory of model files to recursively format.");
        }
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        arguments.addReceiver(new Options());

        CommandAction action = HelpActionWrapper.fromCommand(this, parentCommandName, c -> {
            ColorBuffer buffer = ColorBuffer.of(c, new StringBuilder());
            buffer.println("Examples:");
            buffer.println("   smithy format model-file.smithy", ColorTheme.LITERAL);
            buffer.println("   smithy format model/", ColorTheme.LITERAL);
            return buffer.toString();
        }, this::run);
        return action.apply(arguments, env);
    }

    private int run(Arguments arguments, Env env) {
        if (arguments.getPositional().isEmpty()) {
            throw new CliError("No .smithy model or directory was provided as a positional argument");
        } else if (arguments.getPositional().size() > 1) {
            throw new CliError("Only a single .smithy model or directory can be provided as a positional argument");
        }

        String filename = arguments.getPositional().get(0);
        Path path = Paths.get(filename);

        if (Files.isRegularFile(path)) {
            if (!filename.endsWith(".smithy")) {
                throw new CliError("`" + filename + "` is not a .smithy model file");
            }
        } else if (!Files.isDirectory(path)) {
            throw new CliError("`" + filename + "` is not a valid file or directory");
        }

        formatFile(path);
        return 0;
    }

    private void formatFile(Path file) {
        if (Files.isDirectory(file)) {
            try {
                Files.find(file, 100, (p, a) -> a.isRegularFile()).forEach(this::formatFile);
            } catch (IOException e) {
                throw new CliError("Error formatting " + file + " (directory): " + e.getMessage());
            }
        } else if (Files.isRegularFile(file) && file.toString().endsWith(".smithy")) {
            TokenTree tree = parse(file);
            String formatted = Formatter.format(tree);
            try (OutputStream s = Files.newOutputStream(file, StandardOpenOption.TRUNCATE_EXISTING)) {
                s.write(formatted.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new CliError("Error formatting " + file + " (file): " + e.getMessage());
            }
        }
    }

    private TokenTree parse(Path file) {
        String contents = IoUtils.readUtf8File(file);
        IdlTokenizer tokenizer = IdlTokenizer.create(file.toString(), contents);
        return TokenTree.of(tokenizer);
    }
}
