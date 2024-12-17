/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.codegen.core.SmithyIntegration;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.traits.TraitService;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.traitcodegen.generators.ShapeGenerator;
import software.amazon.smithy.traitcodegen.integrations.TraitCodegenIntegration;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;

/**
 * Orchestration class for Trait code generation.
 *
 * <p>Trait codegen executes the following steps:
 * <ul>
 *     <li>Orchestrator creation - Plugin creates an instance of {@link TraitCodegen}.</li>
 *     <li>Initialization - {@link #initialize()} is called to discover integration, filter out
 *     any shapes with excluded tags, and set up the codegen context.</li>
 *     <li>Execution - {@link #run()} is called to build a list of shapes to generate by pulling
 *     all shapes with the {@link TraitDefinition} trait applied and walking the nested shapes inside
 *     of those trait shapes. Then the {@link ShapeGenerator} is applied to each of the shapes to
 *     generate. Finally, all of the writers created during the shapes generation process are flushed.</li>
 * </ul>
 *
 */
final class TraitCodegen {
    private static final Logger LOGGER = Logger.getLogger(TraitCodegen.class.getName());
    // Get all trait definitions within a namespace
    private static final String SELECTOR_TEMPLATE = "[trait|trait][id|namespace ^= '%s']";

    private Model model;
    private final TraitCodegenSettings settings;
    private final FileManifest fileManifest;
    private final Selector traitSelector;
    private final PluginContext pluginContext;

    private List<TraitCodegenIntegration> integrations;
    private TraitCodegenContext codegenContext;

    private TraitCodegen(
            Model model,
            TraitCodegenSettings settings,
            FileManifest fileManifest,
            PluginContext pluginContext
    ) {
        this.model = Objects.requireNonNull(model);
        this.settings = Objects.requireNonNull(settings);
        this.fileManifest = Objects.requireNonNull(fileManifest);
        this.traitSelector = Selector.parse(String.format(SELECTOR_TEMPLATE, settings.smithyNamespace()));
        // Only allow this plugin to be run on the source projection.
        if (!pluginContext.getProjectionName().equals("source")) {
            throw new IllegalArgumentException("Trait code generation can ONLY be run on the `source` projection.");
        }
        this.pluginContext = pluginContext;
    }

    public static TraitCodegen fromPluginContext(PluginContext context) {
        return new TraitCodegen(
                context.getModel(),
                TraitCodegenSettings.fromNode(context.getSettings()),
                context.getFileManifest(),
                context);
    }

    public void initialize() {
        LOGGER.info("Initializing trait codegen plugin.");
        integrations = getIntegrations();
        model = applyBaseTransforms(model);
        SymbolProvider symbolProvider = createSymbolProvider();
        codegenContext = new TraitCodegenContext(model, settings, symbolProvider, fileManifest, integrations);
        registerInterceptors(codegenContext);
        LOGGER.info("Trait codegen plugin Initialized.");
    }

    public void run() {
        // Check that all required fields have been correctly initialized.
        Objects.requireNonNull(integrations, "`integrations` not initialized.");
        Objects.requireNonNull(codegenContext, "`codegenContext` not initialized.");

        // Find all trait definition shapes excluding traits in the prelude.
        LOGGER.info("Generating trait classes.");
        Set<Shape> traitClosure = getTraitClosure(codegenContext.model());
        for (Shape trait : traitClosure) {
            new ShapeGenerator().accept(new GenerateTraitDirective(codegenContext, trait));
        }

        LOGGER.info("Flushing writers");
        // Flush all writers
        if (!codegenContext.writerDelegator().getWriters().isEmpty()) {
            codegenContext.writerDelegator().flushWriters();
        }
    }

    /**
     * Applies standard transforms to the model.
     * <dl>
     *     <dt>changeStringEnumsToEnumShapes</dt>
     *     <dd>Changes string enums to enum shapes for compatibility</dd>
     *     <dt>flattenAndRemoveMixins</dt>
     *     <dd>Ensures mixins are flattened into any generated traits or nested structures</dd>
     * </dl>
     */
    private static Model applyBaseTransforms(Model model) {
        ModelTransformer transformer = ModelTransformer.create();
        model = transformer.changeStringEnumsToEnumShapes(model);
        model = transformer.flattenAndRemoveMixins(model);
        return model;
    }

    private List<TraitCodegenIntegration> getIntegrations() {
        LOGGER.fine(() -> String.format("Finding integrations using the %s class loader", getClass().getSimpleName()));
        return SmithyIntegration.sort(ServiceLoader.load(TraitCodegenIntegration.class, getClass().getClassLoader()));
    }

    private SymbolProvider createSymbolProvider() {
        SymbolProvider provider = new TraitCodegenSymbolProvider(settings, model);
        for (TraitCodegenIntegration integration : integrations) {
            provider = integration.decorateSymbolProvider(model, settings, provider);
        }
        return SymbolProvider.cache(provider);
    }

    private void registerInterceptors(TraitCodegenContext context) {
        List<CodeInterceptor<? extends CodeSection, TraitCodegenWriter>> interceptors = new ArrayList<>();
        for (TraitCodegenIntegration integration : integrations) {
            interceptors.addAll(integration.interceptors(context));
        }
        context.writerDelegator().setInterceptors(interceptors);
    }

    private Set<Shape> getTraitClosure(Model model) {
        // Get a map of existing providers, so we do not generate any trait definitions
        // for traits we have already manually defined a provider for.
        Set<ShapeId> existingProviders = new HashSet<>();
        ServiceLoader.load(TraitService.class, TraitCodegen.class.getClassLoader())
                .forEach(service -> existingProviders.add(service.getShapeId()));

        // Get all trait shapes within the specified namespace, but filter out
        // any trait shapes for which a provider is already defined or which have
        // excluded tags
        Set<Shape> traitClosure = traitSelector.select(model)
                .stream()
                .filter(pluginContext::isSourceShape)
                .filter(shape -> !existingProviders.contains(shape.getId()))
                .filter(shape -> !this.hasExcludeTag(shape))
                .collect(Collectors.toSet());

        if (traitClosure.isEmpty()) {
            LOGGER.warning("Could not find any trait definitions to generate.");
            return traitClosure;
        }

        // Find all shapes connected to trait shapes and therefore within generation closure.
        // These shapes must all be within the same namespace. Note: we do not need to add members
        // to the closure
        Set<Shape> nested = new HashSet<>();
        Walker walker = new Walker(model);
        for (Shape traitShape : traitClosure) {
            nested.addAll(walker.walkShapes(traitShape)
                    .stream()
                    .filter(shape -> !shape.isMemberShape())
                    .filter(shape -> !Prelude.isPreludeShape(shape))
                    .collect(Collectors.toSet()));
        }

        // If any nested shapes are not in the specified namespace, throw an error.
        Set<Shape> invalidNested = nested.stream()
                .filter(shape -> !shape.getId().getNamespace().startsWith(settings.smithyNamespace()))
                .collect(Collectors.toSet());
        if (!invalidNested.isEmpty()) {
            throw new RuntimeException("Shapes: " + invalidNested + " are within the trait closure but are not within "
                    + "the specified namespace `" + settings.smithyNamespace() + "`.");
        }
        traitClosure.addAll(nested);

        return traitClosure;
    }

    private boolean hasExcludeTag(Shape shape) {
        return shape.getTags().stream().anyMatch(t -> settings.excludeTags().contains(t));
    }
}
