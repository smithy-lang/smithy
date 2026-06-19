/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.EnvironmentVariable;
import software.amazon.smithy.cli.SmithyCli;
import software.amazon.smithy.cli.dependencies.DependencyResolver;
import software.amazon.smithy.cli.dependencies.FilterCliVersionResolver;
import software.amazon.smithy.cli.dependencies.MavenDependencyResolver;
import software.amazon.smithy.cli.dependencies.ResolvedArtifact;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.jmespath.JmesPathQueries;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.java.rulesengine.Bytecode;
import software.amazon.smithy.java.rulesengine.RulesEngineBuilder;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.ExamplesTrait;
import software.amazon.smithy.model.traits.ExternalDocumentationTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;

/**
 * Shared building blocks for the {@code smithy call} and {@code smithy register} commands: model
 * assembly/artifact building (so registration and call-time recompilation can't diverge), service and
 * operation resolution, and the single JSON output chokepoint. All members are package-private statics;
 * the two commands hold no copies of this logic.
 */
final class CallArtifacts {

    private static final java.util.logging.Logger LOGGER_WATCH =
            java.util.logging.Logger.getLogger(CallArtifacts.class.getName());

    private CallArtifacts() {}

    // Codec for CLI input/output documents. Matches the AWS rpcv2-json protocol's JSON settings
    // (arbitrary-precision numbers as strings) so the document we parse round-trips like an AWS JSON
    // payload. The dynamic client still serializes onto the wire using the target service's own
    // protocol; this codec only governs how we read --input and render the JSON envelope.
    static final JsonCodec CODEC = JsonCodec.builder()
            .useStringForArbitraryPrecision(true)
            .build();

    // ===================================================================================
    // Artifact build (registration + call-time recompile)
    // ===================================================================================

    /**
     * The fully-resolved, directory-independent build inputs captured at registration time and replayed
     * verbatim on a stale recompile. Everything is an absolute path or coordinate, so the build produces
     * the same artifacts regardless of the working directory it runs from.
     */
    static final class BuildContext {
        final List<String> sources; // absolute model files/dirs
        final List<String> configs; // absolute smithy-build.json paths (may be empty)
        final String serviceOverride; // explicit service shape ID, or null
        final boolean allowUnknownTraits;

        BuildContext(
                List<String> sources,
                List<String> configs,
                String serviceOverride,
                boolean allowUnknownTraits
        ) {
            this.sources = sources;
            this.configs = configs;
            this.serviceOverride = serviceOverride;
            this.allowUnknownTraits = allowUnknownTraits;
        }
    }

    /**
     * Assembles + validates the model from a {@link BuildContext}, then writes the derived artifacts: a
     * combined {@code model.json} (with docs, for help), {@code model-no-docs.json} (for calls), and a
     * precompiled {@code endpoint.bdd} when the service defines endpoint rules. The {@code classLoader}
     * must already include any Maven dependencies (resolved via {@link #resolveDependencyJars}) so that
     * SPI-discovered traits/protocols are available. Returns the build timestamp (latest source/config
     * mtime). Used by both {@code register} and {@code call}'s stale-recompile, so they can't diverge.
     */
    static long buildArtifacts(String name, BuildContext ctx, ClassLoader classLoader) {
        // Load the baked smithy-build.json config(s), if any. Re-read here (not via ClasspathAction) so
        // the build is headless and directory-independent. A baked config that has since moved/been
        // deleted is a hard error: the registration named it, so silently dropping it would build a
        // different model than the user registered.
        SmithyBuildConfig config = loadConfigs(ctx.configs);

        // discoverModels brings in bundled trait definitions plus anything on the (dependency-augmented)
        // classloader. allowUnknownTraits mirrors the --aut choice recorded at registration.
        ModelAssembler assembler = Model.assembler(classLoader).discoverModels(classLoader);
        if (ctx.allowUnknownTraits) {
            assembler.putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true);
        }
        if (config != null) {
            config.getSources().forEach(assembler::addImport);
            config.getImports().forEach(assembler::addImport);
        }
        for (String s : ctx.sources) {
            assembler.addImport(s);
        }
        Model model = assembler.assemble().unwrap();

        // Resolve the service now so registration fails fast if it's missing/ambiguous.
        ShapeId serviceId = ctx.serviceOverride != null
                ? resolveServiceByName(model, ctx.serviceOverride)
                : resolveSoleService(model);

        Path dir = CallProfiles.artifactDir(name);
        try {
            Files.createDirectories(dir);

            // Combined model with docs (self-contained AST; reloaded without re-discovery for help).
            ModelSerializer withDocs = ModelSerializer.builder().build();
            CallProfiles.writeArtifact(CallProfiles.modelArtifact(name),
                    Node.prettyPrintJson(withDocs.serialize(model)).getBytes(StandardCharsets.UTF_8));

            // Combined model without documentation traits, for calls.
            Model noDocs = ModelTransformer.create()
                    .removeTraitsIf(model,
                            (shape, trait) -> trait.toShapeId().equals(DocumentationTrait.ID)
                                    || trait.toShapeId().equals(ExternalDocumentationTrait.ID)
                                    || trait.toShapeId().equals(ExamplesTrait.ID));
            ModelSerializer noDocsSer = ModelSerializer.builder().build();
            CallProfiles.writeArtifact(CallProfiles.modelNoDocsArtifact(name),
                    Node.printJson(noDocsSer.serialize(noDocs)).getBytes(StandardCharsets.UTF_8));

            // Precompile endpoint rules (BDD or decision-tree) to reusable bytecode if present.
            ServiceShape service = model.expectShape(serviceId, ServiceShape.class);
            Bytecode bytecode = compileEndpointRules(service);
            if (bytecode != null) {
                CallProfiles.writeArtifact(CallProfiles.bytecodeArtifact(name), bytecode.getBytecode());
            } else {
                Files.deleteIfExists(CallProfiles.bytecodeArtifact(name));
            }
        } catch (IOException e) {
            throw new CliError("Unable to write registration artifacts for " + name + ": " + e.getMessage());
        }

        return latestSourceMtime(watchedInputs(ctx));
    }

    /** Loads and merges the baked smithy-build.json config paths, or null if none. Hard-errors if absent. */
    private static SmithyBuildConfig loadConfigs(List<String> configs) {
        if (configs.isEmpty()) {
            return null;
        }
        SmithyBuildConfig.Builder builder = SmithyBuildConfig.builder();
        for (String c : configs) {
            Path p = Path.of(c);
            if (!Files.isRegularFile(p)) {
                throw new CliError("Registered config file no longer exists: " + c
                        + ". Re-run `smithy register` to update the registration.");
            }
            builder.load(p);
        }
        return builder.build();
    }

    /** The Maven dependency coordinates declared in the baked config(s), in declaration order. */
    static List<String> dependenciesFromConfigs(List<String> configs) {
        SmithyBuildConfig config = loadConfigs(configs);
        if (config == null) {
            return List.of();
        }
        return config.getMaven().map(m -> new ArrayList<>(m.getDependencies())).orElseGet(ArrayList::new);
    }

    /**
     * All files whose mtime gates staleness: the positional source models, the config files themselves,
     * and the models the config contributes (its resolved {@code sources}/{@code imports}). Without the
     * last group, editing a model that's only referenced from {@code smithy-build.json} would never mark
     * the registration stale. Tolerant of a missing config here (returns what it can) so that staleness
     * can still trigger a rebuild -- the rebuild then raises the authoritative hard error.
     */
    static List<String> watchedInputs(BuildContext ctx) {
        List<String> inputs = new ArrayList<>();
        inputs.addAll(ctx.sources);
        inputs.addAll(ctx.configs);
        for (String c : ctx.configs) {
            Path p = Path.of(c);
            if (!Files.isRegularFile(p)) {
                continue; // missing config: let buildArtifacts raise the hard error
            }
            try {
                SmithyBuildConfig config = SmithyBuildConfig.builder().load(p).build();
                inputs.addAll(config.getSources());
                inputs.addAll(config.getImports());
            } catch (RuntimeException e) {
                LOGGER_WATCH.fine(() -> "Could not read config sources for staleness check: " + e.getMessage());
            }
        }
        return inputs;
    }

    /**
     * Resolves Maven dependency coordinates to local jar paths so they can be loaded into an isolated
     * classloader (for SPI-discovered traits/protocols/auth schemes). Shared by registration (build the
     * artifacts with custom traits available) and calling (load custom protocols/auth at call time).
     */
    static List<Path> resolveDependencyJars(Collection<String> dependencies) {
        DependencyResolver resolver = new FilterCliVersionResolver(SmithyCli.getVersion(),
                new MavenDependencyResolver(EnvironmentVariable.SMITHY_MAVEN_CACHE.get()));
        resolver.addRepository(MavenRepository.builder().url("https://repo.maven.apache.org/maven2").build());
        for (String coordinates : dependencies) {
            resolver.addDependency(coordinates);
        }
        List<Path> jars = new ArrayList<>();
        for (ResolvedArtifact artifact : resolver.resolve()) {
            jars.add(artifact.getPath());
        }
        return jars;
    }

    /** Compiles a service's endpoint rules to bytecode, or null if it defines none. */
    private static Bytecode compileEndpointRules(ServiceShape service) {
        RulesEngineBuilder engine = new RulesEngineBuilder();
        if (service.hasTrait(EndpointBddTrait.class)) {
            return engine.compile(service.expectTrait(EndpointBddTrait.class));
        }
        if (service.hasTrait(EndpointRuleSetTrait.class)) {
            return engine.compile(service.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet());
        }
        return null;
    }

    static long latestSourceMtime(List<String> sources) {
        long latest = 0;
        for (String s : sources) {
            latest = Math.max(latest, mtimeOf(Path.of(s)));
        }
        return latest;
    }

    private static long mtimeOf(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (var walk = Files.walk(path)) {
                    return walk.filter(Files::isRegularFile)
                            .mapToLong(CallArtifacts::fileMtime)
                            .max()
                            .orElse(0);
                }
            }
            return fileMtime(path);
        } catch (IOException e) {
            return 0;
        }
    }

    private static long fileMtime(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    // ===================================================================================
    // Model loading (call/help consumers -- validation disabled, no re-discovery)
    // ===================================================================================

    /**
     * Loads a pre-built combined model artifact. Validation is disabled and models are NOT
     * re-discovered: the artifact is a self-contained AST written at registration time, so trait
     * definitions are already inlined and re-discovery would duplicate them.
     */
    static Model loadArtifactModel(Path artifact, ClassLoader cl) {
        return Model.assembler(cl)
                .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                .disableValidation()
                .addImport(artifact)
                .assemble()
                .unwrap();
    }

    // ===================================================================================
    // Service/operation resolution
    // ===================================================================================

    static Set<ShapeId> serviceIds(Model model) {
        Set<ShapeId> ids = new HashSet<>();
        for (ServiceShape shape : model.getServiceShapes()) {
            ids.add(shape.toShapeId());
        }
        return ids;
    }

    static ShapeId resolveSoleService(Model model) {
        Set<ShapeId> ids = serviceIds(model);
        if (ids.isEmpty()) {
            throw new CliError("No service shapes found in the model.");
        }
        if (ids.size() > 1) {
            throw new CliError("The model defines multiple services; register with --service <id>. "
                    + "Available: " + ids);
        }
        return ids.iterator().next();
    }

    static ShapeId resolveServiceByName(Model model, String service) {
        Set<ShapeId> ids = serviceIds(model);
        if (service.contains("#")) {
            ShapeId resolved = ShapeId.from(service);
            if (!ids.contains(resolved)) {
                throw new CliError("Service " + service + " not found. Available: " + ids);
            }
            return resolved;
        }
        ShapeId resolved = null;
        for (ShapeId id : ids) {
            if (id.getName().equals(service)) {
                if (resolved != null) {
                    throw new CliError("Multiple services named " + service + ". Use a full shape ID. "
                            + "Available: " + ids);
                }
                resolved = id;
            }
        }
        if (resolved == null) {
            throw new CliError("Service " + service + " not found. Available: " + ids);
        }
        return resolved;
    }

    static OperationShape resolveOperation(Model model, ShapeId serviceId, String operationName) {
        for (OperationShape op : TopDownIndex.of(model).getContainedOperations(serviceId)) {
            if (op.getId().getName().equals(operationName)) {
                return op;
            }
        }
        throw new CliError("Operation " + operationName + " not found in service " + serviceId.getName()
                + ". Use --help to list operations.");
    }

    // ===================================================================================
    // JSON output
    // ===================================================================================

    /**
     * Serializes a Document to JSON and prints it to stdout. Applies {@code --query} (JMESPath) to
     * filter the document first, and {@code --pretty} to format. This is the single chokepoint for all
     * JSON output, so --query filters every JSON-returning path (calls, plan, list, JSON help).
     */
    static void print(Command.Env env, boolean pretty, String query, Document document) {
        Document out = document;
        if (query != null) {
            try {
                out = JmesPathQueries.query(JmespathExpression.parse(query), document);
            } catch (RuntimeException e) {
                throw new CliError("Invalid --query expression: " + e.getMessage());
            }
            if (out == null) {
                // JMESPath matched nothing -> JSON null.
                env.stdout().println("null");
                return;
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ShapeSerializer serializer = CODEC.createSerializer(baos)) {
            out.serialize(serializer);
        }
        String json = baos.toString(StandardCharsets.UTF_8);
        if (pretty) {
            json = Node.prettyPrintJson(Node.parse(json));
        }
        env.stdout().println(json);
    }

    /** Prints a structured {@code @error} envelope to stdout and returns exit code 1. */
    static int errorJson(Command.Env env, boolean pretty, String type, String message) {
        Map<String, Document> error = new LinkedHashMap<>();
        error.put("type", Document.of(type));
        error.put("message", Document.of(message));
        Map<String, Document> out = new LinkedHashMap<>();
        out.put("@error", Document.of(error));
        // Errors are never filtered by --query; the user needs to see the failure.
        print(env, pretty, null, Document.of(out));
        return 1;
    }

    /** Classifies a runtime exception into a coarse error {@code type} for the error envelope. */
    static String classify(RuntimeException e) {
        String cls = e.getClass().getName().toLowerCase(Locale.ROOT);
        if (cls.contains("model") || cls.contains("validat")) {
            return "model";
        }
        if (cls.contains("identity") || cls.contains("credential")) {
            return "credentials";
        }
        if (cls.contains("transport") || cls.contains("connect") || cls.contains("io")) {
            return "transport";
        }
        return "error";
    }
}
