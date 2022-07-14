/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.cli.commands;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import software.amazon.smithy.build.ProjectionResult;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.loader.ParserUtils;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.NumberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.SmithyIdlModelSerializer;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.SimpleParser;
import software.amazon.smithy.utils.SmithyInternalApi;
import software.amazon.smithy.utils.StringUtils;

@SmithyInternalApi
public final class Upgrade1to2Command extends SimpleCommand {
    private static final Logger LOGGER = Logger.getLogger(Upgrade1to2Command.class.getName());
    private static final Pattern VERSION_1 = Pattern.compile("(?m)^\\s*\\$\\s*version:\\s*\"1\\.0\"\\s*$");
    private static final Pattern VERSION_2 = Pattern.compile("(?m)^\\s*\\$\\s*version:\\s*\"2\\.0\"\\s*$");
    private static final EnumSet<ShapeType> HAD_DEFAULT_VALUE_IN_1_0 = EnumSet.of(
            ShapeType.BYTE,
            ShapeType.SHORT,
            ShapeType.INTEGER,
            ShapeType.LONG,
            ShapeType.FLOAT,
            ShapeType.DOUBLE,
            ShapeType.BOOLEAN);

    public Upgrade1to2Command(String parentCommandName) {
        super(parentCommandName);
    }

    private static final class Options implements ArgumentReceiver {
        private String config = "smithy-build.json";

        @Override
        public boolean testOption(String name) {
            return false;
        }

        @Override
        public Consumer<String> testParameter(String name) {
            switch (name) {
                case "--config":
                case "-c":
                    return c -> this.config = c;
                default:
                    return null;
            }
        }

        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.param("--config", "-c", "CONFIG_PATH", "Path to smithy-build.json configuration.");
        }
    }

    @Override
    protected List<ArgumentReceiver> createArgumentReceivers() {
        return Arrays.asList(new Options(), new BuildOptions());
    }

    @Override
    public String getName() {
        return "upgrade-1-to-2";
    }

    @Override
    public String getSummary() {
        return "Upgrades Smithy IDL model files from 1.0 to 2.0 in place.";
    }

    @Override
    protected int run(Arguments arguments, Env env, List<String> models) {
        Options commandOptions = arguments.getReceiver(Options.class);

        // Use the provided smithy-build.json file
        SmithyBuildConfig.Builder configBuilder = SmithyBuildConfig.builder();
        if (Files.exists(Paths.get(commandOptions.config))) {
            configBuilder.load(Paths.get(commandOptions.config).toAbsolutePath());
        }

        // Set an output into a temporary directory - we don't actually care about
        // the serialized output.
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("smithyUpgrade");
        } catch (IOException e) {
            throw new CliError("Unable to create temporary working directory: " + e);
        }
        configBuilder.outputDirectory(tempDir.toString());

        Model initialModel = CommandUtils.buildModel(arguments, models, env, env.stderr(), true);

        SmithyBuild smithyBuild = SmithyBuild.create(env.classLoader())
                .config(configBuilder.build())
                // Only build the source projection
                .projectionFilter(name -> name.equals("source"))
                // The only traits we care about looking at are in the prelude,
                // so we can safely ignore any that are unknown.
                .modelAssemblerSupplier(() -> {
                    ModelAssembler assembler = Model.assembler();
                    assembler.putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true);
                    return assembler;
                })
                .model(initialModel);

        // Run SmithyBuild to get the finalized model
        ResultConsumer resultConsumer = new ResultConsumer();
        smithyBuild.build(resultConsumer, resultConsumer);
        Model finalizedModel = resultConsumer.getResult().getModel();

        for (Path modelFile : resolveModelFiles(finalizedModel, models)) {
            writeUpgradedFile(finalizedModel, modelFile);
        }

        return 0;
    }

    private List<Path> resolveModelFiles(Model model, List<String> modelFilesOrDirectories) {
        Set<Path> absoluteModelFilesOrDirectories = modelFilesOrDirectories.stream()
                .map(path -> Paths.get(path).toAbsolutePath())
                .collect(Collectors.toSet());
        return model.shapes()
                .filter(shape -> !Prelude.isPreludeShape(shape))
                .filter(shape -> !shape.getSourceLocation().getFilename().startsWith("jar:"))
                .map(shape -> Paths.get(shape.getSourceLocation().getFilename()).toAbsolutePath())
                .distinct()
                .filter(locationPath -> {
                    for (Path inputPath : absoluteModelFilesOrDirectories) {
                        if (!locationPath.startsWith(inputPath)) {
                            LOGGER.finest("Skipping non-target model file: " + locationPath);
                            return false;
                        }
                    }
                    if (!locationPath.toString().endsWith(".smithy")) {
                        LOGGER.info("Skipping non-IDL model file: " + locationPath);
                        return false;
                    }
                    return true;
                })
                .sorted()
                .collect(Collectors.toList());
    }

    private void writeUpgradedFile(Model completeModel, Path filePath) {
        try {
            Files.write(filePath, upgradeFile(completeModel, filePath).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new CliError(format("Unable to write upgraded model file to %s: %s", filePath, e));
        }
    }

    String upgradeFile(Model completeModel, Path filePath) {
        String contents = IoUtils.readUtf8File(filePath);
        if (VERSION_2.matcher(contents).find()) {
            return contents;
        }

        ShapeUpgradeVisitor visitor = new ShapeUpgradeVisitor(completeModel, contents);

        completeModel.shapes()
                .filter(shape -> shape.getSourceLocation().getFilename().equals(filePath.toString()))
                // Apply updates to the shapes at the bottom of the file first.
                // This lets us modify the file without invalidating the existing
                // source locations.
                .sorted(Comparator.comparing(Shape::getSourceLocation).reversed())
                .forEach(shape -> shape.accept(visitor));

        return updateVersion(visitor.getModelString());
    }

    private String updateVersion(String modelString) {
        Matcher matcher = VERSION_1.matcher(modelString);
        if (matcher.find()) {
            return matcher.replaceFirst(format("\\$version: \"2.0\"%n"));
        }
        return format("$version: \"2.0\"%n%n") + modelString;
    }

    private static final class ResultConsumer implements Consumer<ProjectionResult>, BiConsumer<String, Throwable> {
        private Throwable error;
        private ProjectionResult result;

        @Override
        public void accept(String name, Throwable throwable) {
            this.error = throwable;
        }

        @Override
        public void accept(ProjectionResult projectionResult) {
            // We only expect one result because we're only building one
            // projection - the source projection.
            this.result = projectionResult;
        }

        ProjectionResult getResult() {
            if (error != null) {
                throw new RuntimeException(error);
            }
            return result;
        }
    }

    private static class ShapeUpgradeVisitor extends ShapeVisitor.Default<Void> {
        private final Model completeModel;
        private final ModelWriter writer;

        ShapeUpgradeVisitor(Model completeModel, String modelString) {
            this.completeModel = completeModel;
            this.writer = new ModelWriter(modelString);
        }

        String getModelString() {
            return writer.flush();
        }

        @Override
        protected Void getDefault(Shape shape) {
            if (shape.hasTrait(BoxTrait.class)) {
                writer.eraseTrait(shape, shape.expectTrait(BoxTrait.class));
            }
            // Handle members in reverse definition order.
            shape.members().stream()
                    .sorted(Comparator.comparing(Shape::getSourceLocation).reversed())
                    .forEach(this::handleMemberShape);
            return null;
        }

        private void handleMemberShape(MemberShape shape) {
            replacePrimitiveTarget(shape);

            if (hasSyntheticDefault(shape)) {
                SourceLocation memberLocation = shape.getSourceLocation();
                String padding = "";
                if (memberLocation.getColumn() > 1) {
                    padding = StringUtils.repeat(' ', memberLocation.getColumn() - 1);
                }
                Shape target = completeModel.expectShape(shape.getTarget());
                String defaultValue = "";
                if (target.isBooleanShape()) {
                    defaultValue = "false";
                } else if (target instanceof NumberShape) {
                    defaultValue = "0";
                } else if (target.isBlobShape() || target.isStringShape()) {
                    defaultValue = "\"\"";
                } else {
                    throw new UnsupportedOperationException("Unexpected default: " + target);
                }
                writer.insertLine(shape.getSourceLocation().getLine(), padding + "@default(" + defaultValue + ")");
            }

            if (shape.hasTrait(BoxTrait.class)) {
                writer.eraseTrait(shape, shape.expectTrait(BoxTrait.class));
            }
        }

        private void replacePrimitiveTarget(MemberShape member) {
            Shape target = completeModel.expectShape(member.getTarget());
            if (!Prelude.isPreludeShape(target) || !HAD_DEFAULT_VALUE_IN_1_0.contains(target.getType())) {
                return;
            }

            IdlAwareSimpleParser parser = new IdlAwareSimpleParser(writer.flush());
            parser.rewind(member.getSourceLocation());

            parser.consumeUntilNoLongerMatches(character -> character != ':');
            parser.skip();
            parser.ws();

            // Capture the start of the target identifier.
            int start = parser.position();
            parser.consumeUntilNoLongerMatches(ParserUtils::isValidIdentifierCharacter);

            // Replace the target with the proper target. Note that we don't
            // need to do any sort of mapping because smithy already upgraded
            // the target, so we can just use the name of the target it added.
            writer.replace(start, parser.position(), target.getId().getName());
        }

        private boolean hasSyntheticDefault(MemberShape shape) {
            Optional<SourceLocation> defaultLocation = shape.getTrait(DefaultTrait.class)
                    .map(Trait::getSourceLocation);
            // When Smithy injects the default trait, it sets the source
            // location equal to the shape's source location. This is
            // impossible in any other scenario, so we can use this info
            // to know whether it was injected or not.
            return defaultLocation.filter(location -> shape.getSourceLocation().equals(location)).isPresent();
        }

        @Override
        public Void memberShape(MemberShape shape) {
            // members are handled from their containers so that they can
            // be properly sorted.
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            if (!shape.hasTrait(EnumTrait.class)) {
                return null;
            }

            EnumTrait enumTrait = shape.expectTrait(EnumTrait.class);
            if (!enumTrait.getValues().iterator().next().getName().isPresent()) {
                return null;
            }

            writer.insertLine(shape.getSourceLocation().getLine() + 1, serializeEnum(shape));
            writer.eraseLine(shape.getSourceLocation().getLine());
            writer.eraseTrait(shape, enumTrait);

            return null;
        }

        private String serializeEnum(StringShape shape) {
            // Strip all the traits from the shape except the enum trait.
            // We're leaving the other traits where they are in the model
            // string to preserve things like comments as much as is possible.
            StringShape stripped = shape.toBuilder()
                    .clearTraits()
                    .addTrait(shape.expectTrait(EnumTrait.class))
                    .build();

            // Build a faux model that only contains the enum we want to write.
            Model model = Model.assembler()
                    .addShapes(stripped)
                    .assemble().unwrap();

            // Use existing conversion tools to convert it to an enum shape,
            // then serialize it using the idl serializer.
            model = ModelTransformer.create().changeStringEnumsToEnumShapes(model);
            Map<Path, String> files = SmithyIdlModelSerializer.builder().build().serialize(model);

            // There's only one shape, so there should only be one file.
            String serialized = files.values().iterator().next();

            // The serialized file will contain things we don't want, like the
            // namespace and version statements, so here we strip everything
            // we find before the enum statement.
            ArrayList<String> lines = new ArrayList<>();
            boolean foundEnum = false;
            for (String line : serialized.split("\\r?\\n")) {
                if (foundEnum) {
                    lines.add(line);
                } else if (line.startsWith("enum")) {
                    lines.add(line);
                    foundEnum = true;
                }
            }

            return String.join(System.lineSeparator(), lines);
        }
    }

    private static class IdlAwareSimpleParser extends SimpleParser {
        IdlAwareSimpleParser(String expression) {
            super(expression);
        }

        public void rewind(SourceLocation location) {
            rewind(0, 1, 1);
            while (!eof()) {
                if (line() == location.getLine() && column() == location.getColumn()) {
                    break;
                }
                skip();
            }
            if (eof()) {
                throw syntax("Expected a source location, but was EOF");
            }
        }

        @Override
        public void ws() {
            while (!eof()) {
                switch (peek()) {
                    case '/':
                        // If we see a comment, advance to the next line.
                        if (peek(1) == '/') {
                            consumeRemainingCharactersOnLine();
                            break;
                        } else {
                            return;
                        }
                    case ' ':
                    case '\t':
                    case '\r':
                    case '\n':
                    case ',':
                        skip();
                        break;
                    default:
                        return;
                }
            }
        }
    }

    private static class ModelWriter {
        private String contents;

        ModelWriter(String contents) {
            this.contents = contents;
        }

        public String flush() {
            if (!contents.endsWith(System.lineSeparator())) {
                contents = contents + System.lineSeparator();
            }
            return contents;
        }

        private void insertLine(int lineNumber, String line) {
            List<String> lines = new ArrayList<>(Arrays.asList(contents.split("\\r?\\n")));
            lines.add(lineNumber - 1, line);
            contents = String.join(System.lineSeparator(), lines);
        }

        private void eraseLine(int lineNumber) {
            List<String> lines = new ArrayList<>(Arrays.asList(contents.split("\\r?\\n")));
            lines.remove(lineNumber - 1);
            contents = String.join(System.lineSeparator(), lines);
        }

        private void eraseTrait(Shape shape, Trait trait) {
            SourceLocation to = findLocationAfterTrait(shape, trait.getClass());
            erase(trait.getSourceLocation(), to);
        }

        private SourceLocation findLocationAfterTrait(Shape shape, Class<? extends Trait> target) {
            boolean haveSeenTarget = false;
            List<Trait> traits = new ArrayList<>(shape.getIntroducedTraits().values());
            traits.sort(Comparator.comparing(Trait::getSourceLocation));
            for (Trait trait : traits) {
                if (target.isInstance(trait)) {
                    haveSeenTarget = true;
                } else if (haveSeenTarget && !trait.getSourceLocation().equals(SourceLocation.NONE)) {
                    return trait.getSourceLocation();
                }
            }
            return shape.getSourceLocation();
        }

        private void erase(SourceLocation from, SourceLocation to) {
            IdlAwareSimpleParser parser = new IdlAwareSimpleParser(contents);
            parser.rewind(from);
            int fromPosition = parser.position();
            parser.rewind(to);
            int toPosition = parser.position();
            contents = contents.substring(0, fromPosition) + contents.substring(toPosition);
        }

        private void replace(int from, int to, String with) {
            contents = contents.substring(0, from) + with + contents.substring(to);
        }
    }
}
