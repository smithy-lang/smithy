/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.ColorTheme;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.SetShape;
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
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResultException;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SimpleParser;
import software.amazon.smithy.utils.StringUtils;

@SuppressWarnings("deprecation")
final class MigrateCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(MigrateCommand.class.getName());
    private static final Pattern VERSION_1 = Pattern.compile("(?m)^\\s*\\$\\s*version:\\s*\"1(\\.0)?\"\\s*$");
    private static final Pattern VERSION_2 = Pattern.compile("(?m)^\\s*\\$\\s*version:\\s*\"2(\\.0)?\"\\s*$");
    private final String parentCommandName;

    MigrateCommand(String parentCommandName) {
        this.parentCommandName = parentCommandName;
    }

    static Command createDeprecatedAlias(Command command) {
        return new Command() {
            @Override
            public String getName() {
                return "upgrade-1-to-2";
            }

            @Override
            public String getSummary() {
                return command.getSummary();
            }

            @Override
            public int execute(Arguments arguments, Env env) {
                if (!arguments.getReceiver(StandardOptions.class).quiet()) {
                    env.colors().style(env.stderr(), "upgrade-1-to-2 is deprecated. Use the migrate command instead."
                                                     + System.lineSeparator(), ColorTheme.DEPRECATED);
                    env.stderr().flush();
                }
                return command.execute(arguments, env);
            }

            @Override
            public boolean isHidden() {
                return true;
            }
        };
    }

    @Override
    public String getName() {
        return "migrate";
    }

    @Override
    public String getSummary() {
        return "Migrate Smithy IDL models from 1.0 to 2.0 in place.";
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        arguments.addReceiver(new ConfigOptions());
        arguments.addReceiver(new BuildOptions());

        CommandAction action = HelpActionWrapper.fromCommand(
                this, parentCommandName, this::run);

        return action.apply(arguments, env);
    }

    private int run(Arguments arguments, Env env) {
        List<String> models = arguments.getPositional();
        ClassLoader classLoader = env.classLoader();
        ConfigOptions configOptions = arguments.getReceiver(ConfigOptions.class);
        SmithyBuildConfig smithyBuildConfig = configOptions.createSmithyBuildConfig();

        // Set an output into a temporary directory - we don't actually care about
        // the serialized output.
        SmithyBuildConfig.Builder configBuilder = smithyBuildConfig.toBuilder();
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("smithyMigrate");
        } catch (IOException e) {
            throw new CliError("Unable to create temporary working directory: " + e);
        }
        configBuilder.outputDirectory(tempDir.toString());
        SmithyBuildConfig temporaryConfig = configBuilder.build();

        Model initialModel = new ModelBuilder()
                .config(smithyBuildConfig)
                .arguments(arguments)
                .env(env)
                .models(models)
                .validationPrinter(env.stderr())
                .defaultSeverity(Severity.DANGER)
                .build();

        SmithyBuild smithyBuild = SmithyBuild.create(classLoader)
                .config(temporaryConfig)
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

        List<Path> resolvedModelFiles = resolveModelFiles(finalizedModel, models);

        // Validate upgraded models before writing
        ModelAssembler assembler = ModelBuilder.createModelAssembler(classLoader);
        smithyBuildConfig.getImports().forEach(assembler::addImport);

        List<Pair<Path, String>> upgradedModels = new ArrayList<>();
        for (Path modelFilePath : resolvedModelFiles) {
            String upgradedModelString = upgradeFile(finalizedModel, modelFilePath);
            upgradedModels.add(Pair.of(modelFilePath, upgradedModelString));
            // Replace existing models with upgraded models for a Smithy IDL model file
            assembler.addUnparsedModel(modelFilePath.toAbsolutePath().toString(), upgradedModelString);
        }


        try {
            assembler.assemble().validate();
        } catch (ValidatedResultException e) {
            throw new RuntimeException("Upgraded Smithy models are invalid. "
                    + "Please report the following errors to Smithy team.\n"
                    + e.getMessage());
        }

        for (Pair<Path, String> upgradedModel : upgradedModels) {
            writeMigratedFile(upgradedModel.right, upgradedModel.left);
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

    private void writeMigratedFile(String upgradeFileString, Path filePath) {
        try {
            Files.write(filePath, upgradeFileString.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new CliError(format("Unable to write migrated model file to %s: %s", filePath, e));
        }
    }

    String upgradeFile(Model completeModel, Path filePath) {
        String contents = IoUtils.readUtf8File(filePath);
        if (VERSION_2.matcher(contents).find()) {
            return contents;
        }

        ShapeMigrateVisitor visitor = new ShapeMigrateVisitor(completeModel, contents);

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

    private static class ShapeMigrateVisitor extends ShapeVisitor.Default<Void> {
        private final Model completeModel;
        private final ModelWriter writer;

        ShapeMigrateVisitor(Model completeModel, String modelString) {
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
            } else if (hasSyntheticDefault(shape)) {
                addDefault(shape, shape.getType());
            }
            // Handle members in reverse definition order.
            shape.members().stream()
                    .sorted(Comparator.comparing(Shape::getSourceLocation).reversed())
                    .forEach(this::handleMemberShape);
            return null;
        }

        private void handleMemberShape(MemberShape shape) {
            if (hasSyntheticDefault(shape)) {
                addDefault(shape, completeModel.expectShape(shape.getTarget()).getType());
            }

            if (shape.hasTrait(BoxTrait.class)) {
                writer.eraseTrait(shape, shape.expectTrait(BoxTrait.class));
            }
        }

        private boolean hasSyntheticDefault(Shape shape) {
            Optional<SourceLocation> defaultLocation = shape.getTrait(DefaultTrait.class)
                    .map(Trait::getSourceLocation);
            // When Smithy injects the default trait, it sets the source
            // location equal to the shape's source location. This is
            // impossible in any other scenario, so we can use this info
            // to know whether it was injected or not.
            return defaultLocation.filter(location -> shape.getSourceLocation().equals(location)).isPresent();
        }

        private void addDefault(Shape shape, ShapeType targetType) {
            SourceLocation memberLocation = shape.getSourceLocation();
            String padding = "";
            if (memberLocation.getColumn() > 1) {
                padding = StringUtils.repeat(' ', memberLocation.getColumn() - 1);
            }
            String defaultValue = "";
            // Boxed members get a null default.
            if (shape.hasTrait(BoxTrait.class)) {
                defaultValue = "null";
            } else {
                switch (targetType) {
                    case BOOLEAN:
                        defaultValue = "false";
                        break;
                    case BYTE:
                    case SHORT:
                    case INTEGER:
                    case LONG:
                    case FLOAT:
                    case DOUBLE:
                        defaultValue = "0";
                        break;
                    case BLOB:
                    case STRING:
                        defaultValue = "\"\"";
                        break;
                    default:
                        throw new UnsupportedOperationException("Unexpected default: " + targetType);
                }
            }
            writer.insertLine(shape.getSourceLocation().getLine(), padding + "@default(" + defaultValue + ")");
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

        @Override
        public Void setShape(SetShape shape) {
            getDefault(shape);
            writer.erase(shape.getSourceLocation(), 3); // `set` has 3 characters
            writer.insert(shape.getSourceLocation(), "@uniqueItems" + System.lineSeparator() + "list");
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

        private void insert(SourceLocation from, String value) {
            IdlAwareSimpleParser parser = new IdlAwareSimpleParser(contents);
            parser.rewind(from);
            int fromPosition = parser.position();
            contents = contents.substring(0, fromPosition) + value + contents.substring(fromPosition);
        }

        private void eraseLine(int lineNumber) {
            List<String> lines = new ArrayList<>(Arrays.asList(contents.split("\\r?\\n")));
            lines.remove(lineNumber - 1);
            contents = String.join(System.lineSeparator(), lines);
        }

        private void eraseTrait(Shape shape, Trait trait) {
            if (trait.getSourceLocation() != SourceLocation.NONE) {
                SourceLocation to = findLocationAfterTrait(shape, trait.getClass());
                erase(trait.getSourceLocation(), to);
            }
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

        private void erase(SourceLocation from, int size) {
            IdlAwareSimpleParser parser = new IdlAwareSimpleParser(contents);
            parser.rewind(from);
            int fromPosition = parser.position();
            SourceLocation to = new SourceLocation(from.getFilename(), from.getLine(), from.getColumn() + size);
            parser.rewind(to);
            int toPosition = parser.position();
            contents = contents.substring(0, fromPosition) + contents.substring(toPosition);
        }

        private void replace(int from, int to, String with) {
            contents = contents.substring(0, from) + with + contents.substring(to);
        }
    }
}
