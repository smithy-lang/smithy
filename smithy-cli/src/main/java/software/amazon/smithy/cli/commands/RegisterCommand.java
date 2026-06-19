/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import software.amazon.smithy.build.model.MavenConfig;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.dependencies.DependencyResolver;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * EXPERIMENTAL: {@code smithy register} registers (builds) a Smithy service so it can later be
 * invoked by name with {@code smithy call}.
 *
 * <p>Registration is a build step: it assembles and validates the model once, then writes the
 * combined/derived artifacts under {@code ~/.config/smithy/}. {@code smithy call} consumes those
 * artifacts and automatically rebuilds them from the captured registration context when their source
 * inputs are newer. It does not accept new models or dependencies at call time.
 *
 * <p>Models are taken as positional arguments and loaded through the same stack as {@code validate},
 * {@code select}, and {@code ast}: a {@code smithy-build.json} (auto-detected or via {@code --config})
 * supplies extra sources/imports and Maven dependencies, and {@code --allow-unknown-traits} is honored.
 * The fully resolved build context (absolute model paths, absolute config paths, dependency coordinates,
 * and the {@code --aut} choice) is baked into the registration so it is reproducible from any directory.
 *
 * <pre>{@code
 * smithy register --name s3 model/ --service com.amazonaws.s3#AmazonS3 --aws-region us-east-1
 * smithy register --name custom service.smithy -c smithy-build.json   # deps from config maven
 * smithy register --list
 * smithy register --remove s3
 * }</pre>
 */
final class RegisterCommand implements Command {

    // Keep every line <= 80 chars: the help renderer word-wraps longer lines, which would
    // break commands mid-token and spoil copy-paste.
    private static final String EXAMPLES =
            "Examples:\n"
                    + "  # Register a single-file model (flags first, model path last):\n"
                    + "  smithy register --name s3 --aws-region us-east-1 s3-2006-03-01.json\n"
                    + "\n"
                    + "  # Pin the service shape when a model defines more than one:\n"
                    + "  smithy register --name s3 --service com.amazonaws.s3#AmazonS3 ./models/\n"
                    + "\n"
                    + "  # Load sources + Maven deps (e.g. custom traits) from smithy-build.json:\n"
                    + "  smithy register --name custom -c smithy-build.json --aut\n"
                    + "\n"
                    + "  # Edit an existing registration in place (only given fields change):\n"
                    + "  smithy register --name s3 --edit --aws-region us-west-2\n"
                    + "\n"
                    + "  # Inspect and remove registrations:\n"
                    + "  smithy register --list\n"
                    + "  smithy register --remove s3\n"
                    + "\n"
                    + "Then call it: smithy call s3"
                    + " ListBuckets  (see `smithy call --help`).";

    private final String parentCommandName;
    private final DependencyResolver.Factory dependencyResolverFactory;

    RegisterCommand(String parentCommandName, DependencyResolver.Factory dependencyResolverFactory) {
        this.parentCommandName = parentCommandName;
        this.dependencyResolverFactory = dependencyResolverFactory;
    }

    @Override
    public String getName() {
        return "register";
    }

    @Override
    public String getSummary() {
        return "Registers a Smithy service so it can be invoked with `smithy call` (experimental).";
    }

    // ===================================================================================
    // Options (registration metadata; models/config/aut come from BuildOptions + ConfigOptions)
    // ===================================================================================

    private static final class Options implements ArgumentReceiver {
        private String name;
        private String service;
        private String url;
        private String auth;
        private String awsRegion;
        private boolean pretty;
        private String query;
        private boolean list;
        private boolean edit;
        private String removeName;

        @Override
        public boolean testOption(String name) {
            switch (name) {
                case "--pretty":
                case "-p":
                    pretty = true;
                    return true;
                case "--list":
                    list = true;
                    return true;
                case "--edit":
                    edit = true;
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public Consumer<String> testParameter(String name) {
            switch (name) {
                case "--name":
                    return value -> this.name = value;
                case "--service":
                    return value -> service = value;
                case "--url":
                    return value -> url = value;
                case "--auth":
                    return value -> auth = value;
                case "--aws-region":
                    return value -> awsRegion = value;
                case "-q":
                case "--query":
                    return value -> query = value;
                case "--remove":
                    return value -> removeName = value;
                default:
                    return null;
            }
        }

        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.param("--name", null, "NAME", "Name to register the service under (used later by `smithy call`).");
            printer.param("--service", null, "SERVICE", "Service shape ID to register (required if model has many).");
            printer.param("--url",
                    null,
                    "URL",
                    "Static endpoint URL baked into the registration (overrides the model's endpoint rules).");
            printer.param("--auth", null, "AUTH", "Auth mode baked into the registration (e.g. sigv4, none).");
            printer.param("--aws-region", null, "REGION", "Default AWS region baked into the registration.");
            printer.option("--pretty", "-p", "Pretty-print JSON output.");
            printer.param("--query", "-q", "JMESPATH", "Filter the JSON output with a JMESPath expression.");
            printer.option("--list",
                    null,
                    "List registered services as JSON (optionally with --name to scope to one).");
            printer.option("--edit",
                    null,
                    "Update the existing registration named by --name: options/models you provide override the "
                            + "stored values, anything omitted is kept. Fails if --name isn't already registered.");
            printer.param("--remove", null, "NAME", "Remove a registered service.");
        }
    }

    // ===================================================================================
    // Dispatch
    // ===================================================================================

    @Override
    public int execute(Arguments arguments, Env env) {
        Options options = new Options();
        arguments.addReceiver(new ConfigOptions());
        arguments.addReceiver(new BuildOptions());
        arguments.addReceiver(options);

        // --list/--remove don't load a model, so they must not go through ClasspathAction (which would
        // resolve config + Maven deps from the cwd). Register does: ClasspathAction resolves the config's
        // dependencies into an isolated classloader, then invokes runRegister with the loaded config.
        ClasspathAction registerAction = new ClasspathAction(dependencyResolverFactory, this::runRegister);
        CommandAction action = HelpActionWrapper.fromCommand(this, parentCommandName, colors -> EXAMPLES, (args, e) -> {
            try {
                if (options.list) {
                    return listServices(options, e);
                }
                if (options.removeName != null) {
                    return removeService(options, e);
                }
                if (options.edit) {
                    // --edit merges onto a stored profile; the baked config is authoritative, so it must
                    // NOT go through ClasspathAction (which would resolve a cwd smithy-build.json). It
                    // resolves its own classloader from the merged dependency set instead.
                    return runEdit(args, e);
                }
                return registerAction.apply(args, e);
            } catch (CliError ce) {
                return CallArtifacts.errorJson(e, options.pretty, "validation", ce.getMessage());
            } catch (RuntimeException re) {
                return CallArtifacts.errorJson(e, options.pretty, CallArtifacts.classify(re), re.getMessage());
            }
        });
        return action.apply(arguments, env);
    }

    // ===================================================================================
    // Register
    // ===================================================================================

    private int runRegister(SmithyBuildConfig config, Arguments arguments, Env env) {
        Options options = arguments.getReceiver(Options.class);
        if (options.name == null) {
            throw new CliError("Missing required --name. Usage: smithy register --name <name> <models...>");
        }

        // Positional models -> absolute paths (the registration must be directory-independent).
        List<String> models = arguments.getPositional();
        if (models.isEmpty() && config.getSources().isEmpty()) {
            throw new CliError("register requires at least one positional <MODELS> path "
                    + "(or sources declared in smithy-build.json).");
        }
        List<String> sources = new ArrayList<>(models.size());
        for (String model : models) {
            Path p = Path.of(model);
            if (!Files.exists(p)) {
                throw new CliError("Model path does not exist: " + model);
            }
            sources.add(p.toAbsolutePath().normalize().toString());
        }

        // Resolved config paths -> absolute, baked in so recompile reloads the same smithy-build.json
        // regardless of the directory `call` later runs from.
        List<String> configs = new ArrayList<>();
        for (String c : arguments.getReceiver(ConfigOptions.class).config()) {
            configs.add(Path.of(c).toAbsolutePath().normalize().toString());
        }

        // Maven dependency coordinates from the config; persisted for call-time SPI + recompile.
        List<String> dependencies = config.getMaven()
                .map(MavenConfig::getDependencies)
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);

        boolean allowUnknownTraits = arguments.getReceiver(BuildOptions.class).allowUnknownTraits();

        // env.classLoader() already includes the resolved Maven deps (ClasspathAction set it up), so the
        // build sees SPI-discovered custom traits/protocols. Both register and call's recompile go through
        // CallArtifacts.buildArtifacts with the same BuildContext, so they can't diverge.
        CallArtifacts.BuildContext ctx =
                new CallArtifacts.BuildContext(sources, configs, options.service, allowUnknownTraits);
        long compiledAt = CallArtifacts.buildArtifacts(options.name, ctx, env.classLoader());

        CallProfiles.Profile profile = new CallProfiles.Profile(
                sources,
                configs,
                options.service,
                dependencies,
                allowUnknownTraits,
                options.awsRegion,
                options.url,
                options.auth,
                compiledAt);
        CallProfiles.save(options.name, profile);

        Map<String, Document> out = new LinkedHashMap<>();
        out.put("registered", Document.of(options.name));
        out.put("profile", profile.toNodeDocument());
        out.put("artifacts", Document.of(CallProfiles.artifactDir(options.name).toString()));
        CallArtifacts.print(env, options.pretty, options.query, Document.of(out));
        return 0;
    }

    /**
     * Updates an existing registration: options/models the user provided override the stored values,
     * everything omitted is kept. Rebuilds the derived artifacts only when a build-affecting input
     * changed (models, configs, service, or allow-unknown-traits) or an artifact is missing; a
     * pure-metadata edit (url/auth/region) skips the rebuild, so it works even if the source model files
     * have since moved. Maven dependencies are re-derived whenever the config set changes.
     */
    private int runEdit(Arguments arguments, Env env) {
        Options options = arguments.getReceiver(Options.class);
        if (options.name == null) {
            throw new CliError("Missing required --name. Usage: smithy register --name <name> --edit [overrides...]");
        }
        CallProfiles.Profile existing = CallProfiles.load(options.name)
                .orElseThrow(() -> new CliError(
                        "'" + options.name + "' is not registered, so there is nothing to edit. Register it first with "
                                + "`smithy register --name " + options.name + " <model> ...`."));

        // Provided => override; omitted => keep. Models and configs are "provided" when non-empty.
        List<String> models = arguments.getPositional();
        List<String> sources = existing.sources;
        if (!models.isEmpty()) {
            sources = new ArrayList<>(models.size());
            for (String model : models) {
                Path p = Path.of(model);
                if (!Files.exists(p)) {
                    throw new CliError("Model path does not exist: " + model);
                }
                sources.add(p.toAbsolutePath().normalize().toString());
            }
        }

        List<String> providedConfigs = arguments.getReceiver(ConfigOptions.class).config();
        List<String> configs = existing.configs;
        boolean configsChanged = false;
        if (!providedConfigs.isEmpty()) {
            configs = new ArrayList<>(providedConfigs.size());
            for (String c : providedConfigs) {
                configs.add(Path.of(c).toAbsolutePath().normalize().toString());
            }
            configsChanged = !configs.equals(existing.configs);
        }

        // --aut has no off-switch anywhere, so in edit mode it can only turn the flag on (or keep it).
        boolean allowUnknownTraits = existing.allowUnknownTraits
                || arguments.getReceiver(BuildOptions.class).allowUnknownTraits();

        String service = options.service != null ? options.service : existing.service;
        String url = options.url != null ? options.url : existing.url;
        String auth = options.auth != null ? options.auth : existing.auth;
        String region = options.awsRegion != null ? options.awsRegion : existing.region;

        // Re-derive Maven deps from the (possibly new) config set; keep stored deps if configs unchanged.
        List<String> dependencies = configsChanged
                ? CallArtifacts.dependenciesFromConfigs(configs)
                : existing.dependencies;

        // Rebuild artifacts only if a build-affecting input changed or an artifact is missing.
        boolean modelsChanged = !sources.equals(existing.sources);
        boolean serviceChanged = !java.util.Objects.equals(service, existing.service);
        boolean autChanged = allowUnknownTraits != existing.allowUnknownTraits;
        boolean artifactsMissing = !Files.isRegularFile(CallProfiles.modelNoDocsArtifact(options.name))
                || !Files.isRegularFile(CallProfiles.modelArtifact(options.name));
        boolean rebuild = modelsChanged || configsChanged || serviceChanged || autChanged || artifactsMissing;

        long compiledAt = existing.compiledAt;
        if (rebuild) {
            CallArtifacts.BuildContext ctx =
                    new CallArtifacts.BuildContext(sources, configs, service, allowUnknownTraits);
            ClassLoader cl = env.classLoader();
            if (!dependencies.isEmpty()) {
                // Build inside an isolated classloader so SPI-discovered custom traits/protocols resolve.
                cl = newDependencyClassLoader(dependencies, cl);
            }
            compiledAt = CallArtifacts.buildArtifacts(options.name, ctx, cl);
        }

        CallProfiles.Profile updated = new CallProfiles.Profile(
                sources,
                configs,
                service,
                dependencies,
                allowUnknownTraits,
                region,
                url,
                auth,
                compiledAt);
        CallProfiles.save(options.name, updated);

        Map<String, Document> out = new LinkedHashMap<>();
        out.put("edited", Document.of(options.name));
        out.put("rebuilt", Document.of(rebuild));
        out.put("profile", updated.toNodeDocument());
        out.put("artifacts", Document.of(CallProfiles.artifactDir(options.name).toString()));
        CallArtifacts.print(env, options.pretty, options.query, Document.of(out));
        return 0;
    }

    /** Resolves Maven deps to a child classloader for a headless (non-ClasspathAction) build. */
    private static ClassLoader newDependencyClassLoader(List<String> dependencies, ClassLoader parent) {
        java.util.List<java.nio.file.Path> jars = CallArtifacts.resolveDependencyJars(dependencies);
        java.net.URL[] urls = new java.net.URL[jars.size()];
        for (int i = 0; i < jars.size(); i++) {
            try {
                urls[i] = jars.get(i).toUri().toURL();
            } catch (java.net.MalformedURLException e) {
                throw new CliError("Could not load dependency jar: " + jars.get(i));
            }
        }
        return new java.net.URLClassLoader(urls, parent);
    }

    private int removeService(Options options, Env env) {
        if (options.removeName == null) {
            throw new CliError("--remove requires a service name.");
        }
        boolean existed = CallProfiles.remove(options.removeName);
        Map<String, Document> out = new LinkedHashMap<>();
        out.put("removed", Document.of(existed));
        out.put("name", Document.of(options.removeName));
        CallArtifacts.print(env, options.pretty, options.query, Document.of(out));
        return existed ? 0 : 1;
    }

    private int listServices(Options options, Env env) {
        Map<String, Document> services = new LinkedHashMap<>();
        String only = options.name;
        if (only != null) {
            // `--list --name <name>`: scope to one registration; error if it isn't registered.
            CallProfiles.Profile p = CallProfiles.load(only)
                    .orElseThrow(() -> new CliError(
                            "'" + only + "' is not a registered service. Use `smithy register --list` to see all."));
            services.put(only, p.toNodeDocument());
        } else {
            CallProfiles.loadAllProfiles()
                    .ifPresent(profiles -> profiles.forEach((name, p) -> services.put(name, p.toNodeDocument())));
        }
        Map<String, Document> out = new LinkedHashMap<>();
        out.put("services", Document.of(services));
        out.put("path", Document.of(CallProfiles.configPath().toString()));
        CallArtifacts.print(env, options.pretty, options.query, Document.of(out));
        return 0;
    }
}
