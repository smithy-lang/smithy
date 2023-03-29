/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.codegen.core.directed;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.logging.Logger;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.codegen.core.ImportContainer;
import software.amazon.smithy.codegen.core.ShapeGenerationOrder;
import software.amazon.smithy.codegen.core.SmithyIntegration;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.SymbolWriter;
import software.amazon.smithy.codegen.core.TopologicalIndex;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
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
    private Model model;
    private S settings;
    private FileManifest fileManifest;
    private Supplier<Iterable<I>> integrationFinder;
    private DirectedCodegen<C, S, I> directedCodegen;
    private final List<BiFunction<Model, ModelTransformer, Model>> transforms = new ArrayList<>();
    private ShapeGenerationOrder shapeGenerationOrder = ShapeGenerationOrder.TOPOLOGICAL;

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
     * Sets the required service being generated.
     *
     * @param service Service to generate.
     */
    public void service(ShapeId service) {
        this.service = service;
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
     * @param settingsType Settings type to deserialize into.
     * @param settingsNode Settings node value to deserialize.
     * @return Returns the deserialized settings as this is needed to provide a service shape ID.
     */
    public S settings(Class<S> settingsType, Node settingsNode) {
        LOGGER.fine(() -> "Loading codegen settings from node value: " + settingsNode.getSourceLocation());
        S deserialized = new NodeMapper().deserialize(settingsNode, settingsType);
        settings(deserialized);
        return deserialized;
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
            return simplifyModelForServiceCodegen(model, Objects.requireNonNull(service), transformer);
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
        validateState();
        performModelTransforms();

        List<I> integrations = findIntegrations();
        preprocessModelWithIntegrations(integrations);

        ServiceShape serviceShape = model.expectShape(service, ServiceShape.class);

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
        CustomizeDirective<C, S> customizeDirective = new CustomizeDirective<>(context, serviceShape);
        directedCodegen.customizeBeforeShapeGeneration(customizeDirective);

        LOGGER.finest(() -> "Generating shapes for service " + serviceShape.getId());
        generateShapesInService(context, serviceShape);

        LOGGER.finest(() -> "Generating service " + serviceShape.getId());
        directedCodegen.generateService(new GenerateServiceDirective<>(context, serviceShape));

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

    private void validateState() {
        SmithyBuilder.requiredState("integrationClass", integrationClass);
        SmithyBuilder.requiredState("service", service);
        SmithyBuilder.requiredState("model", model);
        SmithyBuilder.requiredState("settings", settings);
        SmithyBuilder.requiredState("fileManifest", fileManifest);
        SmithyBuilder.requiredState("directedCodegen", directedCodegen);
        SmithyBuilder.requiredState("shapeGenerationOrder", shapeGenerationOrder);

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
    }

    private List<I> findIntegrations() {
        LOGGER.fine(() -> "Finding integration implementations of " + integrationClass.getName());
        List<I> integrations = SmithyIntegration.sort(integrationFinder.get());
        integrations.forEach(i -> LOGGER.finest(() -> "Found integration " + i.getClass().getCanonicalName()));
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
                new CreateSymbolProviderDirective<>(model, settings, serviceShape));

        LOGGER.finer(() -> "Decorating symbol provider using " + integrationClass.getName());
        for (I integration : integrations) {
            provider = integration.decorateSymbolProvider(model, settings, provider);
        }

        return SymbolProvider.cache(provider);
    }

    private C createContext(ServiceShape serviceShape, SymbolProvider provider, List<I> integrations) {
        LOGGER.fine(() -> "Creating a codegen context for " + directedCodegen.getClass().getName());
        return directedCodegen.createContext(new CreateContextDirective<>(
                model, settings, serviceShape, provider, fileManifest, integrations));
    }

    private void registerInterceptors(C context, List<I> integrations) {
        LOGGER.fine(() -> "Registering CodeInterceptors from integrations of " + integrationClass.getName());
        List<CodeInterceptor<? extends CodeSection, W>> interceptors = new ArrayList<>();
        for (I integration : integrations) {
            interceptors.addAll(integration.interceptors(context));
        }
        context.writerDelegator().setInterceptors(interceptors);
    }

    private void generateShapesInService(C context, ServiceShape serviceShape) {
        LOGGER.fine(() -> String.format("Generating shapes for %s in %s order",
                directedCodegen.getClass().getName(), this.shapeGenerationOrder.name()));
        Set<Shape> shapes = new Walker(context.model()).walkShapes(serviceShape);
        ShapeGenerator<W, C, S> generator = new ShapeGenerator<>(context, serviceShape, directedCodegen);
        List<Shape> orderedShapes = new ArrayList<>();

        switch (this.shapeGenerationOrder) {
            case ALPHABETICAL:
                orderedShapes.addAll(shapes);
                orderedShapes.sort(Comparator.comparing(s -> s.getId().getName(serviceShape)));
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
        private final DirectedCodegen<C, S, ?> directedCodegen;

        ShapeGenerator(C context, ServiceShape serviceShape, DirectedCodegen<C, S, ?> directedCodegen) {
            this.context = context;
            this.serviceShape = serviceShape;
            this.directedCodegen = directedCodegen;
        }

        @Override
        protected Void getDefault(Shape shape) {
            return null;
        }

        @Override
        public Void resourceShape(ResourceShape shape) {
            LOGGER.finest(() -> "Generating resource " + shape.getId());
            directedCodegen.generateResource(
                    new GenerateResourceDirective<>(context, serviceShape, shape));
            return null;
        }

        @Override
        public Void operationShape(OperationShape shape) {
            LOGGER.finest(() -> "Generating operation " + shape.getId());
            directedCodegen.generateOperation(
                new GenerateOperationDirective<>(context, serviceShape, shape));
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            if (shape.hasTrait(ErrorTrait.class)) {
                LOGGER.finest(() -> "Generating error " + shape.getId());
                directedCodegen.generateError(new GenerateErrorDirective<>(context, serviceShape, shape));
            } else {
                LOGGER.finest(() -> "Generating structure " + shape.getId());
                directedCodegen.generateStructure(new GenerateStructureDirective<>(context, serviceShape, shape));
            }
            return null;
        }

        @Override
        public Void unionShape(UnionShape shape) {
            LOGGER.finest(() -> "Generating union " + shape.getId());
            directedCodegen.generateUnion(new GenerateUnionDirective<>(context, serviceShape, shape));
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            if (shape.hasTrait(EnumTrait.class)) {
                LOGGER.finest(() -> "Generating string enum " + shape.getId());
                directedCodegen.generateEnumShape(new GenerateEnumDirective<>(context, serviceShape, shape));
            }
            return null;
        }

        @Override
        public Void enumShape(EnumShape shape) {
            LOGGER.finest(() -> "Generating enum shape" + shape.getId());
            directedCodegen.generateEnumShape(new GenerateEnumDirective<>(context, serviceShape, shape));
            return null;
        }

        @Override
        public Void intEnumShape(IntEnumShape shape) {
            LOGGER.finest(() -> "Generating intEnum shape" + shape.getId());
            directedCodegen.generateIntEnumShape(new GenerateIntEnumDirective<>(context, serviceShape, shape));
            return null;
        }
    }
}
