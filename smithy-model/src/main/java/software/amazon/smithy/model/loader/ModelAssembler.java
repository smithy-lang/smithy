/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.model.validation.ValidatorFactory;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.Pair;

/**
 * Assembles and validates a {@link Model} from documents, files, shapes, and
 * other sources.
 *
 * <p>Validation vents are aggregated into a {@link Set} to ensure that
 * duplicate events are not emitted.
 *
 * <p>Smithy models found on the class path can be discovered using
 * <em>model discovery</em>. Model discovery must be explicitly requested of
 * a {@code ModelAssembler} by invoking {@link #discoverModels()} or
 * {@link #discoverModels(ClassLoader)}.
 *
 * @see Model#assembler()
 */
public final class ModelAssembler {
    /**
     * Allow unknown traits rather than fail.
     */
    public static final String ALLOW_UNKNOWN_TRAITS = "assembler.allowUnknownTraits";

    /**
     * Sets {@link URLConnection#setUseCaches} to false.
     *
     * <p>When running in a build environment, using caches can cause exceptions
     * like `java.util.zip.ZipException: ZipFile invalid LOC header (bad signature)`
     * because a previously loaded JAR might change between builds. The
     * "assembler.disableJarCache" setting should be set to true when embedding
     * Smithy into an environment where this can occur.
     */
    public static final String DISABLE_JAR_CACHE = "assembler.disableJarCache";

    private static final Logger LOGGER = Logger.getLogger(ModelAssembler.class.getName());

    private TraitFactory traitFactory;
    private ValidatorFactory validatorFactory;
    private boolean disableValidation;
    private final Map<String, Supplier<InputStream>> inputStreamModels = new HashMap<>();
    private final List<Validator> validators = new ArrayList<>();
    private final List<Node> documentNodes = new ArrayList<>();
    private final List<Model> mergeModels = new ArrayList<>();
    private final List<Shape> shapes = new ArrayList<>();
    private final List<Pair<ShapeId, Trait>> pendingTraits = new ArrayList<>();
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
        assembler.inputStreamModels.putAll(inputStreamModels);
        assembler.validators.addAll(validators);
        assembler.documentNodes.addAll(documentNodes);
        assembler.mergeModels.addAll(mergeModels);
        assembler.shapes.addAll(shapes);
        assembler.pendingTraits.addAll(pendingTraits);
        assembler.metadata.putAll(metadata);
        assembler.disablePrelude = disablePrelude;
        assembler.properties.putAll(properties);
        assembler.disableValidation = disableValidation;
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
     *     <li>Models registered via {@link #addImport}</li>
     *     <li>Models registered via {@link #addDocumentNode}</li>
     *     <li>Models registered via {@link #addUnparsedModel}</li>
     *     <li>Models registered via {@link #addModel}</li>
     *     <li>Shape registered via {@link #addModel}</li>
     *     <li>Metadata registered via {@link #putMetadata}</li>
     *     <li>Validation is re-enabled if it was disabled.</li>
     * </ul>
     *
     * <p>The state of {@link #disablePrelude} is reset such that the prelude
     * is no longer disabled after calling {@code reset}.
     *
     * @return Returns the model assembler.
     */
    public ModelAssembler reset() {
        shapes.clear();
        pendingTraits.clear();
        metadata.clear();
        mergeModels.clear();
        inputStreamModels.clear();
        validators.clear();
        documentNodes.clear();
        disablePrelude = false;
        disableValidation = false;
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
     * Adds a string containing an unparsed model to the assembler.
     *
     * <p>The provided {@code sourceLocation} string must end with
     * ".json" or ".smithy" to be parsed correctly.
     *
     * @param sourceLocation Source location to assume for the unparsed content.
     * @param model Unparsed model source.
     * @return Returns the assembler.
     */
    public ModelAssembler addUnparsedModel(String sourceLocation, String model) {
        inputStreamModels.put(sourceLocation,
                              () -> new ByteArrayInputStream(model.getBytes(StandardCharsets.UTF_8)));
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
        Objects.requireNonNull(importPath, "The importPath provided to ModelAssembler#addImport was null");

        if (Files.isDirectory(importPath)) {
            try {
                Files.walk(importPath, FileVisitOption.FOLLOW_LINKS)
                        .filter(p -> !p.equals(importPath))
                        .filter(p -> Files.isDirectory(p) || Files.isRegularFile(p))
                        .forEach(this::addImport);
            } catch (IOException e) {
                throw new ModelImportException("Error loading the contents of " + importPath, e);
            }
        } else if (Files.isRegularFile(importPath)) {
            inputStreamModels.put(importPath.toString(), () -> {
                try {
                    return Files.newInputStream(importPath);
                } catch (IOException e) {
                    throw new ModelImportException(
                            "Unable to import Smithy model from " + importPath + ": " + e.getMessage(), e);
                }
            });
        } else {
            throw new ModelImportException("Cannot find import file: " + importPath);
        }

        return this;
    }

    /**
     * Adds an import to the assembler from a URL.
     *
     * <p>The provided URL can point to a .json model, .smithy model, or
     * a .jar file that contains Smithy models.
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
        Objects.requireNonNull(url, "The provided url to ModelAssembler#addImport was null");

        // Format the key used to de-dupe files. Note that a "jar:" prefix
        // can't be removed since it's needed in order to load files from JARs
        // and differentiate between top-level JARs and contents of JARs.
        String key = url.toExternalForm();

        if (key.startsWith("file:")) {
            try {
                // Paths.get ensures paths are normalized for Windows too.
                key = Paths.get(url.toURI()).toString();
            } catch (URISyntaxException e) {
                key = key.substring(5);
            }
        }

        inputStreamModels.put(key, () -> {
            try {
                URLConnection connection = url.openConnection();
                if (properties.containsKey(ModelAssembler.DISABLE_JAR_CACHE)) {
                    connection.setUseCaches(false);
                }
                return connection.getInputStream();
            } catch (IOException | UncheckedIOException e) {
                throw new ModelImportException("Unable to open Smithy model import URL: " + url.toExternalForm(), e);
            }
        });

        return this;
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
        this.shapes.add(shape);
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
     * Explicitly adds a trait to a shape in the assembled model.
     *
     * @param target Shape to add the trait to.
     * @param trait Trait to add.
     * @return Returns the assembler.
     */
    public ModelAssembler addTrait(ShapeId target, Trait trait) {
        this.pendingTraits.add(Pair.of(target, trait));
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
     * manifests using the provided {@code ClassLoader}.
     *
     * @param loader Class loader to use to discover models.
     * @return Returns the model assembler.
     */
    public ModelAssembler discoverModels(ClassLoader loader) {
        return addDiscoveredModels(ModelDiscovery.findModels(loader));
    }

    /**
     * Discovers models by merging in all models returns by {@link ModelDiscovery}
     * manifests using the thread context {@code ClassLoader}.
     *
     * @return Returns the model assembler.
     */
    public ModelAssembler discoverModels() {
        return addDiscoveredModels(ModelDiscovery.findModels());
    }

    private ModelAssembler addDiscoveredModels(List<URL> urls) {
        for (URL url : urls) {
            LOGGER.fine(() -> "Discovered Smithy model: " + url);
            addImport(url);
        }

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
     * assembler.putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true);
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
     * Disables additional validation of the model.
     *
     * @return Returns the assembler.
     */
    public ModelAssembler disableValidation() {
        this.disableValidation = true;
        return this;
    }

    /**
     * Assembles the model and returns the validated result.
     *
     * <h2>Implementation notes</h2>
     *
     * <p>Assembling models is a multi-step process that revolves around
     * {@link ModelFile}s. ModelFiles are essentially files that contain
     * localized definitions of shapes and metadata. Some model files use
     * forward references and can't be fully resolved until all other model
     * files have been loaded. To achieve this, the assembler first creates a
     * model file that represents manually added shapes, traits, validators,
     * etc., and then parses each given import. Parsing an import is used to
     * create zero or more ModelFiles by parsing .json, .smithy, and
     * .jar files.
     *
     * <p>After the parsing phase, each model file returns the metadata
     * defined in the file using {@link ModelFile#metadata()} and the set of
     * shape IDs that were defined in the file using {@link ModelFile#shapeIds()}.
     * The metadata across each file is merged together using the rules
     * defined in the Smithy specification.
     *
     * <p>Next, the assembler calls {@link ModelFile#resolveShapes} on each
     * ModelFile which resolves any forward references, creates traits, and
     * returns all of the traits created in the file. The assembler passes in
     * the set of found shapes IDs along with a function that can be used to
     * get the {@link ShapeType} of any defined shape (this is used to coerce
     * annotation trait values into the appropriate type for a trait).
     * The assembler aggregates and merges the traits applied across all model
     * files using the merge rules defined in the Smithy specification.
     *
     * <p>Next, the assembler invokes {@link ModelFile#createShapes}, passing
     * in all of the traits defined across every model file. This method
     * causes a ModelFile to apply these traits to any shape in the ModelFile,
     * build each shape, and return the built shapes. The assembler then
     * aggregates all of the created traits, performs conflict resolution,
     * and builds a {@link Model} from the shapes and loaded metadata. A shape
     * is allowed to be defined in multiple model files if the conflicting
     * shapes are equivalent after all traits have been applied to both
     * shapes.
     *
     * @return Returns the validated result that optionally contains a Model
     *  and validation events.
     */
    public ValidatedResult<Model> assemble() {
        if (traitFactory == null) {
            traitFactory = LazyTraitFactoryHolder.INSTANCE;
        }

        // Create "model files" for the prelude, manually added shapes, imports, etc.
        List<ModelFile> modelFiles = createModelFiles();

        try {
            CompositeModelFile files = new CompositeModelFile(traitFactory, modelFiles);
            TraitContainer traits = files.resolveShapes(files.shapeIds(), files::getShapeType);
            Model model = Model.builder()
                    .metadata(files.metadata())
                    .addShapes(files.createShapes(traits))
                    .build();
            return validate(model, traits, files.events());
        } catch (SourceException e) {
            List<ValidationEvent> events = new ArrayList<>();
            events.add(ValidationEvent.fromSourceException(e));
            for (ModelFile modelFile : modelFiles) {
                events.addAll(modelFile.events());
            }
            return ValidatedResult.fromErrors(events);
        }
    }

    private List<ModelFile> createModelFiles() {
        List<ModelFile> modelFiles = new ArrayList<>();

        if (!disablePrelude) {
            modelFiles.add(new ImmutablePreludeModelFile(Prelude.getPreludeModel()));
        }

        // A modelFile is created for the assembler to capture anything that was manually added.
        FullyResolvedModelFile assemblerModelFile = FullyResolvedModelFile.fromShapes(traitFactory, shapes);
        modelFiles.add(assemblerModelFile);
        metadata.forEach(assemblerModelFile::putMetadata);
        for (Pair<ShapeId, Trait> pendingTrait : pendingTraits) {
            assemblerModelFile.onTrait(pendingTrait.left, pendingTrait.right);
        }

        // Merge in fully-built models into the assembler.
        for (Model model : mergeModels) {
            // Fully resolved models typically contain a prelude. This ensures that the prelude is not included
            // in the assembler since it would cause pointless conflicts.
            List<Shape> nonPrelude = model.shapes()
                    .filter(FunctionalUtils.not(Prelude::isPreludeShape))
                    .collect(Collectors.toList());
            FullyResolvedModelFile resolvedFile = FullyResolvedModelFile.fromShapes(traitFactory, nonPrelude);
            model.getMetadata().forEach(resolvedFile::putMetadata);
            modelFiles.add(resolvedFile);
        }

        // Load parsed AST nodes and merge them into the assembler.
        for (Node node : documentNodes) {
            try {
                modelFiles.add(ModelLoader.loadParsedNode(traitFactory, node));
            } catch (SourceException e) {
                assemblerModelFile.events().add(ValidationEvent.fromSourceException(e));
            }
        }

        // Load model files and merge them into the assembler.
        for (Map.Entry<String, Supplier<InputStream>> entry : inputStreamModels.entrySet()) {
            try {
                ModelFile loaded = ModelLoader.load(traitFactory, properties, entry.getKey(), entry.getValue());
                if (loaded == null) {
                    LOGGER.warning(() -> "No ModelLoader was able to load " + entry.getKey());
                } else {
                    modelFiles.add(loaded);
                }
            } catch (SourceException e) {
                assemblerModelFile.events().add(ValidationEvent.fromSourceException(e));
            }
        }

        return modelFiles;
    }

    private ValidatedResult<Model> validate(Model model, TraitContainer traits, List<ValidationEvent> events) {
        validateTraits(model.getShapeIds(), traits, events);

        if (disableValidation) {
            return new ValidatedResult<>(model, events);
        }

        if (validatorFactory == null) {
            validatorFactory = LazyValidatorFactoryHolder.INSTANCE;
        }

        // Validate the model based on the explicit validators and model metadata.
        List<ValidationEvent> mergedEvents = ModelValidator.validate(model, validatorFactory, assembleValidators());
        mergedEvents.addAll(events);
        return new ValidatedResult<>(model, mergedEvents);
    }

    private void validateTraits(Set<ShapeId> ids, TraitContainer resolvedTraits, List<ValidationEvent> events) {
        Severity severity = areUnknownTraitsAllowed() ? Severity.WARNING : Severity.ERROR;
        for (Map.Entry<ShapeId, Map<ShapeId, Trait>> entry : resolvedTraits.traits().entrySet()) {
            ShapeId target = entry.getKey();
            for (Trait trait : entry.getValue().values()) {
                // Find trait values that weren't defined.
                if (!ids.contains(trait.toShapeId())) {
                    events.add(ValidationEvent.builder()
                            .id(Validator.MODEL_ERROR)
                            .severity(severity)
                            .sourceLocation(trait)
                            .shapeId(target)
                            .message(String.format(
                                    "Unable to resolve trait `%s`. If this is a custom trait, then it must be "
                                    + "defined before it can be used in a model.", trait.toShapeId()))
                            .build());
                }
                // Find traits applied to shapes that don't exist.
                if (!ids.contains(target)) {
                    events.add(ValidationEvent.builder()
                            .id(Validator.MODEL_ERROR)
                            .severity(Severity.ERROR)
                            .sourceLocation(trait)
                            .message(String.format("Trait `%s` applied to unknown shape `%s`",
                                                   Trait.getIdiomaticTraitName(trait.toShapeId()), target))
                            .build());
                }
            }
        }
    }

    private boolean areUnknownTraitsAllowed() {
        Object allowUnknown = properties.get(ModelAssembler.ALLOW_UNKNOWN_TRAITS);
        return allowUnknown != null && (boolean) allowUnknown;
    }

    private List<Validator> assembleValidators() {
        // Find and register built-in validators with the validator.
        List<Validator> copiedValidators = new ArrayList<>(validatorFactory.loadBuiltinValidators());
        copiedValidators.addAll(validators);
        return copiedValidators;
    }
}
