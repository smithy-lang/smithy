/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Manages registered {@code call} services: an index in {@code ~/.config/smithy.json} plus per-service
 * derived build artifacts under {@code ~/.config/smithy/registrations/<name>/}.
 *
 * <p>Registration is a build step: the model is assembled once and combined/derived artifacts are
 * written so ordinary calls avoid model assembly and validation. Stale registrations are rebuilt from
 * their captured source/config context before a call.
 *
 * <p>Index entry (in {@code ~/.config/smithy.json} under {@code "call"}):
 * <pre>{@code
 * {
 *   "call": {
 *     "s3": {
 *       "sources": ["/abs/s3.json"],   // source model paths/dirs, for staleness checks + recompile
 *       "configs": ["/abs/smithy-build.json"], // resolved config paths (absolute), for headless recompile
 *       "service": "com.amazonaws.s3#AmazonS3",
 *       "dependencies": ["com.example:traits:1.0"], // Maven coords from config, for call-time SPI + recompile
 *       "allowUnknownTraits": true,     // whether --aut was set at registration
 *       "region": "us-east-1",
 *       "url": null,
 *       "auth": "sigv4",
 *       "compiledAt": 1700000000000     // epoch millis the artifacts were last built
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>Artifacts (in {@code ~/.config/smithy/registrations/<name>/}):
 * <ul>
 *   <li>{@code model.json}: combined JSON AST (with docs), used for {@code --help}.</li>
 *   <li>{@code model-no-docs.json}: combined AST without documentation traits, used for calls.</li>
 *   <li>{@code endpoint.bdd}: precompiled endpoint rules bytecode, when the service defines rules.</li>
 * </ul>
 */
final class CallProfiles {

    private static final String VALID_NAME = "[A-Za-z0-9][A-Za-z0-9._-]*";

    static final class Profile {
        final List<String> sources;
        final List<String> configs;
        final String service;
        final List<String> dependencies;
        final boolean allowUnknownTraits;
        final String region;
        final String url;
        final String auth;
        final long compiledAt;

        Profile(
                List<String> sources,
                List<String> configs,
                String service,
                List<String> dependencies,
                boolean allowUnknownTraits,
                String region,
                String url,
                String auth,
                long compiledAt
        ) {
            this.sources = sources;
            this.configs = configs;
            this.service = service;
            this.dependencies = dependencies;
            this.allowUnknownTraits = allowUnknownTraits;
            this.region = region;
            this.url = url;
            this.auth = auth;
            this.compiledAt = compiledAt;
        }

        ObjectNode toNode() {
            ObjectNode.Builder b = Node.objectNodeBuilder();
            if (!sources.isEmpty()) {
                b.withMember("sources", Node.fromStrings(sources));
            }
            if (!configs.isEmpty()) {
                b.withMember("configs", Node.fromStrings(configs));
            }
            if (service != null) {
                b.withMember("service", service);
            }
            if (!dependencies.isEmpty()) {
                b.withMember("dependencies", Node.fromStrings(dependencies));
            }
            if (allowUnknownTraits) {
                b.withMember("allowUnknownTraits", true);
            }
            if (region != null) {
                b.withMember("region", region);
            }
            if (url != null) {
                b.withMember("url", url);
            }
            if (auth != null) {
                b.withMember("auth", auth);
            }
            b.withMember("compiledAt", Node.from(compiledAt));
            return b.build();
        }

        software.amazon.smithy.java.core.serde.document.Document toNodeDocument() {
            Map<String, software.amazon.smithy.java.core.serde.document.Document> m = new LinkedHashMap<>();
            if (!sources.isEmpty()) {
                m.put("sources", docList(sources));
            }
            if (!configs.isEmpty()) {
                m.put("configs", docList(configs));
            }
            if (service != null) {
                m.put("service", doc(service));
            }
            if (!dependencies.isEmpty()) {
                m.put("dependencies", docList(dependencies));
            }
            if (allowUnknownTraits) {
                m.put("allowUnknownTraits", software.amazon.smithy.java.core.serde.document.Document.of(true));
            }
            if (region != null) {
                m.put("region", doc(region));
            }
            if (url != null) {
                m.put("url", doc(url));
            }
            if (auth != null) {
                m.put("auth", doc(auth));
            }
            return software.amazon.smithy.java.core.serde.document.Document.of(m);
        }

        private static software.amazon.smithy.java.core.serde.document.Document doc(String v) {
            return software.amazon.smithy.java.core.serde.document.Document.of(v);
        }

        private static software.amazon.smithy.java.core.serde.document.Document docList(List<String> values) {
            List<software.amazon.smithy.java.core.serde.document.Document> docs = new ArrayList<>();
            for (String v : values) {
                docs.add(doc(v));
            }
            return software.amazon.smithy.java.core.serde.document.Document.of(docs);
        }

        static Profile fromNode(ObjectNode node) {
            return new Profile(
                    stringList(node, "sources"),
                    stringList(node, "configs"),
                    node.getStringMemberOrDefault("service", null),
                    stringList(node, "dependencies"),
                    node.getBooleanMemberOrDefault("allowUnknownTraits", false),
                    node.getStringMemberOrDefault("region", null),
                    node.getStringMemberOrDefault("url", null),
                    node.getStringMemberOrDefault("auth", null),
                    node.getNumberMember("compiledAt").map(n -> n.getValue().longValue()).orElse(0L));
        }

        private static List<String> stringList(ObjectNode node, String member) {
            List<String> result = new ArrayList<>();
            node.getArrayMember(member).ifPresent(array -> {
                for (var element : array.getElements()) {
                    result.add(element.expectStringNode().getValue());
                }
            });
            return result;
        }
    }

    private CallProfiles() {}

    /** Config index file: {@code $XDG_CONFIG_HOME/smithy.json} or {@code ~/.config/smithy.json}. */
    static Path configPath() {
        return configDir().resolve("smithy.json");
    }

    private static Path configDir() {
        String xdg = System.getenv("XDG_CONFIG_HOME");
        return (xdg != null && !xdg.isEmpty())
                ? Paths.get(xdg)
                : Paths.get(System.getProperty("user.home"), ".config");
    }

    /** Directory holding a registration's derived artifacts. */
    static Path artifactDir(String name) {
        validateName(name);
        Path registrations = configDir().resolve("smithy").resolve("registrations").toAbsolutePath().normalize();
        Path result = registrations.resolve(name).normalize();
        if (!java.util.Objects.equals(result.getParent(), registrations)) {
            throw new CliError("Invalid registration name: " + name);
        }
        return result;
    }

    /**
     * Validates the identifier used as both a config key and an artifact directory name. Keeping this
     * deliberately conservative makes names shell-friendly and prevents path traversal.
     */
    static void validateName(String name) {
        if (name == null || !name.matches(VALID_NAME)) {
            throw new CliError("Invalid registration name '" + name + "'. Names must start with an ASCII letter "
                    + "or digit and contain only letters, digits, '.', '_', or '-'.");
        }
    }

    static Path modelArtifact(String name) {
        return artifactDir(name).resolve("model.json");
    }

    static Path modelNoDocsArtifact(String name) {
        return artifactDir(name).resolve("model-no-docs.json");
    }

    static Path bytecodeArtifact(String name) {
        return artifactDir(name).resolve("endpoint.bdd");
    }

    /** Loads a single registration by name, or empty if absent. */
    static Optional<Profile> load(String name) {
        validateName(name);
        return loadAll()
                .flatMap(call -> call.getObjectMember(name))
                .map(Profile::fromNode);
    }

    /** Loads all registrations keyed by name, or empty if the config file does not exist. */
    static Optional<Map<String, Profile>> loadAllProfiles() {
        return loadAll().map(call -> {
            Map<String, Profile> result = new LinkedHashMap<>();
            for (var entry : call.getStringMap().entrySet()) {
                result.put(entry.getKey(), Profile.fromNode(entry.getValue().expectObjectNode()));
            }
            return result;
        });
    }

    private static Optional<ObjectNode> loadAll() {
        Path path = configPath();
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        ObjectNode root = readRoot(path);
        return Optional.of(root.getObjectMember("call").orElse(Node.objectNode()));
    }

    private static ObjectNode readRoot(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            if (content.isBlank()) {
                return Node.objectNode();
            }
            return Node.parse(content, path.toString()).expectObjectNode();
        } catch (IOException e) {
            throw new CliError("Unable to read " + path + ": " + e.getMessage());
        }
    }

    /** Writes (or overwrites) a registration's index entry, preserving other config and entries. */
    static void save(String name, Profile profile) {
        validateName(name);
        withConfigLock(() -> {
            Path path = configPath();
            ObjectNode root = Files.isRegularFile(path) ? readRoot(path) : Node.objectNode();
            ObjectNode call = root.getObjectMember("call").orElse(Node.objectNode());
            call = call.withMember(name, profile.toNode());
            write(path, root.withMember("call", call));
        });
    }

    /** Removes a registration (index entry + derived artifacts); returns true if it existed. */
    static boolean remove(String name) {
        validateName(name);
        boolean[] existed = {false};
        withConfigLock(() -> {
            Path path = configPath();
            if (Files.isRegularFile(path)) {
                ObjectNode root = readRoot(path);
                ObjectNode call = root.getObjectMember("call").orElse(Node.objectNode());
                if (call.containsMember(name)) {
                    existed[0] = true;
                    call = call.withoutMember(name);
                    write(path, root.withMember("call", call));
                }
            }
            deleteArtifacts(name);
        });
        return existed[0];
    }

    private static void deleteArtifacts(String name) {
        Path dir = artifactDir(name);
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best-effort cleanup
                }
            });
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }

    private static void write(Path path, ObjectNode root) {
        try {
            Path parent = java.util.Objects.requireNonNull(path.getParent());
            String fileName = java.util.Objects.requireNonNull(path.getFileName()).toString();
            Files.createDirectories(parent);
            Path temporary = Files.createTempFile(parent, fileName, ".tmp");
            try {
                Files.writeString(temporary,
                        Node.prettyPrintJson(root) + System.lineSeparator(),
                        StandardCharsets.UTF_8);
                moveAtomically(temporary, path);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException e) {
            throw new CliError("Unable to write " + path + ": " + e.getMessage());
        }
    }

    static void writeArtifact(Path path, byte[] contents) throws IOException {
        Path parent = java.util.Objects.requireNonNull(path.getParent());
        String fileName = java.util.Objects.requireNonNull(path.getFileName()).toString();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, fileName, ".tmp");
        try {
            Files.write(temporary, contents);
            moveAtomically(temporary, path);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static synchronized void withConfigLock(Runnable action) {
        Path lockPath = configDir().resolve("smithy.json.lock");
        try {
            Files.createDirectories(java.util.Objects.requireNonNull(lockPath.getParent()));
            try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                    var ignored = channel.lock()) {
                action.run();
            }
        } catch (IOException e) {
            throw new CliError("Unable to lock " + configPath() + ": " + e.getMessage());
        }
    }
}
