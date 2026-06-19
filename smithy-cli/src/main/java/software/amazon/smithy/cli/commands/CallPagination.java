/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.Style;
import software.amazon.smithy.java.cbor.Rpcv2CborCodec;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.PaginatedTrait;

/** Pagination token encoding, validation, and next-page command rendering for {@code smithy call}. */
final class CallPagination {
    private static final int TOKEN_VERSION = 2;
    private static final Rpcv2CborCodec TOKEN_CODEC = Rpcv2CborCodec.builder().build();

    private CallPagination() {}

    static final class Decoded {
        final Document input;
        final String region;
        final String url;
        final String protocol;

        Decoded(Document input, String region, String url, String protocol) {
            this.input = input;
            this.region = region;
            this.url = url;
            this.protocol = protocol;
        }
    }

    static String nextToken(
            OperationShape operation,
            Model model,
            String registration,
            String serviceId,
            String operationName,
            Document input,
            Document result,
            String region,
            String url,
            String protocol
    ) {
        PaginatedTrait paginated = resolvePaginated(operation, model);
        if (paginated == null) {
            return null;
        }
        String outputTokenPath = paginated.getOutputToken().orElse(null);
        String inputTokenMember = paginated.getInputToken().orElse(null);
        if (outputTokenPath == null || inputTokenMember == null) {
            return null;
        }
        Document tokenDocument = resolvePath(result, outputTokenPath);
        if (tokenDocument == null) {
            return null;
        }
        String tokenValue;
        try {
            tokenValue = tokenDocument.asString();
        } catch (RuntimeException e) {
            return null;
        }
        if (tokenValue == null || tokenValue.isEmpty()) {
            return null;
        }

        Map<String, Document> nextInput = new LinkedHashMap<>();
        if (input != null && input.asStringMap() != null) {
            nextInput.putAll(input.asStringMap());
        }
        nextInput.put(inputTokenMember, Document.of(tokenValue));

        List<Document> fields = new ArrayList<>(7 + nextInput.size() * 2);
        fields.add(Document.of(TOKEN_VERSION));
        fields.add(Document.of(registration));
        fields.add(Document.of(serviceId));
        fields.add(Document.of(operationName));
        fields.add(Document.of(region == null ? "" : region));
        fields.add(Document.of(url == null ? "" : url));
        fields.add(Document.of(protocol == null ? "" : protocol));
        nextInput.forEach((key, value) -> {
            fields.add(Document.of(key));
            fields.add(value);
        });
        ByteBuffer encoded = TOKEN_CODEC.serialize(Document.of(fields));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static Decoded decode(String token, String registration, String serviceId, String operationName) {
        byte[] bytes;
        try {
            bytes = Base64.getUrlDecoder().decode(token);
        } catch (IllegalArgumentException e) {
            throw new CliError("Invalid --continue token (not valid base64).");
        }
        List<Document> fields;
        try {
            fields = TOKEN_CODEC.createDeserializer(bytes).readDocument().asList();
        } catch (RuntimeException e) {
            throw new CliError("Invalid --continue token (not valid token data).");
        }
        if (fields == null || fields.size() < 7 || (fields.size() - 7) % 2 != 0) {
            throw new CliError("Invalid --continue token (unexpected structure).");
        }
        int version;
        String tokenRegistration;
        String tokenServiceId;
        String tokenOperation;
        String region;
        String url;
        String protocol;
        try {
            version = fields.get(0).asNumber().intValue();
            tokenRegistration = fields.get(1).asString();
            tokenServiceId = fields.get(2).asString();
            tokenOperation = fields.get(3).asString();
            region = fields.get(4).asString();
            url = fields.get(5).asString();
            protocol = fields.get(6).asString();
        } catch (RuntimeException e) {
            throw new CliError("Invalid --continue token (unexpected field types).");
        }
        if (version != TOKEN_VERSION) {
            throw new CliError("Unsupported --continue token version " + version + " (this CLI emits v"
                    + TOKEN_VERSION + "); regenerate it by re-running the first call.");
        }
        if (!registration.equals(tokenRegistration) || !serviceId.equals(tokenServiceId)) {
            throw new CliError("--continue token is for registered service '" + tokenRegistration
                    + "' (" + tokenServiceId + "), not '" + registration + "' (" + serviceId + ").");
        }
        if (!operationName.equals(tokenOperation)) {
            throw new CliError("--continue token is for operation '" + tokenOperation + "', not '"
                    + operationName + "'.");
        }
        if (region != null && region.isEmpty()) {
            region = null;
        }
        if (url != null && url.isEmpty()) {
            url = null;
        }
        if (protocol != null && protocol.isEmpty()) {
            protocol = null;
        }
        Map<String, Document> input = new LinkedHashMap<>();
        try {
            for (int i = 7; i < fields.size(); i += 2) {
                input.put(fields.get(i).asString(), fields.get(i + 1));
            }
        } catch (RuntimeException e) {
            throw new CliError("Invalid --continue token (unexpected input field types).");
        }
        return new Decoded(Document.of(input), region, url, protocol);
    }

    static void printHint(
            Command.Env env,
            String registration,
            String operation,
            String token,
            CallOptions options
    ) {
        var colors = env.colors();
        StringBuilder command = new StringBuilder("smithy call ")
                .append(registration)
                .append(' ')
                .append(operation);
        if (options.awsProfile != null) {
            command.append(" --aws-profile '").append(options.awsProfile.replace("'", "'\\''")).append('\'');
        }
        if (options.url != null) {
            command.append(" --url '").append(options.url.replace("'", "'\\''")).append('\'');
        }
        if (options.protocol != null) {
            command.append(" --protocol '").append(options.protocol.replace("'", "'\\''")).append('\'');
        }
        if (options.pretty) {
            command.append(" --pretty");
        }
        if (options.wire != null) {
            command.append(" --wire ").append(options.wire);
        }
        if (options.query != null) {
            command.append(" --query '").append(options.query.replace("'", "'\\''")).append('\'');
        }
        env.stderr().println("");
        env.stderr().println(colors.style("next page:", Style.BRIGHT_GREEN));
        env.stderr()
                .println(colors.style(command + " --continue ", Style.CYAN)
                        + colors.style(token, Style.YELLOW));
    }

    private static Document resolvePath(Document document, String path) {
        Document current = document;
        for (String segment : path.split("\\.")) {
            if (current == null) {
                return null;
            }
            current = current.getMember(segment);
        }
        return current;
    }

    private static PaginatedTrait resolvePaginated(OperationShape operation, Model model) {
        PaginatedTrait operationTrait = operation.getTrait(PaginatedTrait.class).orElse(null);
        TopDownIndex index = TopDownIndex.of(model);
        PaginatedTrait serviceTrait = model.getServiceShapes()
                .stream()
                .filter(service -> index.getContainedOperations(service).contains(operation))
                .findFirst()
                .flatMap(service -> service.getTrait(PaginatedTrait.class))
                .orElse(null);
        if (operationTrait == null) {
            return serviceTrait;
        }
        return serviceTrait == null ? operationTrait : operationTrait.merge(serviceTrait);
    }
}
