/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.cli.Style;
import software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity;
import software.amazon.smithy.java.aws.client.auth.scheme.sigv4.SigV4AuthScheme;
import software.amazon.smithy.java.aws.client.core.identity.EnvironmentVariableIdentityResolver;
import software.amazon.smithy.java.aws.client.core.settings.EndpointAuthSchemeSettings;
import software.amazon.smithy.java.aws.client.core.settings.RegionSetting;
import software.amazon.smithy.java.aws.config.AwsProfile;
import software.amazon.smithy.java.aws.config.AwsProfileFile;
import software.amazon.smithy.java.aws.client.auth.scheme.sigv4.SigV4Settings;
import software.amazon.smithy.java.aws.credentials.chain.CredentialChain;
import software.amazon.smithy.java.auth.api.identity.Identity;
import software.amazon.smithy.java.auth.api.identity.IdentityResolver;
import software.amazon.smithy.java.auth.api.identity.IdentityResult;
import software.amazon.smithy.java.cbor.Rpcv2CborCodec;
import software.amazon.smithy.java.client.core.CallContext;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.client.core.auth.scheme.AuthSchemeResolver;
import software.amazon.smithy.java.aws.client.awsjson.AwsJson1Protocol;
import software.amazon.smithy.java.aws.client.restjson.RestJsonClientProtocol;
import software.amazon.smithy.java.aws.client.restxml.RestXmlClientProtocol;
import software.amazon.smithy.java.client.core.ClientProtocol;
import software.amazon.smithy.java.client.core.interceptors.ClientInterceptor;
import software.amazon.smithy.java.client.core.interceptors.RequestHook;
import software.amazon.smithy.java.client.core.interceptors.ResponseHook;
import software.amazon.smithy.java.client.rpcv2.RpcV2CborProtocol;
import software.amazon.smithy.java.client.rpcv2json.RpcV2JsonProtocol;
import software.amazon.smithy.java.core.error.CallException;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.dynamicclient.DocumentException;
import software.amazon.smithy.java.dynamicclient.DynamicClient;
import software.amazon.smithy.java.endpoints.Endpoint;
import software.amazon.smithy.java.endpoints.EndpointResolver;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.java.rulesengine.BddTrace;
import software.amazon.smithy.java.rulesengine.BddTraceSink;
import software.amazon.smithy.java.rulesengine.Bytecode;
import software.amazon.smithy.java.rulesengine.RulesEngineBuilder;
import software.amazon.smithy.java.rulesengine.RulesEngineSettings;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.knowledge.BottomUpIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.model.traits.RetryableTrait;
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
 * Then <strong>call</strong> it by name. Calling never re-reads the source model, re-validates, or
 * accepts additional models/dependencies -- it loads the pre-built artifacts and only takes input and
 * output-formatting options:
 *
 * <pre>{@code
 * smithy call s3 ListObjectsV2 --input '{"Bucket":"my-bucket"}'
 * smithy call s3 ListObjectsV2 -i @input.json --pretty
 * echo '{"Bucket":"b"}' | smithy call s3 ListObjectsV2 -i -
 * smithy call s3 ListObjectsV2 -i '{"Bucket":"b"}' --plan
 * smithy call s3 --help                # JSON service help
 * smithy call s3 ListObjectsV2 --help  # JSON operation help
 * }</pre>
 */
final class CallCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(CallCommand.class.getName());

    static {
        // Use Smithy's own (fast) JSON serde provider rather than Jackson. The provider is selected
        // once in JsonSettings' static initializer from this system property, so it must be set before
        // the JsonCodec below is constructed (i.e. before JsonSettings loads).
        if (System.getProperty("smithy-java.json-provider") == null) {
            System.setProperty("smithy-java.json-provider", "smithy");
        }
    }

    // Codec for CLI input/output documents. Matches the AWS rpcv2-json protocol's JSON settings
    // (arbitrary-precision numbers as strings) so the document we parse round-trips like an AWS JSON
    // payload. The dynamic client still serializes onto the wire using the target service's own
    // protocol; this codec only governs how we read --input and render the JSON envelope.
    private static final JsonCodec CODEC = JsonCodec.builder()
            .useStringForArbitraryPrecision(true)
            .build();

    // Continuation tokens are serialized as a flat CBOR array, then base64url'd into the opaque
    // @continue string. CBOR's tight framing (no quotes/commas, small ints in one byte) makes the
    // token notably smaller than JSON for the same flat array. The token is opaque to callers.
    private static final Rpcv2CborCodec TOKEN_CODEC = Rpcv2CborCodec.builder().build();

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
    // Options
    // ===================================================================================

    private static final class Options implements ArgumentReceiver {
        // Call-time options (do not define the service).
        private String input;
        private String inputPayload;
        private boolean pretty;
        private boolean json;
        private boolean plan;
        private boolean bddTrace;
        private String outputPayload;
        private String awsProfile;
        private String awsRegion;
        private String protocol;
        private String wire; // null, "headers", or "full"
        private String query; // JMESPath expression to filter output
        private String continueToken; // opaque pagination token from a prior call's @continue

        // Call-time endpoint override (the registration's URL/rules apply otherwise).
        private String url;

        @Override
        public boolean testOption(String name) {
            switch (name) {
                case "--pretty":
                case "-p":
                    pretty = true;
                    return true;
                case "--json":
                    json = true;
                    return true;
                case "--plan":
                    plan = true;
                    return true;
                case "--bdd-trace":
                    bddTrace = true;
                    return true;
                case "-w":
                    wire = "headers";
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public Consumer<String> testParameter(String name) {
            switch (name) {
                case "-i":
                case "--input":
                    return value -> input = value;
                case "--input-payload":
                    return value -> inputPayload = value;
                case "-o":
                case "--output-payload":
                    return value -> outputPayload = value;
                case "--wire":
                    return value -> wire = value;
                case "-q":
                case "--query":
                    return value -> query = value;
                case "--continue":
                    return value -> continueToken = value;
                case "--aws-profile":
                case "--profile":
                    return value -> awsProfile = value;
                case "--aws-region":
                    return value -> awsRegion = value;
                case "--protocol":
                    return value -> protocol = value;
                case "--url":
                    return value -> url = value;
                default:
                    return null;
            }
        }

        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.positional("<SERVICE>", "A registered service name (register one with `smithy register`).");
            printer.positional("<OPERATION>", "Operation to call. Omit for service-level --help.");
            printer.param("--input", "-i", "INPUT",
                    "Operation input as JSON: a literal '{...}', @file.json to read a file, or - for stdin.");
            printer.param("--input-payload", null, "FILE",
                    "File to stream as the operation's streaming payload member (e.g. S3 PutObject Body). "
                            + "Use - for stdin. Combine with --input for the non-payload fields.");
            printer.param("--continue", null, "TOKEN",
                    "Fetch the next page using the opaque @continue token from a previous call's output. "
                            + "Carries the original input; cannot be combined with --input.");
            printer.option("--pretty", "-p", "Pretty-print JSON output.");
            printer.param("--query", "-q", "JMESPATH",
                    "Filter the JSON output with a JMESPath expression (e.g. 'MetricAlarms[].AlarmName'). "
                            + "Reduces output to just what you need.");
            printer.option("--json", null, "Emit --help for a service/operation as JSON instead of text.");
            printer.option("--plan", null,
                    "Build and sign the request but do not send it; print what would be sent.");
            printer.option("--bdd-trace", null,
                    "With --plan, add an endpoint.trace explaining how endpoint rules resolved: each "
                            + "condition evaluated and the matched result.");
            printer.param("--output-payload", "-o", "FILE",
                    "Write a streaming response payload (e.g. S3 GetObject Body) to this file.");
            printer.param("--wire", "-w", "MODE",
                    "Include an @http block in the output. 'headers' shows request/response messages "
                            + "without bodies; 'full' includes bodies (base64 for binary, truncated past 4MB). "
                            + "-w is shorthand for --wire headers.");
            printer.param("--aws-profile", null, "PROFILE", "AWS profile for credentials and default region.");
            printer.param("--aws-region", null, "REGION", "AWS region (overrides the registered/profile region).");
            printer.param("--url", null, "URL",
                    "Static endpoint URL, forcing the endpoint (overriding the registered URL and rules).");
            printer.param("--protocol", null, "PROTOCOL",
                    "Force the client protocol instead of auto-detecting from the model: "
                            + "rest-json, aws-json, rest-xml, rpc-v2-cbor, rpc-v2-json.");
        }
    }

    // ===================================================================================
    // Dispatch
    // ===================================================================================

    @Override
    public int execute(Arguments arguments, Env env) {
        Options options = new Options();
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
        if (!debug) {
            Logger.getLogger("software.amazon.smithy.java").setLevel(Level.SEVERE);
        }

        try {
            return run(arguments, positional, options, help, debug, env);
        } catch (CliError ce) {
            return CallArtifacts.errorJson(env, options.pretty, "validation", ce.getMessage());
        } catch (RuntimeException re) {
            return CallArtifacts.errorJson(env, options.pretty, CallArtifacts.classify(re), re.getMessage());
        }
    }

    private int run(Arguments arguments, List<String> positional, Options options, boolean help, boolean debug,
            Env env) {
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

        CallProfiles.Profile profile = CallProfiles.load(name).orElseThrow(() -> new CliError(
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
            Options options,
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
                profile.sources, profile.configs, profile.service, profile.allowUnknownTraits);
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
        CallProfiles.save(name, new CallProfiles.Profile(
                profile.sources, profile.configs, profile.service, profile.dependencies,
                profile.allowUnknownTraits, profile.region, profile.url, profile.auth, compiledAt));
    }

    // ===================================================================================
    // Call
    // ===================================================================================

    private int doCall(String name, CallProfiles.Profile profile, String operation, Options options,
            boolean debug, Env env) {
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
            ContinueToken decoded = decodeContinueToken(options.continueToken, opName);
            input = decoded.input;
            // Pagination is endpoint-scoped: reuse the region the first page ran against so later pages
            // hit the same endpoint. An explicit --aws-region still overrides (escape hatch).
            if (options.awsRegion == null && decoded.region != null) {
                region = decoded.region;
            }
        } else {
            input = readInput(options);
        }

        // Streaming input payload (e.g. S3 PutObject Body): inject the file/stdin as a DataStream under
        // the operation's @streaming input member. --input supplies the other (non-payload) fields.
        String streamingInput = streamingInputMember(opShape, model);
        if (options.inputPayload != null) {
            if (streamingInput == null) {
                throw new CliError(opName + " has no streaming input member; --input-payload is not applicable.");
            }
            input = withStreamingPayload(input, streamingInput, options.inputPayload);
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
        // The response payload is streaming if the operation has a @streaming output member.
        boolean outputStreaming = hasStreamingOutput(opShape, model);

        WireCapture capture = new WireCapture(options.plan, wireFull, inputStreaming, outputStreaming,
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
                    ? resultWithBodyFile(result, streamingMember, options.outputPayload)
                    : result;
            // If this operation is @paginated and the response carries a next-page token, emit an
            // opaque @continue token (the verbatim input with the token injected) for the next page.
            String continueToken = nextContinueToken(opShape, model, opName, input, result, region);
            CallArtifacts.print(env, options.pretty, options.query, envelope(capture, wire, data, continueToken));
            if (continueToken != null) {
                // Also print a ready-to-paste next-page command to stderr. stderr (not stdout) so it
                // needs no JSON escaping, keeps stdout pure JSON, and -- crucially -- survives --query
                // (which can strip @continue out of the stdout JSON entirely). Flush stdout first so the
                // JSON is fully written before the hint, otherwise the two streams interleave.
                env.stdout().flush();
                printNextPageHint(env, name, opName, continueToken, options);
            }
            return 0;
        } catch (DryRunException e) {
            String protocolId = client.config().protocol().id().toString();
            CallArtifacts.print(env, options.pretty, options.query,
                    planDocument(capture, name, profile, opShape, serviceId, opName,
                            echoInput(input, streamingInput), protocolId, wire, model, region, options));
            return 0;
        } catch (DocumentException e) {
            CallArtifacts.print(env, options.pretty, options.query,
                    envelope(capture, wire, e.getContents(), errorMeta(shapeName(e), capture, true, null)));
            return 1;
        } catch (CallException e) {
            // Unmodeled errors are the case where you most need the wire to debug, so default to
            // showing @http headers even without --wire. --wire full still overrides to include bodies.
            String errWire = wire != null ? wire : "headers";
            CallArtifacts.print(env, options.pretty, options.query,
                    envelope(capture, errWire, null, errorMeta(null, capture, false, e.getMessage())));
            return 1;
        }
    }

    /** True if the operation's output has a @streaming member (response payload streams). */
    private boolean hasStreamingOutput(OperationShape op, Model model) {
        Shape struct = model.getShape(op.getOutputShape()).orElse(null);
        if (struct == null) {
            return false;
        }
        for (var member : struct.getAllMembers().values()) {
            Shape target = model.getShape(member.getTarget()).orElse(null);
            if (target != null && target.hasTrait("smithy.api#streaming")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sanitizes input for the dry-run echo: a streaming member holds a DataStream that can't be
     * serialized back to JSON, so replace it with a placeholder string.
     */
    private Document echoInput(Document input, String streamingMember) {
        if (input == null || streamingMember == null) {
            return input;
        }
        Map<String, Document> members = input.asStringMap();
        if (members == null || !members.containsKey(streamingMember)) {
            return input;
        }
        Map<String, Document> copy = new LinkedHashMap<>(members);
        copy.put(streamingMember, Document.of("<streaming payload>"));
        return Document.of(copy);
    }

    /** Name of the operation's {@code @streaming} input member, or null if it has none. */
    private String streamingInputMember(OperationShape op, Model model) {
        Shape struct = model.getShape(op.getInputShape()).orElse(null);
        if (struct == null) {
            return null;
        }
        for (var member : struct.getAllMembers().values()) {
            Shape target = model.getShape(member.getTarget()).orElse(null);
            if (target != null && target.hasTrait("smithy.api#streaming")) {
                return member.getMemberName();
            }
        }
        return null;
    }

    /**
     * Returns a copy of {@code input} with {@code member} set to a {@link DataStream} backed by the
     * given payload source ({@code -} for stdin, otherwise a file path). The dynamic client reads this
     * member via {@code asDataStream()} when serializing, so the body streams rather than buffering.
     */
    private Document withStreamingPayload(Document input, String member, String payloadSource) {
        DataStream stream;
        if (payloadSource.equals("-")) {
            stream = DataStream.ofInputStream(System.in);
        } else {
            Path path = Path.of(payloadSource);
            if (!Files.isRegularFile(path)) {
                throw new CliError("--input-payload file not found: " + payloadSource);
            }
            stream = DataStream.ofFile(path);
        }
        Map<String, Document> members = new LinkedHashMap<>();
        if (input != null) {
            Map<String, Document> existing = input.asStringMap();
            if (existing != null) {
                members.putAll(existing);
            }
        }
        members.put(member, Document.ofObject(stream));
        return Document.of(members);
    }

    /** Reads {@code --input}: a literal JSON string, {@code @file}, or {@code -} for stdin. Null if absent. */
    private Document readInput(Options options) {
        if (options.input == null) {
            return null;
        }
        byte[] bytes;
        String in = options.input;
        try {
            if (in.equals("-")) {
                bytes = readAll(System.in);
            } else if (in.startsWith("@")) {
                bytes = Files.readAllBytes(Path.of(in.substring(1)));
            } else {
                bytes = in.getBytes(StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new CliError("Unable to read --input: " + e.getMessage());
        }
        if (bytes.length == 0) {
            return null;
        }
        try (var deserializer = CODEC.createDeserializer(bytes)) {
            return deserializer.readDocument();
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    // ===================================================================================
    // Pagination (--continue token)
    // ===================================================================================

    /** Current @continue token format version. Bump when the token JSON shape changes. */
    private static final int CONTINUE_TOKEN_VERSION = 1;

    /**
     * If {@code op} is @paginated and {@code result} carries a non-null output token, builds the opaque
     * {@code @continue} token: base64 of {@code {"v":1,"op":...,"input":<verbatim input + injected token>}}.
     * Returns null when there is no next page.
     */
    private String nextContinueToken(OperationShape op, Model model, String opName, Document input, Document result,
            String region) {
        PaginatedTrait paginated = resolvePaginated(op, model);
        if (paginated == null) {
            return null;
        }
        String outputTokenPath = paginated.getOutputToken().orElse(null);
        String inputTokenMember = paginated.getInputToken().orElse(null);
        if (outputTokenPath == null || inputTokenMember == null) {
            return null;
        }
        // Read the output token value (supporting a dotted path, though most are a single member).
        Document tokenDoc = resolvePath(result, outputTokenPath);
        if (tokenDoc == null) {
            return null;
        }
        String tokenValue;
        try {
            tokenValue = tokenDoc.asString();
        } catch (RuntimeException e) {
            return null; // non-string token type; nothing to continue with
        }
        if (tokenValue == null || tokenValue.isEmpty()) {
            return null; // last page
        }

        // Verbatim original input + the next-page token injected at the input token member.
        Map<String, Document> nextInput = new LinkedHashMap<>();
        if (input != null) {
            Map<String, Document> existing = input.asStringMap();
            if (existing != null) {
                nextInput.putAll(existing);
            }
        }
        nextInput.put(inputTokenMember, Document.of(tokenValue));

        // Fully flat positional array keeps the opaque token compact:
        //   [version, operation, region, key1, value1, key2, value2, ...]
        // The region slot is "" when there's no region (a real region is never empty). Input top-level
        // members are flattened into alternating key/value entries; a value may itself be a nested
        // document (so structured inputs still round-trip).
        List<Document> token = new ArrayList<>(3 + nextInput.size() * 2);
        token.add(Document.of(CONTINUE_TOKEN_VERSION));
        token.add(Document.of(opName));
        token.add(Document.of(region != null ? region : ""));
        for (var entry : nextInput.entrySet()) {
            token.add(Document.of(entry.getKey()));
            token.add(entry.getValue());
        }

        ByteBuffer encoded = TOKEN_CODEC.serialize(Document.of(token));
        byte[] tokenBytes = new byte[encoded.remaining()];
        encoded.get(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Prints a ready-to-paste next-page command to stderr for the human in the loop. The token carries
     * the input and region; we re-add the flags that aren't in the token but affect the next call: the
     * credentials profile and presentation options (--pretty/--query/--wire). stderr so it survives
     * --query and needs no JSON escaping. Colored when the terminal supports it: a muted "next page"
     * label, the command in the literal style, and the long opaque token muted so the eye skips it.
     */
    private void printNextPageHint(Env env, String name, String opName, String continueToken, Options options) {
        var colors = env.colors();
        StringBuilder cmd = new StringBuilder("smithy call ").append(name).append(' ').append(opName);
        if (options.awsProfile != null) {
            cmd.append(" --aws-profile ").append(options.awsProfile);
        }
        if (options.pretty) {
            cmd.append(" --pretty");
        }
        if (options.wire != null) {
            cmd.append(" --wire ").append(options.wire);
        }
        if (options.query != null) {
            cmd.append(" --query '").append(options.query).append('\'');
        }
        // Put the label on its own line so the command sits alone on the next line -- a triple-click
        // (or double-click drag) selects just the runnable command without the label getting in the way.
        // Command in cyan, the long opaque token in yellow so the two parts are visually distinct.
        env.stderr().println("");
        env.stderr().println(colors.style("next page:", Style.BRIGHT_GREEN));
        env.stderr().println(colors.style(cmd + " --continue ", Style.CYAN)
                + colors.style(continueToken, Style.YELLOW));
    }

    /** The decoded contents of a {@code --continue} token: the input to send and the pinned region. */
    private static final class ContinueToken {
        final Document input;
        final String region;

        ContinueToken(Document input, String region) {
            this.input = input;
            this.region = region;
        }
    }

    /** Decodes a {@code --continue} token, validating its version and operation. */
    private ContinueToken decodeContinueToken(String token, String opName) {
        byte[] bytes;
        try {
            bytes = Base64.getUrlDecoder().decode(token);
        } catch (IllegalArgumentException e) {
            throw new CliError("Invalid --continue token (not valid base64).");
        }
        List<Document> fields;
        try {
            // Note: not try-with-resources. The CBOR deserializer's close() re-validates that the whole
            // buffer was consumed and throws "Unexpected CBOR content at end of object" for a top-level
            // document even after a clean read, so we read without auto-closing.
            fields = TOKEN_CODEC.createDeserializer(bytes).readDocument().asList();
        } catch (RuntimeException e) {
            throw new CliError("Invalid --continue token (not valid token data).");
        }
        // Layout: [version, operation, region, key1, value1, key2, value2, ...] -- 3 header entries
        // followed by an even number of key/value entries.
        if (fields == null || fields.size() < 3 || (fields.size() - 3) % 2 != 0) {
            throw new CliError("Invalid --continue token (unexpected structure).");
        }
        int version = fields.get(0).asNumber().intValue();
        if (version != CONTINUE_TOKEN_VERSION) {
            throw new CliError("Unsupported --continue token version " + version + " (this CLI emits v"
                    + CONTINUE_TOKEN_VERSION + "); regenerate the token by re-running the first call.");
        }
        String tokenOp = fields.get(1).asString();
        if (tokenOp != null && !tokenOp.equals(opName)) {
            throw new CliError("--continue token is for operation '" + tokenOp + "', not '" + opName + "'.");
        }
        String region = fields.get(2).asString();
        if (region != null && region.isEmpty()) {
            region = null;
        }
        Map<String, Document> input = new LinkedHashMap<>();
        for (int i = 3; i + 1 < fields.size(); i += 2) {
            input.put(fields.get(i).asString(), fields.get(i + 1));
        }
        return new ContinueToken(Document.of(input), region);
    }

    /** Resolves a (possibly dotted) member path within a Document, or null if any segment is absent. */
    private Document resolvePath(Document doc, String path) {
        Document current = doc;
        for (String segment : path.split("\\.")) {
            if (current == null) {
                return null;
            }
            current = current.getMember(segment);
        }
        return current;
    }

    /** Merges the @paginated trait from the operation with the service-level default, or null if none. */
    private PaginatedTrait resolvePaginated(OperationShape op, Model model) {
        PaginatedTrait opTrait = op.getTrait(PaginatedTrait.class).orElse(null);
        TopDownIndex index = TopDownIndex.of(model);
        PaginatedTrait svcTrait = model.getServiceShapes().stream()
                .filter(s -> index.getContainedOperations(s).contains(op))
                .findFirst()
                .flatMap(s -> s.getTrait(PaginatedTrait.class))
                .orElse(null);
        if (opTrait == null) {
            return svcTrait;
        }
        return svcTrait == null ? opTrait : opTrait.merge(svcTrait);
    }

    private DynamicClient buildClient(
            String name,
            CallProfiles.Profile profile,
            Model model,
            ShapeId serviceId,
            String region,
            Options options,
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
            builder.protocol(createProtocol(options.protocol, serviceId));
        }

        Shape serviceShape = model.expectShape(serviceId);
        configureAuth(builder, serviceId, serviceShape, profile.auth, region, options.plan);
        return builder.build();
    }

    /** Builds a client protocol for the {@code --protocol} override. Accepts a few spelling variants. */
    private ClientProtocol<?, ?> createProtocol(String protocol, ShapeId serviceId) {
        switch (protocol.toLowerCase(Locale.ROOT).replace("_", "-")) {
            case "rest-json":
            case "restjson1":
            case "aws.protocols#restjson1":
                return new RestJsonClientProtocol(serviceId);
            case "aws-json":
            case "awsjson":
            case "awsjson1":
                return new AwsJson1Protocol(serviceId);
            case "rest-xml":
            case "restxml":
            case "aws.protocols#restxml":
                return new RestXmlClientProtocol(serviceId);
            case "rpc-v2-cbor":
            case "rpcv2cbor":
            case "smithy.protocols#rpcv2cbor":
                return new RpcV2CborProtocol(serviceId);
            case "rpc-v2-json":
            case "rpcv2json":
            case "smithy.protocols#rpcv2json":
                return new RpcV2JsonProtocol(serviceId);
            default:
                throw new CliError("Unknown --protocol '" + protocol + "'. Supported: rest-json, aws-json, "
                        + "rest-xml, rpc-v2-cbor, rpc-v2-json.");
        }
    }

    private void configureAuth(DynamicClient.Builder builder, ShapeId serviceId, Shape serviceShape,
            String authMode, String region, boolean dryrun) {
        if (authMode == null) {
            // Auto-detect from the model. Register credentials so a detected SigV4 scheme can sign.
            if (serviceShape.hasTrait(SigV4Trait.class) && region == null) {
                throw new CliError("This service uses SigV4, which requires a region. Set --aws-region, "
                        + "AWS_REGION, register a region, or set one in your AWS profile.");
            }
            builder.addIdentityResolver(resolveCredentials(dryrun));
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
                        .addIdentityResolver(resolveCredentials(dryrun));
                break;
            case "none":
                builder.authSchemeResolver(AuthSchemeResolver.NO_AUTH);
                break;
            default:
                throw new CliError("Unsupported auth mode: " + authMode);
        }
    }

    // Credentials are ambient runtime identity (env vars or AWS profile), set once when the client is
    // built. For --plan, nothing is sent, so we use placeholder credentials -- the request is still
    // built and signed (with a throwaway signature) so it can be inspected, without requiring real creds.
    private IdentityResolver<?> resolveCredentials(boolean dryrun) {
        if (dryrun) {
            // Placeholder credentials so the request can be built + signed for inspection without real
            // creds. Must advertise the AwsCredentialsIdentity *interface* as its identity type (the
            // SigV4 scheme looks resolvers up by interface; IdentityResolver.of would report the impl
            // class and the lookup would miss).
            return new StaticAwsCredentialsResolver(
                    AwsCredentialsIdentity.create("AKIDRYRUNEXAMPLE", "dryrun-placeholder-secret-key"));
        }
        if (System.getProperty("aws.profile") != null) {
            return CredentialChain.create(AwsCredentialsIdentity.class);
        }
        return EnvironmentVariableIdentityResolver.INSTANCE;
    }

    /** A fixed-credentials resolver that reports the AwsCredentialsIdentity interface as its type. */
    private static final class StaticAwsCredentialsResolver
            implements IdentityResolver<AwsCredentialsIdentity> {
        private final AwsCredentialsIdentity identity;

        StaticAwsCredentialsResolver(AwsCredentialsIdentity identity) {
            this.identity = identity;
        }

        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public IdentityResult<AwsCredentialsIdentity> resolveIdentity(Context requestProperties) {
            return IdentityResult.of(identity);
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

    private String resolveRegion(Options options, CallProfiles.Profile profile) {
        Sourced r = resolveRegionSourced(options, profile);
        return r == null ? null : r.value;
    }

    /** Region precedence: --aws-region, AWS_REGION, AWS_DEFAULT_REGION, registered region, AWS profile. */
    private Sourced resolveRegionSourced(Options options, CallProfiles.Profile profile) {
        if (options.awsProfile != null) {
            System.setProperty("aws.profile", options.awsProfile);
        }
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
    private Sourced resolveProfileSourced(Options options) {
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

    private int helpJson(String name, CallProfiles.Profile profile, String operation, Options options, Env env) {
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
        TopDownIndex.of(model).getContainedOperations(serviceId).stream()
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
        return 0;
    }

    private void printShapeMembersText(Model model, ShapeId shapeId, software.amazon.smithy.cli.CliPrinter out) {
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

    private int serviceHelpJson(Model model, ShapeId serviceId, Options options, Env env) {
        ServiceShape service = model.expectShape(serviceId, ServiceShape.class);
        Map<String, Document> out = new LinkedHashMap<>();
        out.put("service", Document.of(serviceId.toString()));
        service.getTrait(DocumentationTrait.class)
                .ifPresent(d -> out.put("documentation", Document.of(d.getValue())));
        out.put("version", Document.of(service.getVersion() == null ? "" : service.getVersion()));
        List<Document> ops = TopDownIndex.of(model).getContainedOperations(serviceId).stream()
                .map(op -> op.getId().getName())
                .sorted()
                .map(Document::of)
                .collect(Collectors.toList());
        out.put("operations", Document.of(ops));
        CallArtifacts.print(env, options.pretty, options.query, Document.of(out));
        return 0;
    }

    private int operationHelpJson(Model model, ShapeId serviceId, String operation, Options options, Env env) {
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
                    + "  smithy call s3 ListBuckets\n"
                    + "  smithy call s3 ListObjectsV2 --input '{\"Bucket\":\"my-bucket\"}'\n"
                    + "  smithy call s3 ListObjectsV2 -i @input.json --pretty\n"
                    + "  echo '{\"Bucket\":\"b\"}' | smithy call s3 ListObjectsV2 -i -\n"
                    + "\n"
                    + "  # Inspect without sending: build and sign the request, then print it.\n"
                    + "  smithy call s3 ListObjectsV2 -i '{\"Bucket\":\"b\"}' --plan\n"
                    + "\n"
                    + "  # Filter output with JMESPath, or follow pagination with the printed token:\n"
                    + "  smithy call s3 ListObjectsV2 -i '{\"Bucket\":\"b\"}' -q 'Contents[].Key'\n"
                    + "  smithy call s3 ListObjectsV2 --continue <TOKEN>\n"
                    + "\n"
                    + "  # Discover operations and their input/output shapes:\n"
                    + "  smithy call s3 --help                # list operations\n"
                    + "  smithy call s3 ListObjectsV2 --help  # show input/output";

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

    private Document envelope(WireCapture capture, String wire, Document modeled, Map<String, Document> errorMeta,
            String continueToken) {
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

    /** Writes a streaming output member to the payload file and replaces it in the result with the path. */
    private Document resultWithBodyFile(Document result, String bodyMember, String outputFile) {
        Document bodyDoc = result.getMember(bodyMember);
        if (bodyDoc != null) {
            try (var out = Files.newOutputStream(Path.of(outputFile))) {
                bodyDoc.asDataStream().writeTo(out);
            } catch (IOException e) {
                throw new CliError("Unable to write --output-payload " + outputFile + ": " + e.getMessage());
            }
        }
        Map<String, Document> out = new LinkedHashMap<>();
        Map<String, Document> members = result.asStringMap();
        if (members != null) {
            for (var entry : members.entrySet()) {
                out.put(entry.getKey(),
                        entry.getKey().equals(bodyMember) ? Document.of(outputFile) : entry.getValue());
            }
        }
        out.putIfAbsent(bodyMember, Document.of(outputFile));
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
    private Document planDocument(WireCapture capture, String name, CallProfiles.Profile profile,
            OperationShape opShape, ShapeId serviceId, String operation, Document input, String protocolId,
            String wire, Model model, String region, Options options) {
        Map<String, Document> out = new LinkedHashMap<>();
        out.put("service", Document.of(serviceId.toString()));
        out.put("operation", Document.of(operation));
        if (protocolId != null) {
            out.put("protocol", Document.of(protocolId));
        }
        out.put("config", configDocument(options, profile));
        out.put("endpoint", endpointWithParameters(capture, model, serviceId, input, region));
        out.put("auth", capture.authDocument());
        out.put("safety", safetyDocument(opShape, model, serviceId));
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
    private Document endpointWithParameters(WireCapture capture, Model model, ShapeId serviceId, Document input,
            String region) {
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
            software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters params =
                    endpointParameters(model, serviceId);
            if (params == null) {
                return base;
            }
            Map<String, Document> contextValues = contextParamValues(model, serviceId, input);
            paramValues = new LinkedHashMap<>();
            for (var p : params.toList()) {
                String pName = p.getNameString();
                Document value = contextValues.get(pName);
                if (value == null && "AWS::Region".equals(p.getBuiltIn().orElse(null)) && region != null) {
                    value = Document.of(region);
                }
                if (value == null && p.getDefault().isPresent()) {
                    value = valueToDocument(p.getDefault().get());
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
    private software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters endpointParameters(Model model,
            ShapeId serviceId) {
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
    private Map<String, Document> contextParamValues(Model model, ShapeId serviceId, Document input) {
        Map<String, Document> result = new LinkedHashMap<>();
        if (input == null) {
            return result;
        }
        Map<String, Document> members = input.asStringMap();
        if (members == null) {
            return result;
        }
        // The input shape's members carry @contextParam(name: "<EndpointParam>").
        for (OperationShape op : TopDownIndex.of(model).getContainedOperations(serviceId)) {
            Shape in = model.getShape(op.getInputShape()).orElse(null);
            if (in == null) {
                continue;
            }
            in.getAllMembers().forEach((memberName, member) -> {
                var cp = member.getTrait(software.amazon.smithy.rulesengine.traits.ContextParamTrait.class);
                if (cp.isPresent() && members.containsKey(memberName)) {
                    result.put(cp.get().getName(), members.get(memberName));
                }
            });
        }
        return result;
    }

    private Document valueToDocument(
            software.amazon.smithy.rulesengine.language.evaluation.value.Value value) {
        Object o = value.toObject();
        if (o instanceof Boolean b) {
            return Document.of(b);
        }
        if (o instanceof Number n) {
            return Document.ofNumber(n);
        }
        return Document.of(String.valueOf(o));
    }

    /** Reports each resolved config value and where it came from. */
    private Document configDocument(Options options, CallProfiles.Profile profile) {
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

    /**
     * Safety summary derived from structural signals, strongest first:
     * <ol>
     *   <li>Resource lifecycle binding (@resource read/list/get vs create/put/update/delete) -- the
     *       authoritative semantic role of the operation.</li>
     *   <li>The @readonly and @idempotent traits.</li>
     *   <li>An @idempotencyToken member (client-supplied dedup token) and the @retryable trait.</li>
     *   <li>Name-prefix heuristics, only as a last resort.</li>
     * </ol>
     * {@code basis} reports the strongest signal used (resource | trait | heuristic) so consumers know
     * how much to trust it.
     */
    private Document safetyDocument(OperationShape op, Model model, ShapeId serviceId) {
        String lifecycle = lifecycleRole(op, model, serviceId); // read|list|get|create|put|update|delete|null
        boolean readonlyTrait = op.hasTrait(ReadonlyTrait.class);
        boolean idempotentTrait = op.hasTrait(IdempotentTrait.class);
        boolean hasIdempotencyToken = inputHasIdempotencyToken(op, model);
        boolean retryable = op.hasTrait(RetryableTrait.class);
        String n = op.getId().getName().toLowerCase(Locale.ROOT);

        // readonly
        Boolean readonly = null;
        String basis;
        if (lifecycle != null) {
            readonly = lifecycle.equals("read") || lifecycle.equals("list");
            basis = "resource";
        } else if (readonlyTrait) {
            readonly = true;
            basis = "trait";
        } else {
            readonly = nameLooksReadonly(n);
            basis = "heuristic";
        }

        // destructive
        boolean destructive;
        if ("delete".equals(lifecycle)) {
            destructive = true;
        } else if (Boolean.TRUE.equals(readonly)) {
            destructive = false;
        } else {
            destructive = n.startsWith("delete") || n.startsWith("remove") || n.startsWith("terminate")
                    || n.startsWith("purge") || n.startsWith("destroy");
        }

        // idempotent: definitionally true for read/list/put/update/delete lifecycle slots; @idempotent
        // trait or an @idempotencyToken input member also imply it. create (without a token) is not.
        Boolean idempotent;
        if (idempotentTrait || hasIdempotencyToken || Boolean.TRUE.equals(readonly)) {
            idempotent = true;
        } else if (lifecycle != null) {
            idempotent = !lifecycle.equals("create"); // put/update/delete are idempotent; create is not
        } else {
            idempotent = null; // unknown
        }

        Map<String, Document> s = new LinkedHashMap<>();
        s.put("readonly", Document.of(readonly));
        s.put("mutating", Document.of(!readonly));
        s.put("destructive", Document.of(destructive));
        s.put("idempotent", idempotent == null ? Document.ofObject(null) : Document.of(idempotent));
        s.put("idempotencyToken", Document.of(hasIdempotencyToken));
        s.put("retryable", Document.of(retryable));
        if (lifecycle != null) {
            s.put("lifecycle", Document.of(lifecycle));
        }
        s.put("basis", Document.of(basis));
        return Document.of(s);
    }

    private boolean nameLooksReadonly(String n) {
        return n.startsWith("describe") || n.startsWith("list") || n.startsWith("get")
                || n.startsWith("batchget") || n.startsWith("query") || n.startsWith("scan")
                || n.startsWith("search") || n.startsWith("head") || n.startsWith("lookup");
    }

    /** The resource lifecycle slot this operation fills (read/list/create/put/update/delete), or null. */
    private String lifecycleRole(OperationShape op, Model model, ShapeId serviceId) {
        var binding = BottomUpIndex.of(model)
                .getResourceBinding(serviceId, op);
        if (binding.isEmpty()) {
            return null;
        }
        var r = binding.get();
        ShapeId id = op.getId();
        if (r.getRead().filter(id::equals).isPresent()) {
            return "read";
        }
        if (r.getList().filter(id::equals).isPresent()) {
            return "list";
        }
        if (r.getCreate().filter(id::equals).isPresent()) {
            return "create";
        }
        if (r.getPut().filter(id::equals).isPresent()) {
            return "put";
        }
        if (r.getUpdate().filter(id::equals).isPresent()) {
            return "update";
        }
        if (r.getDelete().filter(id::equals).isPresent()) {
            return "delete";
        }
        return null; // bound as a non-lifecycle operation
    }

    /** True if any input member carries @idempotencyToken (client-supplied dedup token). */
    private boolean inputHasIdempotencyToken(OperationShape op, Model model) {
        Shape input = model.getShape(op.getInputShape()).orElse(null);
        if (input == null) {
            return false;
        }
        for (var member : input.getAllMembers().values()) {
            if (member.hasTrait(IdempotencyTokenTrait.class)) {
                return true;
            }
        }
        return false;
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
        byte[] body;            // null if streaming or absent
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
            return c.contains("json") || c.contains("xml") || c.startsWith("text/")
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
        if (stream.hasKnownLength() || true) {
            ByteBuffer buf = stream.asByteBuffer();
            int n = buf.remaining();
            int take = Math.min(n, WIRE_BODY_LIMIT);
            byte[] bytes = new byte[take];
            buf.duplicate().get(bytes);
            msg.body = bytes;
            msg.truncated = n > take;
        }
    }

    /**
     * Captures the request (and, for real calls, the response) for the {@code @http} block and for
     * {@code --plan}. With dryrun set, aborts before transmit. The response body is replayed back
     * into the pipeline so capturing it doesn't disturb deserialization. Streaming bodies are flagged
     * and never buffered.
     */
    private static final class WireCapture implements ClientInterceptor, BddTraceSink {
        private final boolean dryrun;
        private final boolean wantFull;       // --wire full
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

        WireCapture(boolean dryrun, boolean wantFull, boolean reqStreaming, boolean respStreaming,
                boolean traceConditions) {
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
                if (!java.util.Objects.equals(prev.get(e.getKey()), e.getValue())) {
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
                ByteBuffer buffer = body.asByteBuffer();
                // We always read the response body here (to capture @rawBody / @http and to replay it).
                response.contentType = body.contentType();
                response.contentLength = body.contentLength();
                response.streaming = respStreaming;
                if (!respStreaming) {
                    int n = buffer.remaining();
                    int take = Math.min(n, WIRE_BODY_LIMIT);
                    byte[] bytes = new byte[take];
                    buffer.duplicate().get(bytes);
                    response.body = bytes;
                    response.truncated = n > take;
                }
                haveResponse = true;
                // Replay the (consumed) body so deserialization still works.
                HttpResponse replayed = HttpResponse.of(resp.httpVersion(), resp.statusCode(),
                        resp.headers(), DataStream.ofByteBuffer(buffer.duplicate(), body.contentType()));
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
            CODEC.createSerializer(new ByteArrayOutputStream()).flush();
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
