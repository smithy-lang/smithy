/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.codegen.core.ImportContainer;
import software.amazon.smithy.codegen.core.ShapeGenerationOrder;
import software.amazon.smithy.codegen.core.SmithyIntegration;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.SymbolWriter;
import software.amazon.smithy.codegen.core.TopologicalIndex;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ShapeClosureIndex;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.metadata.ShapeClosure;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.validators.ShapeClosureValidator;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Performs directed code generation of a {@link DirectedCodegen}.
 *
 * @param <W> Type of {@link SymbolWriter} used to generate code.
 * @param <I> Type of {@link SmithyIntegration} to apply.
 * @param <C> Type of {@link CodegenContext} to create and use.
 * @param <S> Type of settings object to pass to directed methods.
 */
public final class CodegenDirector<
        W extends SymbolWriter<W, ? extends ImportContainer>,
        I extends SmithyIntegration<S, W, C>,
        C extends CodegenContext<S, W, I>,
        S> {

    private static final Logger LOGGER = Logger.getLogger(CodegenDirector.class.getName());

    private Class<I> integrationClass;
    private ShapeId service;
    private String shapeClosure;
    private ShapeClosure shapeClosureDefinition;
    private Model model;
    private S settings;
    private ObjectNode integrationSettings = Node.objectNode();
    private FileManifest fileManifest;
    private FileManifest sharedFileManifest;
    private Supplier<Iterable<I>> integrationFinder;
    private DirectedCodegen<C, S, I> directedCodegen;
    private final List<BiFunction<Model, ModelTransformer, Model>> transforms = new ArrayList<>();
    private ShapeGenerationOrder shapeGenerationOrder = ShapeGenerationOrder.TOPOLOGICAL;
    private boolean generateDataShapesOnly;
    private boolean requireCaseInsensitiveNames;

    /**
     * Simplifies a Smithy model for code generation of a single service.
     *
     * <ul>
     *     <li>Flattens error hierarchies onto every operation.</li>
     *     <li>Flattens mixins</li>
     * </ul>
     *
     * <p><em>Note</em>: This transform is applied automatically by a code
     * generator if {@link CodegenDirector#performDefaultCodegenTransforms()} is
     * set to true.
     *
     * @param model Model being code generated.
     * @param service Service being generated.
     * @param transformer Model transformer to use.
     * @return Returns the updated model.
     */
    public static Model simplifyModelForServiceCodegen(Model model, ShapeId service, ModelTransformer transformer) {
        ServiceShape serviceShape = model.expectShape(service, ServiceShape.class);
        model = transformer.copyServiceErrorsToOperations(model, serviceShape);
        model = transformer.flattenAndRemoveMixins(model);
        return model;
    }

    /**
     * Sets the required class used for SmithyIntegrations.
     *
     * @param integrationClass SmithyIntegration class.
     */
    public void integrationClass(Class<I> integrationClass) {
        this.integrationClass = integrationClass;
    }

    /**
     * Sets the service being generated.
     *
     * <p>At least one of {@link #service} or {@link #shapeClosure} must be set. Setting
     * only a service generates the closure of shapes connected to that service. Setting
     * both a service and a shape closure enables "combined mode", where the shape closure is the
     * generated set, and the service is the designated primary service (it must be a member
     * of that closure).
     *
     * @param service Service to generate.
     */
    public void service(ShapeId service) {
        this.service = service;
    }

    /**
     * Sets the shape closure being generated by its metadata-defined id.
     *
     * <p>At least one of {@link #service} or {@link #shapeClosure} must be set. Setting
     * a shape closure generates the shapes in that closure without requiring a service
     * shape to anchor them. A service may also be set to enable combined mode (see
     * {@link #service(ShapeId)}).
     *
     * @param shapeClosure ID of the shape closure to generate.
     */
    public void shapeClosure(String shapeClosure) {
        this.shapeClosure = shapeClosure;
        this.shapeClosureDefinition = null;
    }

    /**
     * Sets the shape closure being generated from a {@link ShapeClosure} object.
     *
     * <p>This lets a generator hand the director a closure it computed in code rather than
     * one authored as model metadata. Before resolution, the closure is injected into the
     * model's {@code shapeClosures} metadata and then resolved through the same
     * {@link ShapeClosureIndex} path as a metadata-authored closure, keyed by the object's
     * id. The id must not collide with a closure already declared in the model.
     *
     * @param shapeClosure Shape closure to inject and generate.
     */
    public void shapeClosure(ShapeClosure shapeClosure) {
        this.shapeClosureDefinition = shapeClosure;
        this.shapeClosure = shapeClosure.getId();
    }

    /**
     * Generates only data shapes, never invoking the {@code generateService},
     * {@code generateResource}, or {@code generateOperation} methods of the
     * {@link DirectedCodegen}.
     *
     * <p>This enables "type codegen", where only the shapes that carry data are
     * generated. Service, resource, and operation shapes are removed from the set of
     * generated shapes even when they are present in the generated closure (for
     * example, when generating the data shapes of a service's closure). The data
     * shapes reachable through removed operations (their inputs, outputs, and errors)
     * are unaffected and are still generated.
     */
    public void generateDataShapesOnly() {
        this.generateDataShapesOnly = true;
    }

    /**
     * Requires that the names of the shapes being generated are case-insensitively
     * unique, failing code generation if they are not.
     *
     * <p>Unlike service closures, shape closures do not require their shape names to be
     * case-insensitively unique. Code generators that cannot represent such conflicts
     * can call this to enforce uniqueness. The check is performed on the actual shapes
     * being generated after model transforms are applied and with closure renames
     * applied, and it fails regardless of whether the related validation event was
     * suppressed.
     *
     * <p>Applying this to service-based generation is a no-op.
     */
    public void requireCaseInsensitiveNames() {
        this.requireCaseInsensitiveNames = true;
    }

    /**
     * Sets the required {@link DirectedCodegen} implementation to invoke.
     *
     * @param directedCodegen Directed code generator to run.
     */
    public void directedCodegen(DirectedCodegen<C, S, I> directedCodegen) {
        this.directedCodegen = directedCodegen;
    }

    /**
     * Sets the required model to generate from.
     *
     * @param model Model to generate from.
     */
    public void model(Model model) {
        this.model = model;
    }

    /**
     * Sets the required settings object used for code generation.
     *
     * <p>{@link #integrationSettings} MUST also be set.
     *
     * @param settings Settings object.
     */
    public void settings(S settings) {
        this.settings = settings;
    }

    /**
     * Sets the required settings object used for code generation using
     * a {@link Node}.
     *
     * <p>A Node value is used by Smithy-Build plugins to configure settings.
     * This method is a helper method that uses Smithy's fairly simple
     * object-mapper to deserialize a node into the desired settings type.
     * You will need to manually deserialize your settings if using types that
     * are not supported by Smithy's {@link NodeMapper}.
     *
     * <p>This will also set {@link #integrationSettings} if the {@code integrations}
     * key is present.
     *
     * @param settingsType Settings type to deserialize into.
     * @param settingsNode Settings node value to deserialize.
     * @return Returns the deserialized settings as this is needed to provide a service shape ID.
     */
    public S settings(Class<S> settingsType, Node settingsNode) {
        LOGGER.fine(() -> "Loading codegen settings from node value: " + settingsNode.getSourceLocation());
        S deserialized = new NodeMapper().deserialize(settingsNode, settingsType);
        settings(deserialized);
        settingsNode.asObjectNode()
                .flatMap(node -> node.getObjectMember("integrations"))
                .ifPresent(this::integrationSettings);
        return deserialized;
    }

    /**
     * Sets the settings node to be passed to integrations.
     *
     * <p>Generators MUST set this with the {@code integrations} key from their
     * plugin settings.
     *
     * <pre>{@code
     * {
     *     "version": "1.0",
     *     "projections": {
     *         "codegen-projection": {
     *             "plugins": {
     *                 "code-generator": {
     *                     "service": "com.example#DocumentedService",
     *                     "integrations": {
     *                         "my-integration": {
     *                             "example-setting": "foo"
     *                         }
     *                     }
     *                 }
     *             }
     *         }
     *     }
     * }
     * }</pre>
     *
     * <p>In this example, the value of the {@code integrations} key is what must
     * be passed to this method. The value of the {@code my-integration} key will
     * then be provided to an integration with the name {@code my-integration}.
     *
     * @param integrationSettings Settings used to configure integrations.
     */
    public void integrationSettings(ObjectNode integrationSettings) {
        this.integrationSettings = Objects.requireNonNull(integrationSettings);
    }

    /**
     * Sets the required file manifest used to write files to disk.
     *
     * @param fileManifest File manifest to write files.
     */
    public void fileManifest(FileManifest fileManifest) {
        this.fileManifest = fileManifest;
    }

    /**
     * Sets the FileManifest used to create files in the projection's shared file
     * space.
     *
     * <p>All files written by a generator should either be written using this
     * manifest or the generator's isolated manifest ({@link #fileManifest}).
     *
     * <p>Files written to this manifest may be read or modified by other Smithy build
     * plugins. Generators SHOULD NOT write files to this manifest unless they
     * specifically intend for them to be consumed by other plugins. Files that are not
     * intended to be shared should be written to the manifest from
     * {@link #fileManifest}.
     *
     * @param sharedFileManifest FileManifest to use for shared files.
     */
    public void sharedFileManifest(FileManifest sharedFileManifest) {
        this.sharedFileManifest = sharedFileManifest;
    }

    /**
     * Sets a custom implementation for finding {@link SmithyIntegration}s.
     *
     * <p>Most implementations can use {@link #integrationClassLoader(ClassLoader)}.
     *
     * @param integrationFinder Smithy integration finder.
     */
    public void integrationFinder(Supplier<Iterable<I>> integrationFinder) {
        this.integrationFinder = integrationFinder;
    }

    /**
     * Sets a custom class loader for finding implementations of {@link SmithyIntegration}.
     *
     * @param classLoader Class loader to find integrations.* @return Returns self.
     */
    public void integrationClassLoader(ClassLoader classLoader) {
        Objects.requireNonNull(integrationClass,
                "integrationClass() must be called before calling integrationClassLoader");
        integrationFinder(() -> ServiceLoader.load(integrationClass, classLoader));
    }

    /**
     * Set to true to apply {@link CodegenDirector#simplifyModelForServiceCodegen}
     * prior to code generation.
     */
    public void performDefaultCodegenTransforms() {
        transforms.add((model, transformer) -> {
            LOGGER.finest("Performing default codegen model transforms for directed codegen");
            // Service codegen simplifies the model for its service, but error flattening
            // is service-specific. Closure-based generation has no service, so it only
            // flattens mixins.
            if (service != null) {
                return simplifyModelForServiceCodegen(model, service, transformer);
            }
            return transformer.flattenAndRemoveMixins(model);
        });
    }

    /**
     * Generates dedicated input and output shapes for every operation if the operation
     * doesn't already have them.
     *
     * <p>This method uses "Input" as the default suffix for input, and "Output" as the
     * default suffix for output shapes. Use {@link #createDedicatedInputsAndOutputs(String, String)}
     * to use custom suffixes.
     *
     * @see ModelTransformer#createDedicatedInputAndOutput(Model, String, String)
     */
    public void createDedicatedInputsAndOutputs() {
        createDedicatedInputsAndOutputs("Input", "Output");
    }

    /**
     * Generates dedicated input and output shapes for every operation if the operation
     * doesn't already have them.
     *
     * @param inputSuffix  Suffix to use for input shapes (e.g., "Input").
     * @param outputSuffix Suffix to use for output shapes (e.g., "Output").
     * @see ModelTransformer#createDedicatedInputAndOutput(Model, String, String)
     */
    public void createDedicatedInputsAndOutputs(String inputSuffix, String outputSuffix) {
        transforms.add((model, transformer) -> {
            LOGGER.finest("Creating dedicated input and output shapes for directed codegen");
            return transformer.createDedicatedInputAndOutput(model, inputSuffix, outputSuffix);
        });
    }

    /**
     * Removes any shapes deprecated before the specified date.
     *
     * @param relativeDate Relative date, in YYYY-MM-DD format, to use to filter out deprecated shapes.
     * @see ModelTransformer#filterDeprecatedRelativeDate(Model, String)
     */
    public void removeShapesDeprecatedBeforeDate(String relativeDate) {
        transforms.add((model, transformer) -> {
            LOGGER.finest("Removing shapes deprecated before date: " + relativeDate);
            return transformer.filterDeprecatedRelativeDate(model, relativeDate);
        });
    }

    /**
     * Removes any shapes deprecated before the specified version.
     *
     * @param relativeVersion Version, in SemVer format, to use to filter out deprecated shapes.
     * @see ModelTransformer#filterDeprecatedRelativeVersion(Model, String)
     */
    public void removeShapesDeprecatedBeforeVersion(String relativeVersion) {
        transforms.add((model, transformer) -> {
            LOGGER.finest("Removing shapes deprecated before version: " + relativeVersion);
            return transformer.filterDeprecatedRelativeVersion(model, relativeVersion);
        });
    }

    /**
     * Makes {@code idempotencyToken} fields {@code clientOptional}.
     *
     * @see ModelTransformer#makeIdempotencyTokensClientOptional(Model)
     */
    public void makeIdempotencyTokensClientOptional() {
        transforms.add((model, transformer) -> {
            LOGGER.finest("Making `@idempotencyToken` fields `@clientOptional`");
            return transformer.makeIdempotencyTokensClientOptional(model);
        });
    }

    /**
     * Changes each compatible string shape with the enum trait to an enum shape.
     *
     * @param synthesizeEnumNames Whether enums without names should have names synthesized if possible.
     * @see ModelTransformer#changeStringEnumsToEnumShapes(Model, boolean)
     */
    public void changeStringEnumsToEnumShapes(boolean synthesizeEnumNames) {
        transforms.add((model, transformer) -> {
            LOGGER.finest("Creating dedicated input and output shapes for directed codegen");
            return transformer.changeStringEnumsToEnumShapes(model, synthesizeEnumNames);
        });
    }

    /**
     * Flattens service-level pagination information into operation pagination traits.
     *
     * @see ModelTransformer#flattenPaginationInfoIntoOperations(Model, ServiceShape)
     */
    public void flattenPaginationInfoIntoOperations() {
        transforms.add((model, transformer) -> {
            LOGGER.finest("Flattening pagination info into operation traits for directed codegen");
            // When a closure drives generation (closure or combined mode), pagination is
            // flattened for every service in the closure being generated. A pure service walk
            // flattens for that single service.
            if (shapeClosure == null) {
                return transformer.flattenPaginationInfoIntoOperations(model,
                        model.expectShape(service, ServiceShape.class));
            }
            Set<Shape> closureShapes = ShapeClosureIndex.of(model).getShapesInClosure(shapeClosure);
            for (ServiceShape serviceShape : model.getServiceShapes()) {
                if (closureShapes.contains(serviceShape)) {
                    model = transformer.flattenPaginationInfoIntoOperations(model, serviceShape);
                }
            }
            return model;
        });
    }

    /**
     * Sets the shapes order for code generation.
     *
     * <p>CodegenDirector order the shapes appropriately before passing them to the code generators.
     * The default order is topological, and can be overridden with this method
     *
     * @param order the order to use for the shape generation process.
     */
    public void shapeGenerationOrder(ShapeGenerationOrder order) {
        this.shapeGenerationOrder = order;
    }

    /**
     * Sorts all members of the model prior to codegen.
     *
     * <p>This should only be used by languages where changing the order of members
     * in a structure or union is a backward compatible change (i.e., not C, C++, Rust, etc).
     * Once this is performed, there's no need to ever explicitly sort members
     * throughout the rest of code generation.
     */
    public void sortMembers() {
        transforms.add((model, transformer) -> {
            LOGGER.finest("Sorting model members for directed codegen");
            return transformer.sortMembers(model, Shape::compareTo);
        });
    }

    /**
     * Finalizes the Runner and performs directed code generation.
     *
     * @throws IllegalStateException if a required value has not been provided.
     */
    public void run() {
        injectShapeClosureDefinition();
        validateState();
        performModelTransforms();

        List<I> integrations = findIntegrations();
        preprocessModelWithIntegrations(integrations);

        ServiceShape serviceShape = service == null ? null : model.expectShape(service, ServiceShape.class);

        SymbolProvider provider = createSymbolProvider(integrations, serviceShape);

        C context = createContext(serviceShape, provider, integrations);

        // After the context is created, it holds the model that should be used for rest of codegen. So `model` should
        // not really be used directly after this point in the flow. Setting the `model` to `context.model()` to avoid
        // issues even if `model` gets used unknowingly.
        model = context.model();

        registerInterceptors(context, integrations);

        LOGGER.fine("All setup done. Beginning code generation");

        LOGGER.finest(() -> "Performing custom codegen for "
                + directedCodegen.getClass().getName() + " before shape codegen");
        CustomizeDirective<C, S> customizeDirective =
                new CustomizeDirective<>(context, serviceShape, shapeClosure, generateDataShapesOnly);
        directedCodegen.customizeBeforeShapeGeneration(customizeDirective);

        if (shapeClosure != null) {
            LOGGER.finest("Generating shapes for closure: " + shapeClosure);
        } else {
            LOGGER.finest("Generating shapes for service: " + serviceShape);
        }
        generateShapes(context, serviceShape);

        // The service is only generated when driven by a service and not performing
        // data-shape-only ("type") code generation. Here the driving service is also the
        // service shape being generated.
        if (serviceShape != null && !generateDataShapesOnly) {
            LOGGER.finest(() -> "Generating service " + serviceShape.getId());
            directedCodegen.generateService(
                    new GenerateServiceDirective<>(context,
                            serviceShape,
                            shapeClosure,
                            generateDataShapesOnly,
                            serviceShape));
        }

        LOGGER.finest(() -> "Performing custom codegen for "
                + directedCodegen.getClass().getName() + " before integrations");
        directedCodegen.customizeBeforeIntegrations(customizeDirective);

        applyIntegrationCustomizations(context, integrations);

        LOGGER.finest(() -> "Performing custom codegen for "
                + directedCodegen.getClass().getName() + " after integrations");
        directedCodegen.customizeAfterIntegrations(customizeDirective);

        LOGGER.finest(() -> "Directed codegen finished for " + directedCodegen.getClass().getName());

        if (!context.writerDelegator().getWriters().isEmpty()) {
            LOGGER.info(() -> "Flushing remaining writers of " + directedCodegen.getClass().getName());
            context.writerDelegator().flushWriters();
        }
    }

    /**
     * Injects a {@link ShapeClosure} passed via {@link #shapeClosure(ShapeClosure)} into the
     * model's {@code shapeClosures} metadata so it resolves through the normal index path.
     */
    private void injectShapeClosureDefinition() {
        if (shapeClosureDefinition == null) {
            return;
        }

        SmithyBuilder.requiredState("model", model);

        Map<String, ShapeClosure> existing = ShapeClosure.fromModel(model);
        if (existing.containsKey(shapeClosureDefinition.getId())) {
            throw new IllegalStateException(
                    "The shape closure `" + shapeClosureDefinition.getId() + "` cannot be injected because a "
                            + "closure with the same id is already declared in the model.");
        }

        List<Node> closureNodes = new ArrayList<>();
        Node closuresNode = model.getMetadata().get(ShapeClosure.METADATA_KEY);
        if (closuresNode != null && closuresNode.isArrayNode()) {
            closureNodes.addAll(closuresNode.expectArrayNode().getElements());
        }
        closureNodes.add(shapeClosureDefinition.toNode());

        model = model.toBuilder()
                .putMetadataProperty(ShapeClosure.METADATA_KEY, Node.fromNodes(closureNodes))
                .build();
    }

    private void validateState() {
        SmithyBuilder.requiredState("integrationClass", integrationClass);
        SmithyBuilder.requiredState("model", model);
        SmithyBuilder.requiredState("settings", settings);
        SmithyBuilder.requiredState("fileManifest", fileManifest);
        SmithyBuilder.requiredState("directedCodegen", directedCodegen);
        SmithyBuilder.requiredState("shapeGenerationOrder", shapeGenerationOrder);

        // Code generation requires at least one source. A service alone walks the service,
        // a shape closure alone generates the closure. Setting both is "combined mode", where
        // the closure is the generated set and the service is the designated primary service.
        if (service == null && shapeClosure == null) {
            throw new IllegalStateException(
                    "At least one of `service` and `shapeClosure` must be set, but neither was provided.");
        }

        // Fail fast on an unknown shape closure rather than deep inside code generation.
        if (shapeClosure != null) {
            Set<String> closureIds = ShapeClosureIndex.of(model).getClosureIds();
            if (!closureIds.contains(shapeClosure)) {
                throw new IllegalStateException(
                        "The shape closure `" + shapeClosure + "` is not defined in the model. Defined "
                                + "closures: " + closureIds);
            }

            // In combined mode the primary service must be part of the closure it drives, so
            // that the closure-generated set actually contains the service being generated.
            if (service != null) {
                Set<Shape> closureShapes = ShapeClosureIndex.of(model).getShapesInClosure(shapeClosure);
                Shape serviceShape = model.getShape(service).orElse(null);
                if (serviceShape == null || !closureShapes.contains(serviceShape)) {
                    throw new IllegalStateException(
                            "The primary service `" + service + "` must be a member of the shape closure `"
                                    + shapeClosure + "` when both are set, but it is not part of that closure.");
                }
            }
        }

        // Use a default integration finder implementation.
        if (integrationFinder == null) {
            LOGGER.fine(() -> String.format("Finding %s integrations using the %s class loader",
                    integrationClass.getName(),
                    CodegenDirector.class.getCanonicalName()));
            integrationClassLoader(getClass().getClassLoader());
        }
    }

    private void performModelTransforms() {
        LOGGER.fine(() -> "Performing model transformations for " + directedCodegen.getClass().getName());
        ModelTransformer transformer = ModelTransformer.create();
        for (BiFunction<Model, ModelTransformer, Model> transform : transforms) {
            model = transform.apply(model, transformer);
        }

        if (requireCaseInsensitiveNames) {
            enforceCaseInsensitiveNames();
        }
    }

    /**
     * Re-runs {@link ShapeClosureValidator} against the transformed model and fails code
     * generation if the shape closure being generated has case-insensitively conflicting
     * shape names.
     *
     * <p>This does nothing for service-driven code generation, since service closures
     * already enforce case-insensitive name uniqueness.
     */
    private void enforceCaseInsensitiveNames() {
        if (shapeClosure == null) {
            return;
        }

        String conflictEventId = "ShapeClosure.NameConflicts." + shapeClosure;
        List<ValidationEvent> conflicts = new ShapeClosureValidator().validate(model)
                .stream()
                .filter(event -> event.getId().equals(conflictEventId))
                .collect(Collectors.toList());

        if (!conflicts.isEmpty()) {
            throw new CodegenException(conflicts.stream()
                    .map(ValidationEvent::getMessage)
                    .collect(Collectors.joining("\n")));
        }
    }

    private List<I> findIntegrations() {
        LOGGER.fine(() -> "Finding integration implementations of " + integrationClass.getName());
        List<I> integrations = SmithyIntegration.sort(integrationFinder.get());
        integrations.forEach(i -> {
            LOGGER.finest(() -> "Found integration " + i.getClass().getCanonicalName());
            i.configure(settings, integrationSettings.getObjectMember(i.name()).orElse(Node.objectNode()));
        });
        return integrations;
    }

    private void preprocessModelWithIntegrations(List<I> integrations) {
        LOGGER.fine(() -> "Preprocessing codegen model using " + integrationClass.getName());
        for (I integration : integrations) {
            model = integration.preprocessModel(model, settings);
        }
        LOGGER.finer(() -> "Preprocessing codegen model using " + integrationClass.getName() + " complete");
    }

    private SymbolProvider createSymbolProvider(List<I> integrations, ServiceShape serviceShape) {
        LOGGER.fine(() -> "Creating a symbol provider from " + settings.getClass().getName());
        SymbolProvider provider = directedCodegen.createSymbolProvider(
                new CreateSymbolProviderDirective<>(model,
                        settings,
                        serviceShape,
                        shapeClosure,
                        generateDataShapesOnly));

        LOGGER.finer(() -> "Decorating symbol provider using " + integrationClass.getName());
        for (I integration : integrations) {
            provider = integration.decorateSymbolProvider(model, settings, provider);
        }

        return SymbolProvider.cache(provider);
    }

    private C createContext(ServiceShape serviceShape, SymbolProvider provider, List<I> integrations) {
        LOGGER.fine(() -> "Creating a codegen context for " + directedCodegen.getClass().getName());
        return directedCodegen.createContext(new CreateContextDirective<>(
                model,
                settings,
                serviceShape,
                shapeClosure,
                generateDataShapesOnly,
                provider,
                fileManifest,
                sharedFileManifest,
                integrations));
    }

    private void registerInterceptors(C context, List<I> integrations) {
        LOGGER.fine(() -> "Registering CodeInterceptors from integrations of " + integrationClass.getName());
        List<CodeInterceptor<? extends CodeSection, W>> interceptors = new ArrayList<>();
        for (I integration : integrations) {
            interceptors.addAll(integration.interceptors(context));
        }
        context.writerDelegator().setInterceptors(interceptors);
    }

    private void generateShapes(C context, ServiceShape serviceShape) {
        LOGGER.fine(() -> String.format("Generating shapes for %s in %s order",
                directedCodegen.getClass().getName(),
                this.shapeGenerationOrder.name()));
        Set<Shape> shapes = resolveShapesToGenerate(context.model(), serviceShape);
        ShapeGenerator<W, C, S> generator =
                new ShapeGenerator<>(context, serviceShape, shapeClosure, generateDataShapesOnly, directedCodegen);
        List<Shape> orderedShapes = new ArrayList<>();

        switch (this.shapeGenerationOrder) {
            case ALPHABETICAL:
                orderedShapes.addAll(shapes);
                Function<Shape, String> nameForSort = contextualNamer(context.model(), serviceShape);
                orderedShapes.sort(Comparator.comparing(nameForSort));
                break;
            case NONE:
                orderedShapes.addAll(shapes);
                break;
            case TOPOLOGICAL:
            default:
                TopologicalIndex topologicalIndex = TopologicalIndex.of(context.model());
                for (Shape shape : topologicalIndex.getOrderedShapes()) {
                    if (shapes.contains(shape)) {
                        orderedShapes.add(shape);
                    }
                }
                for (Shape shape : topologicalIndex.getRecursiveShapes()) {
                    if (shapes.contains(shape)) {
                        orderedShapes.add(shape);
                    }
                }
        }

        for (Shape shape : orderedShapes) {
            if (shapes.contains(shape)) {
                shape.accept(generator);
            }
        }
        LOGGER.finest(() -> "Finished generating shapes for " + directedCodegen.getClass().getName());
    }

    /**
     * Resolves the set of shapes to generate from the configured source.
     *
     * <p>When generating only data shapes, service, resource, and operation
     * shapes are removed so they are never dispatched to their respective
     * generation methods.
     */
    private Set<Shape> resolveShapesToGenerate(Model model, ServiceShape serviceShape) {
        Set<Shape> shapes;
        // The generated set is driven by the shape closure whenever one is present, even in
        // combined mode where a primary service is also set.
        if (shapeClosure != null) {
            shapes = new LinkedHashSet<>(ShapeClosureIndex.of(model).getShapesInClosure(shapeClosure));
            shapes.removeIf(Prelude::isPreludeShape);
        } else {
            shapes = new Walker(model).walkShapes(serviceShape);
        }

        if (generateDataShapesOnly) {
            shapes.removeIf(shape -> shape.getType().getCategory() == ShapeType.Category.SERVICE);
        }

        return shapes;
    }

    private Function<Shape, String> contextualNamer(Model model, ServiceShape serviceShape) {
        if (shapeClosure != null) {
            Map<ShapeId, String> renames = ShapeClosureIndex.of(model).getRenames(shapeClosure);
            return shape -> renames.getOrDefault(shape.getId(), shape.getId().getName());
        }
        return shape -> shape.getId().getName(serviceShape);
    }

    private void applyIntegrationCustomizations(C context, List<I> integrations) {
        for (I integration : integrations) {
            LOGGER.finest(() -> "Customizing codegen for " + directedCodegen.getClass().getName()
                    + " using integration " + integration.getClass().getName());
            integration.customize(context);
        }
    }

    private static class ShapeGenerator<
            W extends SymbolWriter<W, ? extends ImportContainer>,
            C extends CodegenContext<S, W, ?>,
            S> extends ShapeVisitor.Default<Void> {

        private final C context;
        private final ServiceShape serviceShape;
        private final String shapeClosureId;
        private final boolean generateDataShapesOnly;
        private final DirectedCodegen<C, S, ?> directedCodegen;

        ShapeGenerator(
                C context,
                ServiceShape serviceShape,
                String shapeClosureId,
                boolean generateDataShapesOnly,
                DirectedCodegen<C, S, ?> directedCodegen
        ) {
            this.context = context;
            this.serviceShape = serviceShape;
            this.shapeClosureId = shapeClosureId;
            this.generateDataShapesOnly = generateDataShapesOnly;
            this.directedCodegen = directedCodegen;
        }

        @Override
        protected Void getDefault(Shape shape) {
            return null;
        }

        @Override
        public Void serviceShape(ServiceShape shape) {
            // The driving service is generated by a separate, ordered call after all other
            // shapes, so skip it here to avoid generating it twice. Any other service shape
            // (for example, one reached through a shape closure) is generated normally.
            if (shape.equals(serviceShape)) {
                return null;
            }
            LOGGER.finest(() -> "Generating service " + shape.getId());
            directedCodegen.generateService(
                    new GenerateServiceDirective<>(context,
                            serviceShape,
                            shapeClosureId,
                            generateDataShapesOnly,
                            shape));
            return null;
        }

        @Override
        public Void resourceShape(ResourceShape shape) {
            LOGGER.finest(() -> "Generating resource " + shape.getId());
            directedCodegen.generateResource(
                    new GenerateResourceDirective<>(context,
                            serviceShape,
                            shapeClosureId,
                            generateDataShapesOnly,
                            shape));
            return null;
        }

        @Override
        public Void operationShape(OperationShape shape) {
            LOGGER.finest(() -> "Generating operation " + shape.getId());
            directedCodegen.generateOperation(
                    new GenerateOperationDirective<>(context,
                            serviceShape,
                            shapeClosureId,
                            generateDataShapesOnly,
                            shape));
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            if (shape.hasTrait(ErrorTrait.ID)) {
                LOGGER.finest(() -> "Generating error " + shape.getId());
                directedCodegen.generateError(
                        new GenerateErrorDirective<>(context,
                                serviceShape,
                                shapeClosureId,
                                generateDataShapesOnly,
                                shape));
            } else {
                LOGGER.finest(() -> "Generating structure " + shape.getId());
                directedCodegen.generateStructure(
                        new GenerateStructureDirective<>(context,
                                serviceShape,
                                shapeClosureId,
                                generateDataShapesOnly,
                                shape));
            }
            return null;
        }

        @Override
        public Void unionShape(UnionShape shape) {
            LOGGER.finest(() -> "Generating union " + shape.getId());
            directedCodegen.generateUnion(
                    new GenerateUnionDirective<>(context,
                            serviceShape,
                            shapeClosureId,
                            generateDataShapesOnly,
                            shape));
            return null;
        }

        @Override
        public Void listShape(ListShape shape) {
            LOGGER.finest(() -> "Generating list " + shape.getId());
            directedCodegen.generateList(
                    new GenerateListDirective<>(context,
                            serviceShape,
                            shapeClosureId,
                            generateDataShapesOnly,
                            shape));
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            LOGGER.finest(() -> "Generating map " + shape.getId());
            directedCodegen.generateMap(
                    new GenerateMapDirective<>(context,
                            serviceShape,
                            shapeClosureId,
                            generateDataShapesOnly,
                            shape));
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            if (shape.hasTrait(EnumTrait.class)) {
                LOGGER.finest(() -> "Generating string enum " + shape.getId());
                directedCodegen.generateEnumShape(
                        new GenerateEnumDirective<>(context,
                                serviceShape,
                                shapeClosureId,
                                generateDataShapesOnly,
                                shape));
            }
            return null;
        }

        @Override
        public Void enumShape(EnumShape shape) {
            LOGGER.finest(() -> "Generating enum shape" + shape.getId());
            directedCodegen.generateEnumShape(
                    new GenerateEnumDirective<>(context,
                            serviceShape,
                            shapeClosureId,
                            generateDataShapesOnly,
                            shape));
            return null;
        }

        @Override
        public Void intEnumShape(IntEnumShape shape) {
            LOGGER.finest(() -> "Generating intEnum shape" + shape.getId());
            directedCodegen.generateIntEnumShape(
                    new GenerateIntEnumDirective<>(context,
                            serviceShape,
                            shapeClosureId,
                            generateDataShapesOnly,
                            shape));
            return null;
        }
    }
}
