/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.cli.commands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.SmithyIdlModelSerializer;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class TransformAwsSdkModelsCommand extends SimpleCommand {
    private static final Logger LOGGER = Logger.getLogger(TransformAwsSdkModelsCommand.class.getName());

    public TransformAwsSdkModelsCommand(String parentCommandName) {
        super(parentCommandName);
    }

    @Override
    public String getName() {
        return "transform_aws_sdk";
    }

    @Override
    public String getSummary() {
        return "Transforms AWS SDK Smithy models";
    }

    private static final class Options implements ArgumentReceiver {
        private Path output;
        private Path models;

        @Override
        public Consumer<String> testParameter(String name) {
            if (name.equals("--output")) {
                return value -> output = new File(value).toPath();
            } else if (name.equals("--models")) {
                return value -> models = new File(value).toPath();
            }
            return null;
        }

        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.param("--output", null, "OUTPUT...",
                          "Path to produce output");
        }
    }

    @Override
    protected int run(Arguments arguments, Env env, List<String> positional) {
        Options options = arguments.getReceiver(Options.class);
        //models expected to be a single file or directories to scan where 1 file per directory
        List<String> modelFiles = new ArrayList<>();

        try {
            Files.walk(options.models)
                .filter(rawpath -> {
                    Path path = rawpath.normalize();
                    File file = path.toFile();
                    String name = file.getName();
                    if (file.getParentFile() != null) {
                        String parentName = file.getParentFile().getName();
                        return parentName.equals("smithy") && name.equals("model.json");
                    }
                    return false;
                })
                .forEach(path -> modelFiles.add(path.toFile().getAbsolutePath()));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        final Path outputDir = handleOutputDirectory(options.output).orElseGet(() -> Path.of("."));
        final Path reportFileOutput = outputDir.resolve("report.out");
        try (BufferedWriter reportWriter = Files.newBufferedWriter(reportFileOutput, StandardCharsets.UTF_8)) {
            for (String file : modelFiles) {
                try {
                    final Model convertedModel = CommandUtils
                            .buildModelForSingleFile(file, arguments, env, env.stderr(), true);
                    final String serviceName = getServiceName(convertedModel);
                    //filenames of errors
                    // final Path errorPathOut = outputDir.resolve(serviceName.toLowerCase())
                    //.resolve(serviceName.toLowerCase() + ".errors");
                    try {
                        final Map<Path, String> idlFiles = SmithyIdlModelSerializer.builder()
                                .build()
                                .serialize(convertedModel);

                        //there should only be a single file out that is associated with the service
                        final Map.Entry<Path, String> mainIdlFile = getServiceFile(idlFiles);
                        final Path pathOut = getOutputServiceFilename(outputDir, mainIdlFile);

                        writeCurrentSmithyVersionOuput(pathOut, mainIdlFile.getValue());
                        reportWriter.write("Service file written out: "
                                            + pathOut.toFile().getAbsolutePath() + System.lineSeparator());
                    } catch (Exception e) {
                        reportWriter.write(String.format("Exception on service {%s, %s} :: %s%n",
                                                            serviceName, file, e.getMessage()));
                        e.printStackTrace(System.err);
                        continue;
                    }
                } catch (Exception e) {
                    reportWriter.write(String.format("Error loading model file {%s} :: %s%n", file, e.getMessage()));
                    e.printStackTrace(System.err);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not open file for output reporting: " + reportFileOutput.toString(), e);
        }

        return 0;
    }

    private void writeCurrentSmithyVersionOuput(final Path outPath, final String modelText) {
        try {
            Files.writeString(outPath, modelText, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CliError("Could not write model out for: " + outPath.toFile().getAbsolutePath());
        }
    }

    private void writeErrors(Path errorPathOut, ValidatedResult<Model> modelValidatedResult) {
        if (!modelValidatedResult.getValidationEvents().isEmpty()) {
            try (StringWriter content = new StringWriter()) {
                for (ValidationEvent event : modelValidatedResult.getValidationEvents()) {
                    content.write(String.format("%s%n", event.toString()));
                }
                Files.writeString(errorPathOut, content.toString(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new CliError("Could not write errors out for: " + errorPathOut.toFile().getAbsolutePath());
            }
            LOGGER.info("Errors file written out: " + errorPathOut.toFile().getAbsolutePath());
        }
    }

    private Path getOutputServiceFilename(final Path parentDir, final Entry<Path, String> mainIdlFile) {
        final String lastPart = mainIdlFile.getKey().toFile().getName();
        return parentDir.resolve(lastPart.substring("com.amazonaws.".length()));
    }

    private String getServiceName(final Model convertedModel) {
        final Set<ServiceShape> serviceShapes = convertedModel.getServiceShapes();
        if (serviceShapes.size() != 1) {
            throw new RuntimeException(String.format("Zero or more than one service shapes {%d} found.",
                    serviceShapes.size()));
        }
        final ServiceShape shape = serviceShapes.iterator().next();
        return shape.getId().getName();
    }

    private static Map.Entry<Path, String> getServiceFile(final Map<Path, String> files) {
        List<Map.Entry<Path, String>> idlFile = files.entrySet().stream()
                .filter(entry -> entry.getKey().toFile().getName().startsWith("com.amazonaws"))
                .collect(Collectors.toList());
        if (idlFile.size() != 1) {
            throw new RuntimeException(String.format("Could not find exactly one {%d} service.", idlFile.size()));
        }
        return idlFile.get(0);
    }

    public static Optional<Path> handleOutputDirectory(Path output) {
        if (output != null) {
            String outputRelativePath = output.toFile().getPath();
            Path outputDirPath = Path.of(outputRelativePath);
            File outputDirFile = outputDirPath.toFile();
            if (outputDirFile.exists()) {
                if (!outputDirFile.isDirectory()) {
                    throw new CliError("Output path is not a directory!");
                }
            } else {
                if (!outputDirFile.mkdirs()) {
                    throw new CliError("Cannot create output directory!");
                }
            }
            return Optional.of(outputDirPath);
        }
        return Optional.empty();
    }

    @Override
    public void printHelp(Arguments arguments, CliPrinter printer) {
        // TODO Auto-generated method stub
    }

    @Override
    protected List<ArgumentReceiver> createArgumentReceivers() {
        return ListUtils.of(new Options(), new BuildOptions(), new StandardOptions());
    }
}
