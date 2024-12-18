/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.SetUtils;

/**
 * A container for all known dependencies of a generator.
 *
 * <p>A DependencyTracker can include predefined dependencies loaded from a
 * file (for example to track versions of runtime dependencies used in the
 * generator), or dependencies that are accumulated dynamically as code is
 * generated.
 *
 * <p>Notes:
 * <ul>
 *     <li>Multiple packages of the same name and type can be added to tracker.
 *     There's no de-duplication.</li>
 *     <li>Note that this class is mutable and not synchronized.</li>
 * </ul>
 *
 * <h2>Loading from JSON</h2>
 *
 * <p>Dependencies can be loaded from a JSON file to more easily track
 * dependencies used at runtime by generated code. This feature can also
 * be used to generate the dependencies tracked by the generated from from
 * other dependency graph formats like lockfiles.
 *
 * <p>The JSON file has the following format:
 *
 * <pre>
 * {@code
 * {
 *     "version": "1.0",
 *     "dependencies": [
 *         {
 *             "packageName": "string",
 *             "version": "string",
 *             "dependencyType": "string",
 *             "properties": {
 *                 "x": true,
 *                 "y": [10],
 *                 "z": "string"
 *             }
 *         }
 *     ]
 * }
 * }
 * </pre>
 *
 * <ul>
 *     <li>"version" (string, required): Must be set to "1.0".</li>
 *     <li>"dependencies" is a list of dependency objects that contain the following
 *         properties:
 *         <ul>
 *             <li>"packageName" (string, required): The required name of the package.</li>
 *             <li>"version" (string, required): The required dependency version.</li>
 *             <li>"dependencyType" (string): The optional type of dependency. This value
 *             is dependent on the package manager of the target environment.</li>
 *             <li>"properties" (map of string to any value): Properties to assign to
 *             the symbol. These properties can be any JSON value type other than null.
 *             List values are converted to a {@link List}, map values are converted to
 *             a {@link Map}, boolean values to Java's boolean, numeric values to an
 *             appropriate {@link Number} type, and string values to {@link String}.</li>
 *         </ul>
 *     </li>
 * </ul>
 */
public final class DependencyTracker implements SymbolDependencyContainer {

    private static final String VERSION = "version";
    private static final String DEPENDENCIES = "dependencies";
    private static final String PACKAGE_NAME = "packageName";
    private static final String DEPENDENCY_TYPE = "dependencyType";
    private static final String PROPERTIES = "properties";
    private static final Set<String> TOP_LEVEL_PROPERTIES = SetUtils.of(VERSION, DEPENDENCIES);
    private static final Set<String> ALLOWED_SYMBOL_PROPERTIES = SetUtils.of(
            PACKAGE_NAME,
            DEPENDENCY_TYPE,
            VERSION,
            PROPERTIES);

    private final List<SymbolDependency> dependencies = new ArrayList<>();

    @Override
    public List<SymbolDependency> getDependencies() {
        return dependencies;
    }

    /**
     * Gets the first found dependency by name.
     *
     * @param name Package name of the dependency to get.
     * @return Returns the dependency.
     * @throws IllegalArgumentException if the dependency cannot be found.
     */
    public SymbolDependency getByName(String name) {
        for (SymbolDependency dependency : dependencies) {
            if (dependency.getPackageName().equals(name)) {
                return dependency;
            }
        }
        throw new IllegalArgumentException("Unknown dependency '" + name + "'. Known dependencies: " + dependencies);
    }

    /**
     * Gets the first found dependency by name and dependency type.
     *
     * @param name Package name of the dependency to get.
     * @param dependencyType The dependency type of package to find.
     * @return Returns the dependency.
     * @throws IllegalArgumentException if the dependency cannot be found.
     */
    public SymbolDependency getByName(String name, String dependencyType) {
        for (SymbolDependency dependency : dependencies) {
            if (dependency.getPackageName().equals(name) && dependency.getDependencyType().equals(dependencyType)) {
                return dependency;
            }
        }
        throw new IllegalArgumentException("Unknown dependency '" + name + "' of type '" + dependencyType + "'. "
                + "Known dependencies: " + dependencies);
    }

    /**
     * Gets a list of matching dependencies that have a dependency type
     * matching {@code dependencyType}.
     *
     * @param dependencyType Dependency type to find.
     * @return Returns the matching dependencies.
     */
    public List<SymbolDependency> getByType(String dependencyType) {
        List<SymbolDependency> result = new ArrayList<>();
        for (SymbolDependency dependency : dependencies) {
            if (dependency.getDependencyType().equals(dependencyType)) {
                result.add(dependency);
            }
        }
        return result;
    }

    /**
     * Gets a list of matching dependencies that contain a property named
     * {@code property}.
     *
     * @param property Property to find.
     * @return Returns the matching dependencies.
     */
    public List<SymbolDependency> getByProperty(String property) {
        List<SymbolDependency> result = new ArrayList<>();
        for (SymbolDependency dependency : dependencies) {
            if (dependency.getProperty(property).isPresent()) {
                result.add(dependency);
            }
        }
        return result;
    }

    /**
     * Gets a list of matching dependencies that contain a property named
     * {@code property} with a value of {@code value}.
     *
     * @param property Property to find.
     * @param value Value to match.
     * @return Returns the matching dependencies.
     */
    public List<SymbolDependency> getByProperty(String property, Object value) {
        List<SymbolDependency> result = new ArrayList<>();
        for (SymbolDependency dependency : dependencies) {
            if (dependency.getProperty(property).filter(p -> p.equals(value)).isPresent()) {
                result.add(dependency);
            }
        }
        return result;
    }

    /**
     * Adds a dependency.
     *
     * @param dependency Dependency to add.
     */
    public void addDependency(SymbolDependency dependency) {
        dependencies.add(dependency);
    }

    /**
     * Adds a dependency.
     *
     * @param packageName Name of the dependency.
     * @param version Version of the dependency.
     * @param dependencyType Type of dependency (e.g., "dev", "test", "runtime", etc).
     *                       This value wholly depends on the type of dependency graph
     *                       being generated.
     */
    public void addDependency(String packageName, String version, String dependencyType) {
        SymbolDependency dependency = SymbolDependency.builder()
                .packageName(packageName)
                .version(version)
                .dependencyType(dependencyType)
                .build();
        addDependency(dependency);
    }

    /**
     * Adds dependencies from a {@link SymbolDependencyContainer}.
     *
     * @param container Container to copy depdencies from.
     */
    public void addDependencies(SymbolDependencyContainer container) {
        for (SymbolDependency dependency : container.getDependencies()) {
            addDependency(dependency);
        }
    }

    /**
     * Loads predefined dependencies from a JSON file (for example, to track
     * known dependencies used by generated code at runtime).
     *
     * <pre>
     * {@code
     * DependencyTracker tracker = new DependencyTracker();
     * tracker.addDependenciesFromJson(getClass().getResource("some-file.json"));
     * }
     * </pre>
     *
     * @param jsonFile URL location of the JSON file.
     */
    public void addDependenciesFromJson(URL jsonFile) {
        Objects.requireNonNull(jsonFile, "Dependency JSON file is null, probably because the file could not be found.");
        try (InputStream stream = jsonFile.openConnection().getInputStream()) {
            parseDependenciesFromJson(Node.parse(stream));
        } catch (IOException e) {
            throw new UncheckedIOException("Error loading dependencies from "
                    + jsonFile + ": " + e.getMessage(), e);
        }
    }

    private void parseDependenciesFromJson(Node node) {
        NodeMapper mapper = new NodeMapper();
        ObjectNode root = node.expectObjectNode();
        root.warnIfAdditionalProperties(TOP_LEVEL_PROPERTIES);
        // Must define a version.
        root.expectStringMember(VERSION).expectOneOf("1.0");
        // Must define a list of dependencies, each an ObjectNode.
        for (ObjectNode value : root.expectArrayMember(DEPENDENCIES).getElementsAs(ObjectNode.class)) {
            value.warnIfAdditionalProperties(ALLOWED_SYMBOL_PROPERTIES);
            SymbolDependency.Builder builder = SymbolDependency.builder();
            builder.packageName(value.expectStringMember(PACKAGE_NAME).getValue());
            builder.version(value.expectStringMember(VERSION).getValue());
            value.getStringMember(DEPENDENCY_TYPE).ifPresent(v -> builder.dependencyType(v.getValue()));
            value.getObjectMember(PROPERTIES).ifPresent(properties -> {
                for (Map.Entry<String, Node> entry : properties.getStringMap().entrySet()) {
                    Object nodeAsJavaValue = mapper.deserialize(entry.getValue(), Object.class);
                    builder.putProperty(entry.getKey(), nodeAsJavaValue);
                }
            });
            addDependency(builder.build());
        }
    }
}
