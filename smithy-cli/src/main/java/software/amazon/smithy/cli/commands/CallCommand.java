/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.java.auth.api.identity.Identity;
import software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity;
import software.amazon.smithy.java.aws.client.auth.scheme.sigv4.SigV4AuthScheme;
import software.amazon.smithy.java.aws.client.auth.scheme.sigv4.SigV4Settings;
import software.amazon.smithy.java.aws.client.core.identity.EnvironmentVariableIdentityResolver;
import software.amazon.smithy.java.aws.client.core.settings.EndpointAuthSchemeSettings;
import software.amazon.smithy.java.aws.client.core.settings.RegionSetting;
import software.amazon.smithy.java.aws.config.AwsProfile;
import software.amazon.smithy.java.aws.config.AwsProfileFile;
import software.amazon.smithy.java.client.core.CallContext;
import software.amazon.smithy.java.client.core.auth.scheme.AuthSchemeResolver;
import software.amazon.smithy.java.client.core.interceptors.ClientInterceptor;
import software.amazon.smithy.java.client.core.interceptors.RequestHook;
import software.amazon.smithy.java.client.core.interceptors.ResponseHook;
import software.amazon.smithy.java.core.error.CallException;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.SerializationException;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.dynamicclient.DocumentException;
import software.amazon.smithy.java.dynamicclient.DynamicClient;
import software.amazon.smithy.java.endpoints.Endpoint;
import software.amazon.smithy.java.endpoints.EndpointResolver;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.java.rulesengine.BddTrace;
import software.amazon.smithy.java.rulesengine.BddTraceSink;
import software.amazon.smithy.java.rulesengine.Bytecode;
import software.amazon.smithy.java.rulesengine.RulesEngineBuilder;
import software.amazon.smithy.java.rulesengine.RulesEngineSettings;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.traits.ContextParamTrait;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.utils.StringUtils;

/**
 * EXPERIMENTAL: {@code smithy call} sends a request to a registered Smithy service.
 *
 * <p>Usage is two-phase. First <strong>register</strong> a service with {@code smithy register}
 * (a build step that assembles and validates the model once, then writes combined/derived artifacts
 * under {@code ~/.config/smithy/}):
 *
 * <pre>{@code
 * smithy register --name s3 s3.json --service com.amazonaws.s3#AmazonS3 --aws-region us-east-1
 * }</pre>
 *
 * Then <strong>call</strong> it by name. Calling normally loads the pre-built artifacts; if registered
 * source/config inputs are newer, it rebuilds those artifacts from the captured registration context.
 * It does not accept additional models or dependencies at call time:
 *
 * <pre>{@code
 * smithy call SERVICE OPERATION --input '{"key":"value"}'
 * smithy call SERVICE OPERATION -i @input.json --pretty
 * echo '{"key":"value"}' | smithy call SERVICE OPERATION -i -
 * smithy call SERVICE OPERATION -i '{"key":"value"}' --plan
 * smithy call SERVICE --help                   # text service help
 * smithy call SERVICE OPERATION --help         # text operation help
 * smithy call SERVICE OPERATION --help --json  # structured operation help
 * }</pre>
 */
final class CallCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(CallCommand.class.getName());
    private static final Logger SMITHY_JAVA_LOGGER = Logger.getLogger("software.amazon.smithy.java");

    static {
        // Use Smithy's own (fast) JSON serde provider rather than Jackson. The provider is selected
        // once in JsonSettings' static initializer from this system property, so it must be set before
        // the call I/O codec is constructed (i.e. before JsonSettings loads).
        if (System.getProperty("smithy-java.json-provider") == null) {
            System.setProperty("smithy-java.json-provider", "smithy");
        }
    }

    private final String parentCommandName;

    CallCommand(String parentCommandName) {
        this.parentCommandName = parentCommandName;
    }

    @Override
    public String getName() {
        return "call";
    }

    @Override
    public String getSummary() {
        return "Calls a registered Smithy service (experimental).";
    }

    // ===================================================================================
    // Dispatch
    // ===================================================================================

    @Override
    public int execute(Arguments arguments, Env env) {
        CallOptions options = new CallOptions();
        arguments.addReceiver(options);

        // Interleaved parsing: a shift() loop keeps offering every token to receivers, so options may
        // appear before or after the positionals. (getPositional() would stop after the first bare arg.)
        List<String> positional = new ArrayList<>();
        String token;
        while ((token = arguments.shift()) != null) {
            if (token.length() > 1 && token.charAt(0) == '-') {
                throw new CliError("Unknown option: " + token);
            }
            positional.add(token);
        }
        // Trigger the CLI's standard logging configuration. It only runs on the first getPositional()
        // call; since this command consumes args via the shift() loop above, we must call it explicitly
        // or the root logger stays at its default level and smithy-java INFO logs leak to stderr.
        arguments.getPositional();

        boolean help = arguments.getReceiver(StandardOptions.class).help();
        boolean debug = arguments.getReceiver(StandardOptions.class).debug();

        // The smithy-java runtime logs through java.util.logging (e.g. EndpointRulesPlugin emits an
        // INFO line each call). For a single-shot CLI that emits structured JSON on stdout, that noise
        // on stderr is unwanted -- restrict the smithy-java namespace to SEVERE (errors only) unless
        // --debug is set. Output stays clean JSON; real errors still surface.
        Level previousSmithyJavaLevel = SMITHY_JAVA_LOGGER.getLevel();
        String previousAwsProfile = System.getProperty("aws.profile");
        if (!debug) {
            SMITHY_JAVA_LOGGER.setLevel(Level.SEVERE);
        }
        if (options.awsProfile != null) {
            System.setProperty("aws.profile", options.awsProfile);
        }

        try {
            return run(arguments, positional, options, help, debug, env);
        } catch (CliError ce) {
            return CallArtifacts.errorJson(env, options.pretty, "validation", ce.getMessage());
        } catch (RuntimeException re) {
            if (debug) {
                LOGGER.log(Level.SEVERE, "smithy call failed", re);
            }
            return CallArtifacts.errorJson(env, options.pretty, CallArtifacts.classify(re), re.getMessage());
        } finally {
            if (!debug) {
                SMITHY_JAVA_LOGGER.setLevel(previousSmithyJavaLevel);
            }
            if (previousAwsProfile == null) {
                System.clearProperty("aws.profile");
            } else {
                System.setProperty("aws.profile", previousAwsProfile);
            }
        }
    }

    private int run(
            Arguments arguments,
            List<String> positional,
            CallOptions options,
            boolean help,
            boolean debug,
            Env env
    ) {
        if (positional.isEmpty()) {
            if (help) {
                // Standard CLI help for `smithy call --help` (no service): render the usual flag help,
                // matching every other command. Service/operation --help (with a positional) stays JSON.
                printStandardHelp(arguments, env);
                return 0;
            }
            throw new CliError("Missing required <service>. Register one with `smithy register "
                    + "--name <name> <model> ...`, then call it by name.");
        }

        String name = positional.get(0);
        String operation = positional.size() > 1 ? positional.get(1) : null;
        if (positional.size() > 2) {
            throw new CliError("Unexpected positional arguments after <service> <operation>: "
                    + positional.subList(2, positional.size()));
        }

        CallProfiles.Profile profile = CallProfiles.load(name)
                .orElseThrow(() -> new CliError(
                        "'" + name + "' is not a registered service. Register it with `smithy register --name "
                                + name + " <model> ...`."));

        if (profile.dependencies.isEmpty()) {
            recompileIfStale(name, profile, env);
            return dispatchWithModel(name, profile, operation, options, help, debug, env);
        }
        // Load the registered Maven dependencies into an isolated classloader so SPI-discovered custom
        // traits/protocols/auth schemes are available, then run BOTH the stale-recompile and the call
        // inside it (recompiling without the custom traits on the classloader could fail to assemble).
        int[] exit = {0};
        new IsolatedRunnable(CallArtifacts.resolveDependencyJars(profile.dependencies), env.classLoader(), cl -> {
            Env scoped = env.withClassLoader(cl);
            recompileIfStale(name, profile, scoped);
            exit[0] = dispatchWithModel(name, profile, operation, options, help, debug, scoped);
        }).run();
        return exit[0];
    }

    private int dispatchWithModel(
            String name,
            CallProfiles.Profile profile,
            String operation,
            CallOptions options,
            boolean help,
            boolean debug,
            Env env
    ) {
        if (help) {
            return helpJson(name, profile, operation, options, env);
        }
        if (operation == null) {
            throw new CliError("Missing required <operation>. Use `smithy call " + name
                    + " --help` to list operations.");
        }
        return doCall(name, profile, operation, options, debug, env);
    }

    /**
     * Rebuilds artifacts if any source or config file is newer than the recorded build, or an artifact
     * is missing. Registration normally happens via {@code smithy register}; this keeps a stale
     * registration's derived artifacts in sync on the next call without forcing a manual re-register.
     * The rebuild replays the baked build context (sources, configs, allowUnknownTraits) headlessly, so
     * it reproduces the original build from any directory. A baked config that has since gone missing is
     * a hard error (raised by {@link CallArtifacts#buildArtifacts}).
     */
    private void recompileIfStale(String name, CallProfiles.Profile profile, Env env) {
        boolean missing = !Files.isRegularFile(CallProfiles.modelNoDocsArtifact(name))
                || !Files.isRegularFile(CallProfiles.modelArtifact(name));
        CallArtifacts.BuildContext ctx = new CallArtifacts.BuildContext(
                profile.sources,
                profile.configs,
                profile.service,
                profile.allowUnknownTraits);
        // A baked config that has gone missing forces a rebuild: it can't be a no-op, because the models
        // that config contributes aren't otherwise visible for the staleness check (we'd silently serve
        // stale artifacts). The forced rebuild then raises the authoritative hard error.
        boolean configMissing = profile.configs.stream().anyMatch(c -> !Files.isRegularFile(Path.of(c)));
        boolean stale = configMissing
                || CallArtifacts.latestSourceMtime(CallArtifacts.watchedInputs(ctx)) > profile.compiledAt;
        if (!missing && !stale) {
            return;
        }
        LOGGER.fine(() -> "Recompiling registration '" + name + "' (missing=" + missing + ", stale=" + stale + ")");
        long compiledAt = CallArtifacts.buildArtifacts(name, ctx, env.classLoader());
        CallProfiles.save(name,
                new CallProfiles.Profile(
                        profile.sources,
                        profile.configs,
                        profile.service,
                        profile.dependencies,
                        profile.allowUnknownTraits,
                        profile.region,
                        profile.url,
                        profile.auth,
                        compiledAt));
    }

    // ===================================================================================
    // Call
    // ===================================================================================

    private int doCall(
            String name,
            CallProfiles.Profile profile,
            String operation,
            CallOptions options,
            boolean debug,
            Env env
    ) {
        Model model = CallArtifacts.loadArtifactModel(CallProfiles.modelNoDocsArtifact(name), env.classLoader());
        ShapeId serviceId = profile.service != null
                ? CallArtifacts.resolveServiceByName(model, profile.service)
                : CallArtifacts.resolveSoleService(model);
        OperationShape opShape = CallArtifacts.resolveOperation(model, serviceId, operation);
        String opName = opShape.getId().getName();

        if ("-".equals(options.input) && "-".equals(options.inputPayload)) {
            throw new CliError("--input and --input-payload cannot both read from stdin (-).");
        }

        String region = resolveRegion(options, profile);

        // --continue carries the full original input (verbatim), the region it ran against, and the
        // next-page token. It is the sole source of input, so it cannot be combined with --input.
        Document input;
        if (options.continueToken != null) {
            if (options.input != null || options.inputPayload != null) {
                throw new CliError("--continue cannot be combined with --input or --input-payload "
                        + "(the token already carries the input).");
            }
            CallPagination.Decoded decoded = CallPagination.decode(
                    options.continueToken,
                    name,
                    serviceId.toString(),
                    opName);
            input = decoded.input;
            // Pagination is endpoint-scoped: reuse the region the first page ran against so later pages
            // hit the same endpoint. An explicit --aws-region still overrides (escape hatch).
            if (options.awsRegion == null && decoded.region != null) {
                region = decoded.region;
            }
            if (options.url == null && decoded.url != null) {
                options.url = decoded.url;
            }
            if (options.protocol == null && decoded.protocol != null) {
                options.protocol = decoded.protocol;
            }
        } else {
            input = CallIo.readInput(options.input);
        }

        // Streaming input payload (e.g. S3 PutObject Body): inject the file/stdin as a DataStream under
        // the operation's @streaming input member. --input supplies the other (non-payload) fields.
        String streamingInput = CallIo.streamingMember(opShape, model, true);
        if (options.inputPayload != null) {
            if (streamingInput == null) {
                throw new CliError(opName + " has no streaming input member; --input-payload is not applicable.");
            }
            input = CallIo.withStreamingPayload(input, streamingInput, options.inputPayload);
        }

        // Validate the --wire mode early.
        String wire = options.wire;
        if (wire != null && !wire.equals("headers") && !wire.equals("full")) {
            throw new CliError("--wire must be 'headers' or 'full' (got '" + wire + "').");
        }
        boolean wireFull = "full".equals(wire);

        // --bdd-trace only makes sense alongside --plan (it explains the endpoint resolution that --plan
        // performs without sending the request).
        if (options.bddTrace && !options.plan) {
            throw new CliError("--bdd-trace requires --plan.");
        }

        boolean inputStreaming = streamingInput != null;
        boolean outputStreaming = CallIo.streamingMember(opShape, model, false) != null;

        WireCapture capture = new WireCapture(options.plan,
                wireFull,
                inputStreaming,
                outputStreaming,
                options.bddTrace);
        DynamicClient client = buildClient(name, profile, model, serviceId, region, options, capture);

        // Streaming output (e.g. S3 GetObject) must be written to a file with --output-payload.
        String streamingMember = streamingOutputMember(client, opName);
        if (!options.plan && streamingMember != null && options.outputPayload == null) {
            throw new CliError(opName + " returns a streaming response body (member '" + streamingMember
                    + "'); provide --output-payload (-o) to write it to a file.");
        }

        try {
            Document result = input == null ? client.call(opName) : client.call(opName, input);
            Document data = streamingMember != null
                    ? CallIo.writeStreamingOutput(result, streamingMember, options.outputPayload)
                    : result;
            // If this operation is @paginated and the response carries a next-page token, emit an
            // opaque @continue token (the verbatim input with the token injected) for the next page.
            String effectiveUrl = options.url != null ? options.url : profile.url;
            String continueToken = CallPagination.nextToken(
                    opShape,
                    model,
                    name,
                    serviceId.toString(),
                    opName,
                    input,
                    result,
                    region,
                    effectiveUrl,
                    options.protocol);
            CallArtifacts.print(env, options.pretty, options.query, envelope(capture, wire, data, continueToken));
            if (continueToken != null) {
                // Also print a ready-to-paste next-page command to stderr. stderr (not stdout) so it
                // needs no JSON escaping, keeps stdout pure JSON, and -- crucially -- survives --query
                // (which can strip @continue out of the stdout JSON entirely). Flush stdout first so the
                // JSON is fully written before the hint, otherwise the two streams interleave.
                env.stdout().flush();
                CallPagination.printHint(env, name, opName, continueToken, options);
            }
            return 0;
        } catch (DryRunException e) {
            String protocolId = client.config().protocol().id().toString();
            CallArtifacts.print(env,
                    options.pretty,
                    options.query,
                    planDocument(capture,
                            name,
                            profile,
                            opShape,
                            serviceId,
                            opName,
                            CallIo.echoInput(input, streamingInput),
                            protocolId,
                            wire,
                            model,
                            region,
                            options));
            return 0;
        } catch (DocumentException e) {
            CallArtifacts.print(env,
                    options.pretty,
                    options.query,
                    envelope(capture, wire, e.getContents(), errorMeta(shapeName(e), capture, true, null)));
            return 1;
        } catch (CallException e) {
            // Wire data can contain secrets outside Authorization (custom auth headers, cookies, and
            // payload fields), so only include it when the caller explicitly opted in with --wire.
            CallArtifacts.print(env,
                    options.pretty,
                    options.query,
                    envelope(capture, wire, null, errorMeta(null, capture, false, e.getMessage())));
            return 1;
        } catch (SerializationException | IllegalArgumentException e) {
            // A serde/shape mismatch, almost always the --input document not matching the operation's
            // schema (wrong type, non-object, or a member that doesn't fit) while building the request.
            // These aren't call outcomes (those are DocumentException/CallException above) nor credentials/
            // transport failures (other exception types, which propagate unchanged). Point the caller
            // (often an LLM) at the operation's full schema so they can reconcile the input in one step.
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            throw new CliError(msg + ". See the operation's full schema with: "
                    + closureCommand(name, opShape.getId()));
        }
    }

    private DynamicClient buildClient(
            String name,
            CallProfiles.Profile profile,
            Model model,
            ShapeId serviceId,
            String region,
            CallOptions options,
            WireCapture capture
    ) {
        DynamicClient.Builder builder = DynamicClient.builder()
                .serviceId(serviceId)
                .model(model)
                .addInterceptor(capture);

        if (region != null) {
            builder.putConfig(RegionSetting.REGION, region);
        }

        // For --plan, attach the trace sink so the rules engine reports exactly what drove endpoint
        // resolution: the authoritative resolved parameters always, plus the per-condition decision path
        // when --bdd-trace is set. WireCapture is the sink and reads the data back after the call.
        if (options.plan) {
            builder.putConfig(RulesEngineSettings.BDD_TRACE_SINK, capture);
        }

        // Reuse precompiled endpoint bytecode if present; EndpointRulesPlugin then skips recompiling.
        Path bdd = CallProfiles.bytecodeArtifact(name);
        if (options.url != null || profile.url != null) {
            builder.endpointResolver(EndpointResolver.staticEndpoint(options.url != null ? options.url : profile.url));
        } else if (Files.isRegularFile(bdd)) {
            try {
                builder.putConfig(RulesEngineSettings.BYTECODE, new RulesEngineBuilder().load(bdd));
            } catch (RuntimeException e) {
                LOGGER.fine(() -> "Could not load precompiled endpoint bytecode, will compile from model: "
                        + e.getMessage());
            }
        }

        // Force a protocol if requested; otherwise DynamicClient auto-detects from the model.
        if (options.protocol != null) {
            builder.protocol(CallProtocol.create(options.protocol, serviceId));
        }

        Shape serviceShape = model.expectShape(serviceId);
        configureAuth(builder, serviceId, serviceShape, profile.auth, region, options.plan);
        return builder.build();
    }

    private void configureAuth(
            DynamicClient.Builder builder,
            ShapeId serviceId,
            Shape serviceShape,
            String authMode,
            String region,
            boolean dryrun
    ) {
        if (authMode == null) {
            // Auto-detect from the model. Register credentials so a detected SigV4 scheme can sign.
            if (serviceShape.hasTrait(SigV4Trait.class) && region == null) {
                throw new CliError("This service uses SigV4, which requires a region. Set --aws-region, "
                        + "AWS_REGION, register a region, or set one in your AWS profile.");
            }
            builder.addIdentityResolver(CallCredentials.resolve(dryrun));
            return;
        }
        switch (authMode.toLowerCase()) {
            case "sigv4":
            case "aws":
                if (region == null) {
                    throw new CliError("SigV4 auth requires a region.");
                }
                String signingName = serviceShape.getTrait(SigV4Trait.class)
                        .map(SigV4Trait::getName)
                        .orElseGet(() -> serviceShape.getTrait(ServiceTrait.class)
                                .map(ServiceTrait::getArnNamespace)
                                .orElse(serviceId.getNamespace().toLowerCase()));
                builder.putSupportedAuthSchemes(new SigV4AuthScheme(signingName))
                        .authSchemeResolver(AuthSchemeResolver.DEFAULT)
                        .addIdentityResolver(CallCredentials.resolve(dryrun));
                break;
            case "none":
                builder.authSchemeResolver(AuthSchemeResolver.NO_AUTH);
                break;
            default:
                throw new CliError("Unsupported auth mode: " + authMode);
        }
    }

    /** A resolved config value plus where it came from (for the --plan config section). */
    private static final class Sourced {
        final String value;
        final String source;

        Sourced(String value, String source) {
            this.value = value;
            this.source = source;
        }
    }

    private String resolveRegion(CallOptions options, CallProfiles.Profile profile) {
        Sourced r = resolveRegionSourced(options, profile);
        return r == null ? null : r.value;
    }

    /** Region precedence: --aws-region, AWS_REGION, AWS_DEFAULT_REGION, registered region, AWS profile. */
    private Sourced resolveRegionSourced(CallOptions options, CallProfiles.Profile profile) {
        if (options.awsRegion != null) {
            return new Sourced(options.awsRegion, "argument:--aws-region");
        }
        String env = System.getenv("AWS_REGION");
        if (env != null && !env.isEmpty()) {
            return new Sourced(env, "environment:AWS_REGION");
        }
        env = System.getenv("AWS_DEFAULT_REGION");
        if (env != null && !env.isEmpty()) {
            return new Sourced(env, "environment:AWS_DEFAULT_REGION");
        }
        if (profile.region != null) {
            return new Sourced(profile.region, "registration");
        }
        try {
            AwsProfileFile file = AwsProfileFile.loadSilently();
            if (file != null) {
                AwsProfile p = options.awsProfile != null
                        ? file.profile(options.awsProfile)
                        : file.activeProfile(System::getenv);
                if (p != null) {
                    String r = p.property("region");
                    if (r != null && !r.isEmpty()) {
                        return new Sourced(r, "aws-profile:region");
                    }
                }
            }
        } catch (RuntimeException e) {
            LOGGER.fine(() -> "Unable to resolve region from AWS profile: " + e.getMessage());
        }
        return null;
    }

    /** Resolves which AWS profile is active and how it was selected (for the --plan config section). */
    private Sourced resolveProfileSourced(CallOptions options) {
        if (options.awsProfile != null) {
            return new Sourced(options.awsProfile, "argument:--aws-profile");
        }
        String env = System.getenv("AWS_PROFILE");
        if (env != null && !env.isEmpty()) {
            return new Sourced(env, "environment:AWS_PROFILE");
        }
        return new Sourced("default", "default");
    }

    // ===================================================================================
    // JSON help
    // ===================================================================================

    private int helpJson(String name, CallProfiles.Profile profile, String operation, CallOptions options, Env env) {
        // Help uses the with-docs model so descriptions are available.
        Model model = CallArtifacts.loadArtifactModel(CallProfiles.modelArtifact(name), env.classLoader());
        ShapeId serviceId = profile.service != null
                ? CallArtifacts.resolveServiceByName(model, profile.service)
                : CallArtifacts.resolveSoleService(model);

        if (operation == null) {
            return options.json ? serviceHelpJson(model, serviceId, options, env)
                    : serviceHelpText(name, model, serviceId, env);
        }
        return options.json ? operationHelpJson(model, serviceId, operation, options, env)
                : operationHelpText(name, model, serviceId, operation, env);
    }

    // ----- Human-readable help (default) -----

    private int serviceHelpText(String name, Model model, ShapeId serviceId, Env env) {
        ServiceShape service = model.expectShape(serviceId, ServiceShape.class);
        var out = env.stdout();
        out.println(name + "  (" + serviceId + ")");
        service.getTrait(DocumentationTrait.class)
                .ifPresent(d -> out.println("  " + firstLine(stripHtml(d.getValue()))));
        if (service.getVersion() != null) {
            out.println("  Version: " + service.getVersion());
        }
        out.println("");
        out.println("Operations:");
        TopDownIndex.of(model)
                .getContainedOperations(serviceId)
                .stream()
                .map(op -> op.getId().getName())
                .sorted()
                .forEach(op -> out.println("    " + op));
        out.println("");
        out.println("Run `smithy call " + name + " <Operation> --help` for an operation's input/output.");
        return 0;
    }

    private int operationHelpText(String name, Model model, ShapeId serviceId, String operation, Env env) {
        OperationShape op = CallArtifacts.resolveOperation(model, serviceId, operation);
        var out = env.stdout();
        out.println(name + " " + op.getId().getName());
        op.getTrait(DocumentationTrait.class)
                .ifPresent(d -> out.println("  " + firstLine(stripHtml(d.getValue()))));
        out.println("");
        out.println("Input:");
        printShapeMembersText(model, op.getInputShape(), out);
        out.println("");
        out.println("Output:");
        printShapeMembersText(model, op.getOutputShape(), out);
        out.println("");
        out.println("Full shapes (input, output, errors, and everything they reference) as JSON:");
        out.println("  " + closureCommand(name, op.getId()));
        return 0;
    }

    /**
     * A ready-to-run {@code smithy ast} command that dumps the complete transitive closure of an
     * operation (the operation shape plus every shape reachable from it) as a JSON model. Surfaced in
     * operation help and in input-shaping errors so an agent can get the authoritative schema in one
     * step. The selector {@code [id = 'OP'] :is(*, ~>)} matches the operation itself and its recursive
     * closure.
     */
    // Package-private for unit testing the exact hint format (unquoted id, --no-docs, copyable).
    static String closureCommand(String name, ShapeId operationId) {
        // --no-docs keeps the dump compact (AWS doc traits are large HTML blobs); these hints target
        // agents/LLMs that want structure, not prose. A human can drop --no-docs to get documentation.
        return "smithy ast --name " + name + " --no-docs --selector '[id = " + operationId + "] :is(*, ~>)'";
    }

    private void printShapeMembersText(Model model, ShapeId shapeId, CliPrinter out) {
        Shape shape = model.getShape(shapeId).orElse(null);
        if (shape == null || shape.getAllMembers().isEmpty()) {
            out.println("    (none)");
            return;
        }
        shape.getAllMembers().forEach((memberName, member) -> {
            String required = member.hasTrait("smithy.api#required") ? " (required)" : "";
            String target = member.getTarget().getName();
            out.println("    " + memberName + ": " + target + required);
            member.getTrait(DocumentationTrait.class)
                    .ifPresent(d -> out.println("        " + firstLine(stripHtml(d.getValue()))));
        });
    }

    /** Strips HTML tags and collapses whitespace (AWS docs are HTML) for terminal output. */
    private static String stripHtml(String s) {
        return s.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private static String firstLine(String s) {
        // Keep help compact: first sentence or ~160 chars.
        int dot = s.indexOf(". ");
        if (dot > 0 && dot < 200) {
            return s.substring(0, dot + 1);
        }
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

    // ----- Structured (JSON) help, via --json -----

    private int serviceHelpJson(Model model, ShapeId serviceId, CallOptions options, Env env) {
        ServiceShape service = model.expectShape(serviceId, ServiceShape.class);
        Map<String, Document> out = new LinkedHashMap<>();
        out.put("service", Document.of(serviceId.toString()));
        service.getTrait(DocumentationTrait.class)
                .ifPresent(d -> out.put("documentation", Document.of(d.getValue())));
        out.put("version", Document.of(service.getVersion() == null ? "" : service.getVersion()));
        List<Document> ops = TopDownIndex.of(model)
                .getContainedOperations(serviceId)
                .stream()
                .map(op -> op.getId().getName())
                .sorted()
                .map(Document::of)
                .collect(Collectors.toList());
        out.put("operations", Document.of(ops));
        CallArtifacts.print(env, options.pretty, options.query, Document.of(out));
        return 0;
    }

    private int operationHelpJson(Model model, ShapeId serviceId, String operation, CallOptions options, Env env) {
        OperationShape op = CallArtifacts.resolveOperation(model, serviceId, operation);
        Map<String, Document> out = new LinkedHashMap<>();
        out.put("operation", Document.of(op.getId().getName()));
        op.getTrait(DocumentationTrait.class).ifPresent(d -> out.put("documentation", Document.of(d.getValue())));
        out.put("input", shapeHelp(model, op.getInputShape()));
        out.put("output", shapeHelp(model, op.getOutputShape()));
        CallArtifacts.print(env, options.pretty, options.query, Document.of(out));
        return 0;
    }

    /** Describes a structure shape's members for operation help (name -> {target, documentation, required}). */
    private Document shapeHelp(Model model, ShapeId shapeId) {
        Shape shape = model.getShape(shapeId).orElse(null);
        Map<String, Document> members = new LinkedHashMap<>();
        if (shape != null) {
            shape.getAllMembers().forEach((memberName, member) -> {
                Map<String, Document> m = new LinkedHashMap<>();
                m.put("target", Document.of(member.getTarget().toString()));
                member.getTrait(DocumentationTrait.class)
                        .ifPresent(d -> m.put("documentation", Document.of(d.getValue())));
                boolean required = member.hasTrait("smithy.api#required");
                if (required) {
                    m.put("required", Document.of(true));
                }
                members.put(memberName, Document.of(m));
            });
        }
        return Document.of(members);
    }

    /** Renders the standard CLI flag help (same style as every other command) for `smithy call --help`. */
    private void printStandardHelp(Arguments arguments, Env env) {
        String title = StringUtils.isEmpty(parentCommandName) ? getName() : parentCommandName + " " + getName();
        HelpPrinter.fromArguments(title, arguments)
                .summary(getSummary())
                .documentation(EXAMPLES)
                .print(env.colors(), env.stdout());
    }

    // Keep every line <= 80 chars: the help renderer word-wraps longer lines, which would
    // break commands mid-token and spoil copy-paste.
    private static final String EXAMPLES =
            "Examples:\n"
                    + "  # Register a service first (see `smithy register --help`), then call it:\n"
                    + "  smithy call s3"
                    + " ListBuckets\n"
                    + "  smithy call s3"
                    + " ListObjectsV2 -i '{\"Bucket\":\"my-bucket\"}'\n"
                    + "  smithy call s3"
                    + " ListObjectsV2 -i @input.json --pretty\n"
                    + "  echo '{\"Bucket\":\"b\"}' | smithy call s3"
                    + " ListObjectsV2 -i -\n"
                    + "\n"
                    + "  # Inspect without sending: build and sign the request, then print it.\n"
                    + "  smithy call s3"
                    + " ListObjectsV2 -i '{\"Bucket\":\"b\"}' --plan\n"
                    + "\n"
                    + "  # Filter output with JMESPath, or follow pagination with the printed token:\n"
                    + "  smithy call s3"
                    + " ListObjectsV2 -i '{\"Bucket\":\"b\"}' -q 'Contents[].Key'\n"
                    + "  smithy call s3"
                    + " ListObjectsV2 --continue <TOKEN>\n"
                    + "\n"
                    + "  # Discover operations and their input/output shapes:\n"
                    + "  smithy call s3 --help                       # list operations\n"
                    + "  smithy call s3"
                    + " ListObjectsV2 --help         # show input/output\n"
                    + "  smithy call s3"
                    + " ListObjectsV2 --help --json  # structured help";

    // ===================================================================================
    // Output envelope
    // ===================================================================================

    /**
     * Builds the call-result envelope. Modeled data is spread at the top level; {@code @error} (if any),
     * {@code @http} (only when {@code wire != null}), and {@code @continue} (next-page token, if any)
     * carry metadata.
     */
    private Document envelope(WireCapture capture, String wire, Document modeled, String continueToken) {
        return envelope(capture, wire, modeled, (Map<String, Document>) null, continueToken);
    }

    private Document envelope(WireCapture capture, String wire, Document modeled, Map<String, Document> errorMeta) {
        return envelope(capture, wire, modeled, errorMeta, null);
    }

    private Document envelope(
            WireCapture capture,
            String wire,
            Document modeled,
            Map<String, Document> errorMeta,
            String continueToken
    ) {
        Map<String, Document> out = new LinkedHashMap<>();
        if (wire != null) {
            out.put("@http", capture.httpBlock("full".equals(wire)));
        }
        if (errorMeta != null) {
            out.putAll(errorMeta);
        }
        if (continueToken != null) {
            out.put("@continue", Document.of(continueToken));
        }
        if (modeled != null) {
            Map<String, Document> members = modeled.asStringMap();
            if (members != null) {
                out.putAll(members);
            }
        }
        return Document.of(out);
    }

    private Map<String, Document> errorMeta(String code, WireCapture capture, boolean modeled, String message) {
        int status = capture.statusCode();
        Map<String, Document> result = new LinkedHashMap<>();
        Map<String, Document> error = new LinkedHashMap<>();
        if (code != null) {
            error.put("code", Document.of(code));
        }
        if (status >= 500) {
            error.put("fault", Document.of("server"));
        } else if (status >= 400) {
            error.put("fault", Document.of("client"));
        }
        error.put("modeled", Document.of(modeled));
        if (message != null) {
            error.put("message", Document.of(message));
        }
        result.put("@error", Document.of(error));
        if (!modeled) {
            String body = capture.responseBodyText();
            if (body != null && !body.isEmpty()) {
                result.put("@rawBody", Document.of(body));
            }
        }
        return result;
    }

    /**
     * Builds the --plan document: service/operation/protocol, config (with sources), endpoint (incl.
     * resolved rule parameters), auth, safety summary, input, and the @http block.
     */
    private Document planDocument(
            WireCapture capture,
            String name,
            CallProfiles.Profile profile,
            OperationShape opShape,
            ShapeId serviceId,
            String operation,
            Document input,
            String protocolId,
            String wire,
            Model model,
            String region,
            CallOptions options
    ) {
        Map<String, Document> out = new LinkedHashMap<>();
        out.put("service", Document.of(serviceId.toString()));
        out.put("operation", Document.of(operation));
        if (protocolId != null) {
            out.put("protocol", Document.of(protocolId));
        }
        out.put("config", configDocument(options, profile));
        out.put("endpoint", endpointWithParameters(capture, model, serviceId, opShape, input, region));
        out.put("auth", capture.authDocument());
        out.put("safety", CallSafety.describe(opShape, model, serviceId));
        out.put("input", input != null ? input : Document.of(Map.of()));
        // --plan always includes the request side; --wire full upgrades the body to full bytes.
        out.put("@http", capture.httpBlock("full".equals(wire)));
        return Document.of(out);
    }

    /**
     * The resolved endpoint (resolver + uri + properties from the capture) augmented with the endpoint
     * rule {@code parameters} actually fed to the rules engine. The values are authoritative: the rules
     * engine snapshots its resolved parameter registers (input {@code @contextParam}s, builtins like
     * {@code AWS::Region}, and declared defaults) when {@code --plan} sets the capture flag. If the
     * snapshot is unavailable (e.g. a static {@code --url} endpoint with no rules engine), we fall back
     * to reconstructing the values from the same sources the engine reads.
     */
    private Document endpointWithParameters(
            WireCapture capture,
            Model model,
            ShapeId serviceId,
            OperationShape operation,
            Document input,
            String region
    ) {
        Document base = capture.endpointDocument();

        Map<String, Document> paramValues;
        if (capture.resolvedEndpointParams != null && !capture.resolvedEndpointParams.isEmpty()) {
            // Authoritative: exactly the parameter values the rules engine resolved.
            paramValues = new LinkedHashMap<>();
            capture.resolvedEndpointParams.forEach((pName, value) -> {
                Document doc = objectToDocument(value);
                if (doc != null) {
                    paramValues.put(pName, doc);
                }
            });
        } else {
            // Fallback reconstruction when the engine didn't capture (e.g. static --url endpoint).
            Parameters params = endpointParameters(model, serviceId);
            if (params == null) {
                return base;
            }
            Map<String, Document> contextValues = contextParamValues(model, operation, input);
            paramValues = new LinkedHashMap<>();
            for (var p : params.toList()) {
                String pName = p.getNameString();
                Document value = contextValues.get(pName);
                if (value == null && "AWS::Region".equals(p.getBuiltIn().orElse(null)) && region != null) {
                    value = Document.of(region);
                }
                if (value == null && p.getDefault().isPresent()) {
                    value = objectToDocument(p.getDefault().get().toObject());
                }
                if (value != null) {
                    paramValues.put(pName, value);
                }
            }
        }

        Map<String, Document> out = new LinkedHashMap<>(base.asStringMap());
        if (!paramValues.isEmpty()) {
            out.put("parameters", Document.of(paramValues));
        }
        // --bdd-trace: the per-condition decision path the rules engine took (decoded from the bytecode).
        // The final result step carries the resolved endpoint, attached by the sink at result() time.
        if (!capture.endpointTrace.isEmpty()) {
            out.put("trace", Document.of(capture.endpointTrace));
        }
        return Document.of(out);
    }

    /** Converts a raw rules-engine register value (String/Boolean/Number/List/Map) into a Document. */
    @SuppressWarnings("unchecked")
    private static Document objectToDocument(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Boolean b) {
            return Document.of(b);
        }
        if (o instanceof Number n) {
            return Document.ofNumber(n);
        }
        if (o instanceof String s) {
            return Document.of(s);
        }
        if (o instanceof List<?> list) {
            List<Document> docs = new ArrayList<>();
            for (Object e : list) {
                Document d = objectToDocument(e);
                if (d != null) {
                    docs.add(d);
                }
            }
            return Document.of(docs);
        }
        if (o instanceof Map<?, ?> map) {
            Map<String, Document> docs = new LinkedHashMap<>();
            ((Map<String, Object>) map).forEach((k, v) -> {
                Document d = objectToDocument(v);
                if (d != null) {
                    docs.put(k, d);
                }
            });
            return Document.of(docs);
        }
        // A value that knows its own Node form (e.g. the rules engine's partition map) is inlined as
        // structured JSON rather than stringified, so traces show real nested data.
        if (o instanceof ToNode toNode) {
            return nodeToDocument(toNode.toNode());
        }
        return Document.of(String.valueOf(o));
    }

    /** Converts a Smithy {@link Node} into the equivalent {@link Document}. */
    private static Document nodeToDocument(Node node) {
        if (node == null || node.isNullNode()) {
            return null;
        }
        if (node instanceof StringNode s) {
            return Document.of(s.getValue());
        }
        if (node instanceof NumberNode n) {
            return Document.ofNumber(n.getValue());
        }
        if (node instanceof BooleanNode b) {
            return Document.of(b.getValue());
        }
        if (node instanceof ArrayNode array) {
            List<Document> docs = new ArrayList<>();
            for (Node e : array.getElements()) {
                Document d = nodeToDocument(e);
                if (d != null) {
                    docs.add(d);
                }
            }
            return Document.of(docs);
        }
        if (node instanceof ObjectNode obj) {
            Map<String, Document> docs = new LinkedHashMap<>();
            obj.getStringMap().forEach((k, v) -> {
                Document d = nodeToDocument(v);
                if (d != null) {
                    docs.put(k, d);
                }
            });
            return Document.of(docs);
        }
        return Document.of(Node.printJson(node));
    }

    /** The endpoint rule parameters declared on the service (BDD or decision-tree), or null. */
    private Parameters endpointParameters(Model model, ShapeId serviceId) {
        ServiceShape service = model.expectShape(serviceId, ServiceShape.class);
        if (service.hasTrait(EndpointBddTrait.class)) {
            return service.expectTrait(EndpointBddTrait.class).getParameters();
        }
        if (service.hasTrait(EndpointRuleSetTrait.class)) {
            return service.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet().getParameters();
        }
        return null;
    }

    /** Maps endpoint-param names to values pulled from the input via @contextParam member bindings. */
    private Map<String, Document> contextParamValues(Model model, OperationShape operation, Document input) {
        Map<String, Document> result = new LinkedHashMap<>();
        if (input == null) {
            return result;
        }
        Map<String, Document> members = input.asStringMap();
        if (members == null) {
            return result;
        }
        // Only the selected operation's input members can bind endpoint context parameters.
        Shape in = model.getShape(operation.getInputShape()).orElse(null);
        if (in != null) {
            in.getAllMembers().forEach((memberName, member) -> {
                var cp = member.getTrait(ContextParamTrait.class);
                if (cp.isPresent() && members.containsKey(memberName)) {
                    result.put(cp.get().getName(), members.get(memberName));
                }
            });
        }
        return result;
    }

    /** Reports each resolved config value and where it came from. */
    private Document configDocument(CallOptions options, CallProfiles.Profile profile) {
        Map<String, Document> cfg = new LinkedHashMap<>();
        Sourced region = resolveRegionSourced(options, profile);
        if (region != null) {
            cfg.put("region", sourcedDoc(region));
        }
        cfg.put("profile", sourcedDoc(resolveProfileSourced(options)));
        return Document.of(cfg);
    }

    private Document sourcedDoc(Sourced s) {
        Map<String, Document> m = new LinkedHashMap<>();
        m.put("value", Document.of(s.value));
        m.put("source", Document.of(s.source));
        return Document.of(m);
    }

    private static String shapeName(DocumentException e) {
        var schema = e.schema();
        return schema == null ? null : schema.id().getName();
    }

    private String streamingOutputMember(DynamicClient client, String operation) {
        Schema output = client.getOperation(operation).outputSchema();
        for (Schema member : output.members()) {
            if (member.memberTarget().hasTrait(TraitKey.STREAMING_TRAIT)) {
                return member.memberName();
            }
        }
        return null;
    }

    // ===================================================================================
    // Interceptors: response capture and dry-run
    // ===================================================================================

    /** Sentinel thrown to abort a request after it's fully prepared (for --plan). */
    private static final class DryRunException extends RuntimeException {
        DryRunException() {
            super(null, null, false, false);
        }
    }

    /** Max body bytes embedded in an @http block under --wire full before truncation. */
    private static final int WIRE_BODY_LIMIT = 4 * 1024 * 1024;

    // Known Authorization scheme tokens that are safe to echo in the @http block. The scheme is shown so a
    // reader can see *how* the request authenticated, but the credential material after it is always
    // redacted. An allow-list is deliberate: the scheme token is only revealed when it matches a
    // recognized scheme, so an unrecognized or malformed Authorization header is redacted whole rather
    // than risk leaking a secret sitting in the first token. Compared case-insensitively.
    private static final Set<String> KNOWN_AUTH_SCHEMES = Set.of(
            "aws4-hmac-sha256",
            "basic",
            "bearer",
            "digest",
            "negotiate",
            "ntlm");

    /**
     * Redacts an Authorization header value while preserving a recognized scheme token. For a known
     * scheme (see {@link #KNOWN_AUTH_SCHEMES}) the result is {@code "<scheme> <redacted>"}; anything else
     * (no scheme token, or an unrecognized one) is redacted whole as {@code "<redacted>"} so an unexpected
     * format can't leak the credential. Note sigv4's {@code "AWS4-HMAC-SHA256 ... Credential=AKIA...,
     * Signature=..."} carries the access key id and signature after the scheme, so only the scheme token
     * is ever shown.
     */
    static String redactAuthorization(String value) {
        int sp = value.indexOf(' ');
        String scheme = sp < 0 ? value : value.substring(0, sp);
        if (!scheme.isEmpty() && KNOWN_AUTH_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            return scheme + " <redacted>";
        }
        return "<redacted>";
    }

    /**
     * A single captured HTTP message side (request or response): headers plus the raw payload bytes.
     * Streaming bodies are flagged but never buffered. {@link #toDocument} renders the summary/full
     * shapes used in the {@code @http} block.
     */
    private static final class WireMessage {
        Map<String, List<String>> headers = Map.of();
        // request fields
        String method;
        String uri;
        // response fields
        int status = -1;
        // body
        boolean streaming;
        String contentType;
        long contentLength = -1;
        byte[] body; // null if streaming or absent
        boolean truncated;

        /** Builds the message Document. {@code full} controls whether the body is summary or full. */
        Document toDocument(boolean full, boolean request) {
            Map<String, Document> m = new LinkedHashMap<>();
            if (request) {
                m.put("method", Document.of(method));
                m.put("uri", Document.of(uri));
            } else {
                m.put("status", Document.of(status));
            }
            Map<String, Document> hdrs = new LinkedHashMap<>();
            for (var e : headers.entrySet()) {
                // Never echo the Authorization header's credential material, but keep the scheme visible.
                String v = e.getKey().equalsIgnoreCase("authorization")
                        ? redactAuthorization(String.join(", ", e.getValue()))
                        : String.join(", ", e.getValue());
                hdrs.put(e.getKey(), Document.of(v));
            }
            m.put("headers", Document.of(hdrs));
            m.put("body", bodyDocument(full));
            return Document.of(m);
        }

        private Document bodyDocument(boolean full) {
            Map<String, Document> b = new LinkedHashMap<>();
            b.put("type", Document.of(full ? "full" : "summary"));
            b.put("streaming", Document.of(streaming));
            if (contentType != null) {
                b.put("contentType", Document.of(contentType));
            }
            if (contentLength >= 0) {
                b.put("contentLength", Document.of(contentLength));
            }
            if (full && !streaming && body != null) {
                // Render textual content inline; binary (cbor/octet-stream/...) as base64.
                if (isTextualContentType(contentType)) {
                    b.put("body", Document.of(new String(body, StandardCharsets.UTF_8)));
                } else {
                    b.put("bodyEncoding", Document.of("base64"));
                    b.put("body", Document.of(Base64.getEncoder().encodeToString(body)));
                }
                b.put("bodyTruncated", Document.of(truncated));
            }
            return Document.of(b);
        }

        private static boolean isTextualContentType(String ct) {
            if (ct == null) {
                return false;
            }
            String c = ct.toLowerCase(Locale.ROOT);
            return c.contains("json") || c.contains("xml")
                    || c.startsWith("text/")
                    || c.contains("x-www-form-urlencoded");
        }
    }

    /** Reads a non-streaming body into bytes (capped at the wire limit), setting truncation flags. */
    private static void captureBody(DataStream stream, WireMessage msg, boolean bodyIsStreaming, boolean wantFull) {
        if (stream == null) {
            return;
        }
        msg.contentType = stream.contentType();
        msg.contentLength = stream.contentLength();
        msg.streaming = bodyIsStreaming;
        if (bodyIsStreaming || !wantFull) {
            return; // never buffer a streaming payload, and only read bytes when --wire full needs them
        }
        ByteBuffer buf = stream.asByteBuffer();
        int n = buf.remaining();
        int take = Math.min(n, WIRE_BODY_LIMIT);
        byte[] bytes = new byte[take];
        buf.duplicate().get(bytes);
        msg.body = bytes;
        msg.truncated = n > take;
    }

    /**
     * Captures the request (and, for real calls, the response) for the {@code @http} block and for
     * {@code --plan}. With dryrun set, aborts before transmit. The response body is replayed back
     * into the pipeline so capturing it doesn't disturb deserialization. Streaming bodies are flagged
     * and never buffered.
     */
    private static final class WireCapture implements ClientInterceptor, BddTraceSink {
        private final boolean dryrun;
        private final boolean wantFull; // --wire full
        private final boolean reqStreaming;
        private final boolean respStreaming;

        final WireMessage request = new WireMessage();
        final WireMessage response = new WireMessage();
        boolean haveRequest;
        boolean haveResponse;

        // Context captured for the dryrun endpoint/auth sections.
        Endpoint endpoint;
        EndpointResolver resolver;
        Identity identity;
        String region;
        String signingName;
        // Effective SigV4 signing region/name resolved with the same precedence the signer uses: an
        // endpoint auth-scheme override (e.g. global services signing us-east-1, S3 MRAP signing "*")
        // wins over the client region / scheme signingName. Null when there's no SigV4 scheme.
        String signingRegion;
        String signingRegionSource;

        // Endpoint resolution capture (populated for --plan via the BddTraceSink the rules engine drives).
        // resolvedEndpointParams is the authoritative parameter set; endpointTrace is the per-condition
        // decision path, recorded only when --bdd-trace is set.
        private final boolean traceConditions;
        final Map<String, Object> resolvedEndpointParams = new LinkedHashMap<>();
        final List<Document> endpointTrace = new ArrayList<>();

        WireCapture(
                boolean dryrun,
                boolean wantFull,
                boolean reqStreaming,
                boolean respStreaming,
                boolean traceConditions
        ) {
            this.dryrun = dryrun;
            this.wantFull = wantFull;
            this.reqStreaming = reqStreaming;
            this.respStreaming = respStreaming;
            this.traceConditions = traceConditions;
        }

        // BddTraceSink: the rules engine calls this once per endpoint resolution (only when --plan
        // attaches this sink). We snapshot the resolved parameters for the plan's endpoint.parameters,
        // and -- when --bdd-trace is set -- record each condition/result step decoded from the bytecode.
        @Override
        public BddTrace begin(Bytecode bytecode, Map<String, Object> parameters) {
            // parameters is a live view; copy the present entries for the plan's endpoint.parameters.
            resolvedEndpointParams.putAll(parameters);
            if (!traceConditions) {
                return null; // params only; skip the per-condition trace
            }
            // Track the prior state so each step can report what changed (variables get assigned as
            // conditions run). Seeded with the initial inputs so the first assignment shows as a diff.
            Map<String, Object> prev = new LinkedHashMap<>(parameters);
            return new BddTrace() {
                @Override
                public void node(int nodeRef, int conditionId, boolean satisfied, boolean branch) {
                    Map<String, Document> step = new LinkedHashMap<>();
                    step.put("node", Document.of(nodeRef));
                    step.put("condition", Document.of(conditionId));
                    step.put("instructions", instructionList(bytecode.describeCondition(conditionId)));
                    step.put("satisfied", Document.of(satisfied));
                    step.put("branch", Document.of(branch));
                    Document changes = paramChanges(parameters, prev);
                    if (changes != null) {
                        step.put("paramChanges", changes);
                    }
                    endpointTrace.add(Document.of(step));
                }

                @Override
                public void result(int resultId, Endpoint endpoint) {
                    Map<String, Document> step = new LinkedHashMap<>();
                    step.put("result", Document.of(resultId));
                    if (resultId >= 0) {
                        step.put("instructions", instructionList(bytecode.describeResult(resultId)));
                    }
                    Document changes = paramChanges(parameters, prev);
                    if (changes != null) {
                        step.put("paramChanges", changes);
                    }
                    // The concrete outcome the result rule produced (uri, auth schemes, properties).
                    if (endpoint != null) {
                        step.put("resolved", resolvedEndpointDocument(endpoint));
                    }
                    endpointTrace.add(Document.of(step));
                }
            };
        }

        /** Splits multi-line disassembly into a list of single-instruction strings. */
        private static Document instructionList(String disassembly) {
            List<Document> lines = new ArrayList<>();
            for (String line : disassembly.split("\n")) {
                String trimmed = line.strip();
                if (!trimmed.isEmpty()) {
                    lines.add(Document.of(trimmed));
                }
            }
            return Document.of(lines);
        }

        /**
         * What changed in the live param view since the previous step, mutating {@code prev} to the
         * current state. Returns a map of {@code name -> {"added"|"changed": <new value>}}, or null if
         * nothing changed. This is just a record of what moved at this step, not a re-applyable patch
         * (values are the engine's resolved objects, rendered for display).
         */
        private static Document paramChanges(Map<String, Object> now, Map<String, Object> prev) {
            Map<String, Document> changes = new LinkedHashMap<>();
            for (var e : now.entrySet()) {
                if (!Objects.equals(prev.get(e.getKey()), e.getValue())) {
                    String kind = prev.containsKey(e.getKey()) ? "changed" : "added";
                    Document v = objectToDocument(e.getValue());
                    changes.put(e.getKey(), Document.of(Map.of(kind, v == null ? Document.ofObject(null) : v)));
                    prev.put(e.getKey(), e.getValue());
                }
            }
            return changes.isEmpty() ? null : Document.of(changes);
        }

        @Override
        public void readBeforeTransmit(RequestHook<?, ?, ?> hook) {
            if (hook.request() instanceof HttpRequest req) {
                request.method = req.method();
                request.uri = req.uri().toString();
                request.headers = req.headers().map();
                captureBody(req.body(), request, reqStreaming, wantFull || dryrun);
                haveRequest = true;

                var ctx = hook.context();
                endpoint = ctx.get(CallContext.ENDPOINT);
                resolver = ctx.get(CallContext.ENDPOINT_RESOLVER);
                identity = ctx.get(CallContext.IDENTITY);
                region = ctx.get(RegionSetting.REGION);
                signingName = ctx.get(
                        SigV4Settings.SIGNING_NAME);
                captureSigningOverrides();
            }
            if (dryrun) {
                throw new DryRunException();
            }
        }

        /**
         * Resolves the effective SigV4 signing region/name the way {@code SigV4Signer} does: an endpoint
         * auth-scheme property override wins over the client region / scheme-level signing name. Only the
         * sigv4 (and sigv4a) endpoint auth schemes carry these, so we look there first.
         */
        private void captureSigningOverrides() {
            if (endpoint == null) {
                signingRegion = region;
                signingRegionSource = region != null ? "client-region" : null;
                return;
            }
            for (var scheme : endpoint.authSchemes()) {
                if (!scheme.authSchemeId().contains("sigv4")) {
                    continue;
                }
                String epRegion = scheme.property(EndpointAuthSchemeSettings.SIGNING_REGION);
                if (epRegion == null) {
                    List<String> set = scheme.property(EndpointAuthSchemeSettings.SIGNING_REGION_SET);
                    if (set != null && !set.isEmpty()) {
                        epRegion = String.join(",", set);
                    }
                }
                if (epRegion != null) {
                    signingRegion = epRegion;
                    signingRegionSource = "endpoint";
                }
                String epName = scheme.property(EndpointAuthSchemeSettings.SIGNING_NAME);
                if (epName != null) {
                    signingName = epName; // endpoint override wins over the scheme-level signing name.
                }
                break;
            }
            if (signingRegion == null) {
                signingRegion = region;
                signingRegionSource = region != null ? "client-region" : null;
            }
        }

        @Override
        public <ResponseT> ResponseT modifyBeforeDeserialization(ResponseHook<?, ?, ?, ResponseT> hook) {
            if (hook.response() instanceof HttpResponse resp) {
                response.status = resp.statusCode();
                response.headers = resp.headers().map();
                DataStream body = resp.body();
                response.contentType = body.contentType();
                response.contentLength = body.contentLength();
                response.streaming = respStreaming;
                haveResponse = true;
                if (respStreaming) {
                    // Preserve the transport-backed stream for --output-payload. Calling asByteBuffer()
                    // here would materialize the entire payload and defeat streaming.
                    return hook.response();
                }
                ByteBuffer buffer = body.asByteBuffer();
                int n = buffer.remaining();
                int take = Math.min(n, WIRE_BODY_LIMIT);
                byte[] bytes = new byte[take];
                buffer.duplicate().get(bytes);
                response.body = bytes;
                response.truncated = n > take;
                // Replay the (consumed) body so deserialization still works.
                HttpResponse replayed = HttpResponse.of(resp.httpVersion(),
                        resp.statusCode(),
                        resp.headers(),
                        DataStream.ofByteBuffer(buffer.duplicate(), body.contentType()));
                return hook.asResponseType(replayed);
            }
            return hook.response();
        }

        /** Response status (or 0 if no HTTP response was captured), for the error envelope. */
        int statusCode() {
            return haveResponse ? response.status : 0;
        }

        /** Response body as UTF-8 text, for @rawBody on unmodeled errors. */
        String responseBodyText() {
            return response.body == null ? null : new String(response.body, StandardCharsets.UTF_8);
        }

        /** Builds the @http block: request (always if captured) plus response (real calls). */
        Document httpBlock(boolean full) {
            Map<String, Document> http = new LinkedHashMap<>();
            if (haveRequest) {
                http.put("request", request.toDocument(full, true));
            }
            if (haveResponse) {
                http.put("response", response.toDocument(full, false));
            }
            return Document.of(http);
        }

        // ----- dryrun endpoint/auth sections -----

        Document endpointDocument() {
            Map<String, Document> ep = new LinkedHashMap<>();
            ep.put("resolver", Document.of(resolverLabel(resolver)));
            if (endpoint != null) {
                ep.putAll(resolvedEndpointDocument(endpoint).asStringMap());
            }
            return Document.of(ep);
        }

        /** Renders an endpoint's resolved outcome (uri, properties, authSchemes), without resolver label. */
        private static Document resolvedEndpointDocument(Endpoint endpoint) {
            Map<String, Document> ep = new LinkedHashMap<>();
            ep.put("uri", Document.of(endpoint.uri().toString()));
            Map<String, Document> props = new LinkedHashMap<>();
            for (var key : endpoint.properties()) {
                props.put(key.toString(), valueDocument(endpoint.property(key)));
            }
            if (!props.isEmpty()) {
                ep.put("properties", Document.of(props));
            }
            List<Document> schemes = new ArrayList<>();
            for (var scheme : endpoint.authSchemes()) {
                Map<String, Document> s = new LinkedHashMap<>();
                s.put("id", Document.of(scheme.authSchemeId()));
                schemes.add(Document.of(s));
            }
            if (!schemes.isEmpty()) {
                ep.put("authSchemes", Document.of(schemes));
            }
            return Document.of(ep);
        }

        Document authDocument() {
            Map<String, Document> auth = new LinkedHashMap<>();
            String authHeader = firstHeader("authorization");
            if (authHeader != null && authHeader.startsWith("AWS4-HMAC-SHA256")) {
                auth.put("scheme", Document.of("sigv4"));
            } else if (authHeader != null) {
                auth.put("scheme", Document.of("other"));
            } else {
                auth.put("scheme", Document.of("none"));
            }
            // AWS-specific signing details (sigv4). Grouped under "aws" since they don't apply to
            // non-AWS auth schemes. signingRegion is the region actually signed against, which can
            // differ from the client region (global services sign us-east-1; S3 MRAP signs "*").
            Map<String, Document> aws = new LinkedHashMap<>();
            if (signingName != null) {
                aws.put("signingName", Document.of(signingName));
            }
            if (signingRegion != null) {
                aws.put("signingRegion", Document.of(signingRegion));
                if (signingRegionSource != null) {
                    aws.put("signingRegionSource", Document.of(signingRegionSource));
                }
            }
            if (!aws.isEmpty()) {
                auth.put("aws", Document.of(aws));
            }
            if (identity instanceof AwsCredentialsIdentity creds) {
                Map<String, Document> id = new LinkedHashMap<>();
                id.put("resolved", Document.of(true));
                id.put("temporary", Document.of(creds.sessionToken() != null));
                if (creds.accountId() != null) {
                    id.put("accountId", Document.of(creds.accountId()));
                }
                if (creds.expirationTime() != null) {
                    id.put("expiration", Document.of(creds.expirationTime().toString()));
                }
                auth.put("identity", Document.of(id));
            }
            return Document.of(auth);
        }

        private String firstHeader(String name) {
            for (var e : request.headers.entrySet()) {
                if (e.getKey().equalsIgnoreCase(name) && !e.getValue().isEmpty()) {
                    return e.getValue().get(0);
                }
            }
            return null;
        }

        private static String resolverLabel(EndpointResolver resolver) {
            if (resolver == null) {
                return "unknown";
            }
            switch (resolver.getClass().getSimpleName()) {
                case "HostLabelEndpointResolver":
                case "StaticHostResolver":
                    return "static";
                case "BytecodeEndpointResolver":
                    return "rules-engine (bytecode)";
                case "DecisionTreeEndpointResolver":
                    return "rules-engine (decision-tree)";
                default:
                    return resolver.getClass().getSimpleName();
            }
        }

        private static Document valueDocument(Object value) {
            if (value == null) {
                return Document.of("");
            }
            if (value instanceof Map<?, ?> map) {
                Map<String, Document> out = new LinkedHashMap<>();
                map.forEach((k, v) -> out.put(String.valueOf(k), valueDocument(v)));
                return Document.of(out);
            }
            if (value instanceof List<?> list) {
                List<Document> out = new ArrayList<>();
                for (Object e : list) {
                    out.add(valueDocument(e));
                }
                return Document.of(out);
            }
            if (value instanceof Boolean b) {
                return Document.of(b);
            }
            return Document.of(value.toString());
        }
    }

    // ===================================================================================
    // Warmup (AppCDS)
    // ===================================================================================

    static void warmup(ClassLoader classLoader) {
        try {
            Model model = Model.assembler(classLoader)
                    .discoverModels(classLoader)
                    .addUnparsedModel("warmup.smithy", WARMUP_MODEL)
                    .assemble()
                    .unwrap();
            ShapeId serviceId = ShapeId.from("smithy.warmup#Warmup");
            DynamicClient.builder()
                    .serviceId(serviceId)
                    .model(model)
                    .putConfig(RegionSetting.REGION, "us-east-1")
                    .putSupportedAuthSchemes(new SigV4AuthScheme("warmup"))
                    .authSchemeResolver(AuthSchemeResolver.DEFAULT)
                    .addIdentityResolver(EnvironmentVariableIdentityResolver.INSTANCE)
                    .build();
            CallIo.warmup();
        } catch (RuntimeException e) {
            LOGGER.fine(() -> "call warmup skipped: " + e.getMessage());
        }
    }

    private static final String WARMUP_MODEL =
            "$version: \"2\"\n"
                    + "namespace smithy.warmup\n"
                    + "use aws.protocols#restJson1\n"
                    + "use aws.auth#sigv4\n"
                    + "use smithy.rules#endpointRuleSet\n"
                    + "@restJson1\n"
                    + "@sigv4(name: \"warmup\")\n"
                    + "@endpointRuleSet({\n"
                    + "  version: \"1.0\",\n"
                    + "  parameters: {Region: {type: \"String\", required: true, builtIn: \"AWS::Region\"}},\n"
                    + "  rules: [{conditions: [], endpoint: {url: \"https://example.com\"}, type: \"endpoint\"}]\n"
                    + "})\n"
                    + "service Warmup {\n"
                    + "  version: \"2024-01-01\",\n"
                    + "  operations: [Ping]\n"
                    + "}\n"
                    + "@http(method: \"GET\", uri: \"/ping\")\n"
                    + "@readonly\n"
                    + "operation Ping {\n"
                    + "  output := {message: String}\n"
                    + "}\n";
}
