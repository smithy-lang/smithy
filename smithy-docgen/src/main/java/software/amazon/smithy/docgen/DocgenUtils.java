/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.ServiceIndex.AuthSchemeMode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * Provides various utility methods.
 */
@SmithyUnstableApi
public final class DocgenUtils {

    private static final Logger LOGGER = Logger.getLogger(DocgenUtils.class.getName());

    private DocgenUtils() {}

    /**
     * Executes a given shell command in a given directory.
     *
     * @param command The string command to execute, e.g. "sphinx-build".
     * @param directory The directory to run the command in.
     * @return Returns the console output of the command.
     */
    public static String runCommand(String command, Path directory) {
        String[] finalizedCommand;
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            finalizedCommand = new String[] {"cmd.exe", "/c", command};
        } else {
            finalizedCommand = new String[] {"sh", "-c", command};
        }

        ProcessBuilder processBuilder = new ProcessBuilder(finalizedCommand)
                .redirectErrorStream(true)
                .directory(directory.toFile());

        try {
            Process process = processBuilder.start();
            List<String> output = new ArrayList<>();

            // Capture output for reporting.
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(),
                    Charset.defaultCharset()))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    LOGGER.finest(line);
                    output.add(line);
                }
            }

            process.waitFor();
            process.destroy();

            String joinedOutput = String.join(System.lineSeparator(), output);
            if (process.exitValue() != 0) {
                throw new CodegenException(format(
                        "Command `%s` failed with output:%n%n%s",
                        command,
                        joinedOutput));
            }
            return joinedOutput;
        } catch (InterruptedException | IOException e) {
            throw new CodegenException(e);
        }
    }

    /**
     * Replaces all newline characters in a string with the system line separator.
     * @param input The string to normalize
     * @return A string with system-appropriate newlines.
     */
    public static String normalizeNewlines(String input) {
        return input.replaceAll("\r?\n", System.lineSeparator());
    }

    /**
     * Gets a relative link pointing to a given symbol.
     *
     * <p>If the given symbol has no definition file or no
     * {@link DocSymbolProvider#LINK_ID_PROPERTY}, the response will be empty.
     *
     * @param symbol The symbol to link to.
     * @param relativeTo A path that the symbol should be relative to. This must be the
     *                   path to the file containing the link.
     * @return Optionally returns a relative link to the given symbol.
     */
    public static Optional<String> getSymbolLink(Symbol symbol, Path relativeTo) {
        Optional<String> linkId = symbol.getProperty(DocSymbolProvider.LINK_ID_PROPERTY, String.class);
        var relativeToParent = relativeTo.getParent();
        if (StringUtils.isBlank(symbol.getDefinitionFile())
                || linkId.isEmpty()
                || StringUtils.isBlank(linkId.get())
                || relativeToParent == null) {
            return Optional.empty();
        }
        return Optional.of(format(
                "./%s#%s",
                relativeToParent.relativize(Paths.get(symbol.getDefinitionFile())),
                linkId.get()));
    }

    /**
     * Gets a priority-ordered list of the service's auth types.
     *
     * <p>This includes all the auth types bound to the service, not just those present
     * in the {@code auth} trait. Auth types not present in the auth trait are at the
     * end of the list, in alphabetical order.
     *
     * @param model The model being generated from.
     * @param service The service being documented.
     * @return returns a priority-ordered list of service auth types.
     */
    public static List<ShapeId> getPrioritizedServiceAuth(Model model, ToShapeId service) {
        var index = ServiceIndex.of(model);

        // Get the effective auth schemes first and add them to an ordered set. This
        // is important to do because the effective schemes are explicitly ordered in
        // the model by the auth trait, and we want to document the auth options in
        // that same order.
        var authSchemes = new LinkedHashSet<>(index.getEffectiveAuthSchemes(service, AuthSchemeMode.MODELED).keySet());

        // Since the auth trait can exclude some of the service's auth types, we need
        // to add those in last.
        authSchemes.addAll(index.getAuthSchemes(service).keySet());

        return List.copyOf(authSchemes);
    }
}
