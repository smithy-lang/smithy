/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.loader;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.ValidatedResult;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.Suppression;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.model.validation.ValidatorFactory;

/**
 * Assembles and validates a {@link Model} from documents, files, shapes, and
 * other sources.
 *
 * <p>Validation vents are aggregated into a {@link Set} to ensure that
 * duplicate events are not emitted.
 *
 * @see Model#assembler()
 */
public final class ModelAssembler {
    public static final String ALLOW_UNKNOWN_TRAITS = "assembler.allowsUnknownTraits";
    private static final Logger LOGGER = Logger.getLogger(ModelAssembler.class.getName());
    private static final ModelLoader DEFAULT_LOADER = ModelLoader.createDefaultLoader();

    private TraitFactory traitFactory;
    private ValidatorFactory validatorFactory;
    private ModelLoader modelLoader = DEFAULT_LOADER;
    private final Map<String, String> stringModels = new LinkedHashMap<>();
    private final List<Validator> validators = new ArrayList<>();
    private final List<Suppression> suppressions = new ArrayList<>();
    private final List<Node> documentNodes = new ArrayList<>();
    private final List<Model> mergeModels = new ArrayList<>();
    private final List<AbstractShapeBuilder<?, ?>> shapes = new ArrayList<>();
    private final List<TraitDefinition> traitDefinitions = new ArrayList<>();
    private final Map<String, Node> metadata = new HashMap<>();
    private final Map<String, Object> properties = new HashMap<>();
    private boolean disablePrelude;

    // Lazy initialization holder class idiom to hold a default validator factory.
    private static final class LazyValidatorFactoryHolder {
        static final ValidatorFactory INSTANCE = ValidatorFactory.createServiceFactory(
                ModelAssembler.class.getClassLoader());
    }

    // Lazy initialization holder class idiom to hold a default trait factory.
    static final class LazyTraitFactoryHolder {
        static final TraitFactory INSTANCE = TraitFactory.createServiceFactory(ModelAssembler.class.getClassLoader());
    }

    /**
     * Creates a copy of the current model assembler.
     *
     * @return Returns the created model assembler copy.
     */
    public ModelAssembler copy() {
        ModelAssembler assembler = new ModelAssembler();
        assembler.traitFactory = traitFactory;
        assembler.validatorFactory = validatorFactory;
        assembler.modelLoader = modelLoader;
        assembler.stringModels.putAll(stringModels);
        assembler.validators.addAll(validators);
        assembler.suppressions.addAll(suppressions);
        assembler.documentNodes.addAll(documentNodes);
        assembler.mergeModels.addAll(mergeModels);
        assembler.shapes.addAll(shapes);
        assembler.traitDefinitions.addAll(traitDefinitions);
        assembler.metadata.putAll(metadata);
        assembler.disablePrelude = disablePrelude;
        assembler.properties.putAll(properties);
        return assembler;
    }

    /**
     * Resets the state of the ModelAssembler.
     *
     * <p>The following properties of the ModelAssembler are cleared when
     * this method is called:
     *
     * <ul>
     *     <li>Validators registered via {@link #addValidator}</li>
     *     <li>Suppressions registered via {@link #addSuppression}</li>
     *     <li>Models registered via {@link #addImport}</li>
     *     <li>Models registered via {@link #addDocumentNode}</li>
     *     <li>Models registered via {@link #addUnparsedModel}</li>
     *     <li>Models registered via {@link #addModel}</li>
     *     <li>Shape registered via {@link #addModel}</li>
     *     <li>Trait definitions registered via {@link #addTraitDefinition}</li>
     *     <li>Metadata registered via {@link #putMetadata}</li>
     *     <li>Custom properties set via {@link #putProperty}</li>
     * </ul>
     *
     * <p>The state of {@link #disablePrelude} is reset such that the prelude
     * is no longer disabled after calling {@code reset}.
     *
     * @return Returns the model assembler.
     */
    public ModelAssembler reset() {
        shapes.clear();
        traitDefinitions.clear();
        metadata.clear();
        mergeModels.clear();
        stringModels.clear();
        validators.clear();
        suppressions.clear();
        documentNodes.clear();
        properties.clear();
        disablePrelude = false;
        return this;
    }

    /**
     * Uses a custom {@link ModelLoader} for loading different file types.
     *
     * @param modelLoader Model loader to use instead of the default.
     * @return Returns the assembler.
     */
    public ModelAssembler modelLoader(ModelLoader modelLoader) {
        this.modelLoader = Objects.requireNonNull(modelLoader);
        return this;
    }

    /**
     * Uses a custom {@link TraitFactory} to resolve and configure traits.
     *
     * @param traitFactory Trait factory to use instead of the default.
     * @return Returns the assembler.
     */
    public ModelAssembler traitFactory(TraitFactory traitFactory) {
        this.traitFactory = Objects.requireNonNull(traitFactory);
        return this;
    }

    /**
     * Sets a custom {@link ValidatorFactory} used to dynamically resolve
     * validator definitions.
     *
     * <p>Note that if you do not provide an explicit validatorFactory, a
     * default factory is utilized that uses service discovery.
     *
     * @param validatorFactory Validator factory to use.
     * @return Returns the assembler.
     */
    public ModelAssembler validatorFactory(ValidatorFactory validatorFactory) {
        this.validatorFactory = Objects.requireNonNull(validatorFactory);
        return this;
    }

    /**
     * Registers a validator to be used when validating the model.
     *
     * @param validator Validator to register.
     * @return Returns the assembler.
     */
    public ModelAssembler addValidator(Validator validator) {
        validators.add(Objects.requireNonNull(validator));
        return this;
    }

    /**
     * Registers a suppression to be used when validating the model.
     *
     * @param suppression Suppression to register.
     * @return Returns the assembler.
     */
    public ModelAssembler addSuppression(Suppression suppression) {
        suppressions.add(Objects.requireNonNull(suppression));
        return this;
    }

    /**
     * Adds a string containing an unparsed model to the assembler.
     *
     * <p>The provided {@link SourceLocation} should end with one of the
     * supported filename suffixes of the {@link ModelLoader} of the
     * assemebler or the contents of the provided model need to be detected
     * by the associated ModelLoader.
     *
     * @param sourceLocation Source location to assume for the unparsed content.
     * @param model Unparsed model source.
     * @return Returns the assembler.
     */
    public ModelAssembler addUnparsedModel(String sourceLocation, String model) {
        stringModels.put(sourceLocation, model);
        return this;
    }

    /**
     * Adds a parsed JSON model file as a {@link Node} to the assembler.
     *
     * @param document Parsed document node to add.
     * @return Returns the assembler.
     */
    public ModelAssembler addDocumentNode(Node document) {
        documentNodes.add(Objects.requireNonNull(document));
        return this;
    }

    /**
     * Adds an import to the assembler.
     *
     * @param importPath Import path to add.
     * @return Returns the assembler.
     * @see #addImport(Path)
     */
    public ModelAssembler addImport(String importPath) {
        return addImport(Paths.get(Objects.requireNonNull(importPath, "importPath must not be null")));
    }

    /**
     * Adds an import to the assembler.
     *
     * <p>If a directory is found, all ".json" and ".ion" files that contain
     * a "smithy" key-value pair found in the directory and any subdirectories
     * are imported into the model.
     *
     * @param importPath Import path to add.
     * @return Returns the assembler.
     */
    public ModelAssembler addImport(Path importPath) {
        Objects.requireNonNull(importPath, "importPath must not be null");

        if (Files.isDirectory(importPath)) {
            try {
                Files.walk(importPath)
                        .filter(p -> !p.equals(importPath))
                        .filter(p -> Files.isDirectory(p) || Files.isRegularFile(p))
                        .forEach(this::addImport);
            } catch (IOException e) {
                throw new RuntimeException("Error loading the contents of " + importPath, e);
            }
        } else if (Files.isRegularFile(importPath)) {
            String contents = LoaderUtils.readUtf8File(importPath.toString());
            stringModels.put(importPath.toString(), contents);
        } else {
            throw new InvalidPathException(importPath.toString(), "Cannot find import file");
        }

        return this;
    }

    /**
     * Adds an import to the assembler from a URL.
     *
     * <pre>
     * {@code
     * Model model = Model.assembler()
     *      .addImport(getClass().getClassLoader().getResource("model.json"))
     *      .assemble()
     *      .unwrap();
     * }
     * </pre>
     *
     * @param url Resource URL to load and add.
     * @return Returns the assembler.
     */
    public ModelAssembler addImport(URL url) {
        Objects.requireNonNull(url, "The provided url to addImport was null");
        try {
            String contents = LoaderUtils.readInputStream(url.openStream(), "utf-8");
            stringModels.put(url.toExternalForm(), contents);
            return this;
        } catch (IOException e) {
            throw new RuntimeException("Unable to load Smithy model resource from URL: " + url.toExternalForm(), e);
        }
    }

    /**
     * Disables automatically loading the prelude models.
     *
     * @return Returns the assembler.
     */
    public ModelAssembler disablePrelude() {
        disablePrelude = true;
        return this;
    }

    /**
     * Explicitly injects a shape into the assembled model.
     *
     * @param shape Shape to add.
     * @return Returns the assembler.
     */
    public ModelAssembler addShape(Shape shape) {
        this.shapes.add(Shape.shapeToBuilder(shape));
        return this;
    }

    /**
     * Explicitly injects multiple shapes into the assembled model.
     *
     * @param shapes Shapes to add.
     * @return Returns the assembler.
     */
    public ModelAssembler addShapes(Shape... shapes) {
        for (Shape shape : shapes) {
            addShape(shape);
        }
        return this;
    }

    /**
     * Merges a loaded model into the model assembler.
     *
     * @param model Model to merge in.
     * @return Returns the model assembler.
     */
    public ModelAssembler addModel(Model model) {
        mergeModels.add(model);
        return this;
    }

    /**
     * Adds a trait definition to the model.
     *
     * @param definition Trait definition to add.
     * @return Returns the model assembler.
     */
    public ModelAssembler addTraitDefinition(TraitDefinition definition) {
        traitDefinitions.add(Objects.requireNonNull(definition));
        return this;
    }

    /**
     * Adds metadata to the model.
     *
     * @param name Metadata key to set.
     * @param value Metadata value to set.
     * @return Returns the model assembler.
     */
    public ModelAssembler putMetadata(String name, Node value) {
        metadata.put(Objects.requireNonNull(name), Objects.requireNonNull(value));
        return this;
    }

    /**
     * Discovers models by merging in all models returns by {@link ModelDiscovery}
     * service providers.
     *
     * @param loader Class loader to use to discover models.
     * @return Returns the model assembler.
     */
    public ModelAssembler discoverModels(ClassLoader loader) {
        return discover(ServiceLoader.load(ModelDiscovery.class, loader));
    }

    /**
     * Discovers models by merging in all models returns by {@link ModelDiscovery}
     * service providers.
     *
     * @param loader ModuleLayer to use to discover models.
     * @return Returns the model assembler.
     */
    public ModelAssembler discoverModels(ModuleLayer loader) {
        return discover(ServiceLoader.load(loader, ModelDiscovery.class));
    }

    private ModelAssembler discover(Iterable<ModelDiscovery> iterable) {
        iterable.forEach(contributor -> contributor.getModels().forEach(model -> {
            if (model == null) {
                throw new RuntimeException(String.format(
                        "Smithy ModelDiscovery implementation %s was unable to find one or more model resources",
                        contributor.getClass().getName()));
            }
            LOGGER.finest(() -> String.format(
                    "Discovered model %s with %s" + model, contributor.getClass().getName()));
            addImport(model);
        }));
        return this;
    }

    /**
     * Puts a configuration property on the ModelAssembler.
     *
     * <p>Any number of properties can be given to the model assembler to
     * affect how models are loaded. Some properties like {@link #ALLOW_UNKNOWN_TRAITS}
     * are built-in properties, while other properties can be custom
     * properties that are specific to certain {@link ModelLoader}
     * implementations.
     *
     * <p>The following example configures the ModelAssembler to emit warnings
     * for unknown traits rather than fail:
     *
     * <pre>{@code
     * ModelAssembler assembler = Model.assembler();
     * assembler.putLoaderVisitorSetting(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true);
     * }</pre>
     *
     * @param setting Name of the property to put.
     * @param value Value to set for the property.
     * @return Returns the assembler.
     */
    public ModelAssembler putProperty(String setting, Object value) {
        properties.put(setting, value);
        return this;
    }

    /**
     * Removes a setting from the ModelAssembler.
     *
     * @param setting Setting to remove.
     * @return Returns the assembler.
     */
    public ModelAssembler removeProperty(String setting) {
        properties.remove(setting);
        return this;
    }

    /**
     * Assembles the model and returns the validated result.
     *
     * @return Returns the validated result that optionally contains a Model
     *  and validation events.
     */
    public ValidatedResult<Model> assemble() {
        try {
            return doAssemble();
        } catch (SourceException e) {
            return ValidatedResult.fromErrors(List.of(ValidationEvent.fromSourceException(e)));
        }
    }

    private ValidatedResult<Model> doAssemble() {
        if (traitFactory == null) {
            traitFactory = LazyTraitFactoryHolder.INSTANCE;
        }

        var visitor = new LoaderVisitor(traitFactory, properties);

        if (!disablePrelude) {
            mergeModelIntoVisitor(Prelude.getPreludeModel(), visitor);
        }

        shapes.forEach(visitor::onShape);
        traitDefinitions.forEach(visitor::onTraitDef);
        metadata.forEach(visitor::onMetadata);

        for (var model : mergeModels) {
            mergeModelIntoVisitor(model, visitor);
        }

        if (!documentNodes.isEmpty()) {
            JsonModelLoader loader = new JsonModelLoader();
            for (var node : documentNodes) {
                loader.load(visitor, node);
            }
        }

        for (var modelEntry : stringModels.entrySet()) {
            if (!modelLoader.load(modelEntry.getKey(), modelEntry.getValue(), visitor)) {
                LOGGER.warning(() -> "No ModelLoader was able to load " + modelEntry.getKey());
            }
        }

        var modelResult = visitor.onEnd();
        return modelResult.getResult().isEmpty()
               ? modelResult
               : validate(modelResult.getResult().get(), modelResult.getValidationEvents());
    }

    private static void mergeModelIntoVisitor(Model model, LoaderVisitor visitor) {
        model.getTraitDefinitions().forEach(visitor::onTraitDef);
        model.getMetadata().forEach(visitor::onMetadata);
        model.getShapeIndex().shapes().forEach(visitor::onShape);
    }

    private ValidatedResult<Model> validate(Model model, List<ValidationEvent> modelResultEvents) {
        if (validatorFactory == null) {
            validatorFactory = LazyValidatorFactoryHolder.INSTANCE;
        }

        // Validate the model based on the explicit validators and model metadata.
        var events = ModelValidator.validate(model, validatorFactory, assembleValidators(), suppressions);
        events.addAll(modelResultEvents);
        return new ValidatedResult<>(model, events);
    }

    private List<Validator> assembleValidators() {
        // Find and register built-in validators with the validator.
        var copiedValidators = new ArrayList<>(validatorFactory.loadBuiltinValidators());
        copiedValidators.addAll(validators);
        return copiedValidators;
    }
}
