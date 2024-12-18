/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.JarUtils;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.InternalTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.SuppressTrait;
import software.amazon.smithy.model.traits.TagsTrait;
import software.amazon.smithy.model.traits.synthetic.OriginalShapeIdTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidatedResultException;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.model.validation.ValidatorFactory;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;

public class ModelAssemblerTest {

    private Path outputDirectory;

    @BeforeEach
    public void before() throws IOException {
        outputDirectory = Files.createTempDirectory(getClass().getName());
    }

    @AfterEach
    public void after() throws IOException {
        Files.walk(outputDirectory).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

    @Test
    public void addsExplicitShapes() {
        StringShape shape = StringShape.builder().id("ns.foo#Bar").build();
        ValidatedResult<Model> result = new ModelAssembler()
                .addShape(shape)
                .assemble();

        assertThat(result.getValidationEvents(), empty());
        assertThat(result.unwrap().getShape(ShapeId.from("ns.foo#Bar")), is(Optional.of(shape)));
    }

    @Test
    public void addsExplicitTraits() {
        StringShape shape = StringShape.builder().id("ns.foo#Bar").build();
        SuppressTrait trait = SuppressTrait.builder().build();
        ValidatedResult<Model> result = new ModelAssembler()
                .addShape(shape)
                .addTrait(shape.toShapeId(), trait)
                .assemble();

        assertThat(result.getValidationEvents(), empty());
        Shape resultShape = result.unwrap().getShape(ShapeId.from("ns.foo#Bar")).get();
        assertTrue(resultShape.findTrait("smithy.api#suppress").isPresent());
        assertTrue(resultShape.getTrait(SuppressTrait.class).isPresent());
    }

    @Test
    public void addsExplicitTraitsToBuiltModel() {
        StringShape shape = StringShape.builder().id("ns.foo#Bar").build();
        SuppressTrait trait = SuppressTrait.builder().build();
        ValidatedResult<Model> result = new ModelAssembler()
                .addModel(Model.assembler().addShape(shape).assemble().unwrap())
                .addTrait(shape.toShapeId(), trait)
                .assemble();

        assertThat(result.getValidationEvents(), empty());
        Shape resultShape = result.unwrap().getShape(ShapeId.from("ns.foo#Bar")).get();
        assertTrue(resultShape.findTrait("smithy.api#suppress").isPresent());
        assertTrue(resultShape.getTrait(SuppressTrait.class).isPresent());
    }

    @Test
    public void addsExplicitTraitsToUnparsedModel() {
        String unparsed =
                "{\"smithy\": \"" + Model.MODEL_VERSION + "\", \"shapes\": { \"ns.foo#Bar\": { \"type\": \"string\"}}}";
        SuppressTrait trait = SuppressTrait.builder().build();
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel(SourceLocation.NONE.getFilename(), unparsed)
                .addTrait(ShapeId.from("ns.foo#Bar"), trait)
                .assemble();

        assertThat(result.getValidationEvents(), empty());
        Shape resultShape = result.unwrap().getShape(ShapeId.from("ns.foo#Bar")).get();
        assertTrue(resultShape.findTrait("smithy.api#suppress").isPresent());
        assertTrue(resultShape.getTrait(SuppressTrait.class).isPresent());
    }

    @Test
    public void addsExplicitTraitsToParsedDocumentNode() {
        String unparsed =
                "{\"smithy\": \"" + Model.MODEL_VERSION + "\", \"shapes\": { \"ns.foo#Bar\": { \"type\": \"string\"}}}";
        SuppressTrait trait = SuppressTrait.builder().build();
        ValidatedResult<Model> result = new ModelAssembler()
                .addDocumentNode(Node.parse(unparsed, SourceLocation.NONE.getFilename()))
                .addTrait(ShapeId.from("ns.foo#Bar"), trait)
                .assemble();

        assertThat(result.getValidationEvents(), empty());
        Shape resultShape = result.unwrap().getShape(ShapeId.from("ns.foo#Bar")).get();
        assertTrue(resultShape.findTrait("smithy.api#suppress").isPresent());
        assertTrue(resultShape.getTrait(SuppressTrait.class).isPresent());
    }

    @Test
    public void addsExplicitDocumentNode_1_0_0() {
        ObjectNode node = Node.objectNode()
                .withMember("smithy", "1.0")
                .withMember("shapes",
                        Node.objectNode()
                                .withMember("ns.foo#String",
                                        Node.objectNode()
                                                .withMember("type", Node.from("string"))));
        ValidatedResult<Model> result = new ModelAssembler().addDocumentNode(node).assemble();

        assertTrue(result.unwrap().getShape(ShapeId.from("ns.foo#String")).isPresent());
    }

    @Test
    public void addsExplicitUnparsedDocumentNode() {
        String document = "{\"smithy\": \"" + Model.MODEL_VERSION
                + "\", \"shapes\": { \"ns.foo#String\": { \"type\": \"string\"}}}";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel(SourceLocation.NONE.getFilename(), document)
                .assemble();

        assertThat(result.getValidationEvents(), empty());
        assertTrue(result.unwrap().getShape(ShapeId.from("ns.foo#String")).isPresent());
    }

    @Test
    public void addsExplicitValidators() {
        ValidationEvent event = ValidationEvent.builder()
                .severity(Severity.ERROR)
                .id("Foo")
                .message("bar")
                .build();
        String document = "{\"smithy\": \"" + Model.MODEL_VERSION + "\"}";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel(SourceLocation.NONE.getFilename(), document)
                .addValidator(index -> Collections.singletonList(event))
                .assemble();

        assertThat(result.getValidationEvents(), contains(event));
    }

    @Test
    public void detectsTraitsOnUnknownShape() {
        String document = "{\"smithy\": \"" + Model.MODEL_VERSION
                + "\", \"shapes\": {\"ns.foo#Unknown\": {\"type\": \"apply\", \"traits\": {\"smithy.api#documentation\": \"foo\"}}}}";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel(SourceLocation.NONE.getFilename(), document)
                .assemble();

        assertThat(result.getValidationEvents(), hasSize(1));
        assertThat(result.getValidationEvents().get(0).getMessage(),
                containsString(
                        "Trait `documentation` applied to unknown shape `ns.foo#Unknown`"));
        assertThat(result.getValidationEvents().get(0).getSeverity(), is(Severity.ERROR));
    }

    @Test
    public void detectsInvalidShapeTypes() {
        ValidatedResult<Model> result = new ModelAssembler()
                .addImport(getClass().getResource("invalid-shape-types.json"))
                .assemble();

        assertThat(result.getValidationEvents(), hasSize(1));
        assertThat(result.getValidationEvents().get(0).getMessage(), containsString("Invalid shape `type`: foobaz"));
        assertThat(result.getValidationEvents().get(0).getSeverity(), is(Severity.ERROR));
        assertTrue(result.getResult()
                .get()
                .getShape(ShapeId.from("example.namespace#String"))
                .isPresent());
    }

    @Test
    public void detectsUnresolvedImports() {
        Assertions.assertThrows(RuntimeException.class, () -> new ModelAssembler().addImport("/bad/path"));
    }

    @Test
    public void importsSymlinksDirectoryWithAllShapes() throws Exception {
        ValidatedResult<Model> result = new ModelAssembler()
                .addImport(getClass().getResource("main.json"))
                .addImport(createSymbolicLink(Paths.get(getClass().getResource("nested").toURI()), "symlink-nested"))
                .assemble();

        Model model = result.unwrap();
        assertTrue(model.getShape(ShapeId.from("example.namespace#String")).isPresent());
        assertThat(model.getShape(ShapeId.from("example.namespace#String")).get().getType(),
                is(ShapeType.STRING));
        assertThat(model.getShape(ShapeId.from("example.namespace#String2")).get().getType(),
                is(ShapeType.STRING));
        assertThat(model.getShape(ShapeId.from("example.namespace#String3")).get().getType(),
                is(ShapeType.STRING));
        assertThat(model.getShape(ShapeId.from("example.namespace#String")).get().getType(),
                is(ShapeType.STRING));
        assertThat(model.getShape(ShapeId.from("example.namespace#Integer")).get().getType(),
                is(ShapeType.INTEGER));
        assertThat(model.getShape(ShapeId.from("example.namespace#Long")).get().getType(),
                is(ShapeType.LONG));
        assertThat(model.getShape(ShapeId.from("example.namespace#Float")).get().getType(),
                is(ShapeType.FLOAT));
        assertThat(model.getShape(ShapeId.from("example.namespace#BigDecimal")).get().getType(),
                is(ShapeType.BIG_DECIMAL));
        assertThat(model.getShape(ShapeId.from("example.namespace#BigInteger")).get().getType(),
                is(ShapeType.BIG_INTEGER));
        assertThat(model.getShape(ShapeId.from("example.namespace#Blob")).get().getType(),
                is(ShapeType.BLOB));
        assertThat(model.getShape(ShapeId.from("example.namespace#Boolean")).get().getType(),
                is(ShapeType.BOOLEAN));
        assertThat(model.getShape(ShapeId.from("example.namespace#Timestamp")).get().getType(),
                is(ShapeType.TIMESTAMP));
        assertThat(model.getShape(ShapeId.from("example.namespace#List")).get().getType(),
                is(ShapeType.LIST));
        assertThat(model.getShape(ShapeId.from("example.namespace#Map")).get().getType(),
                is(ShapeType.MAP));
        assertThat(model.getShape(ShapeId.from("example.namespace#Structure")).get().getType(),
                is(ShapeType.STRUCTURE));
        assertThat(model.getShape(ShapeId.from("example.namespace#TaggedUnion")).get().getType(),
                is(ShapeType.UNION));
        assertThat(model.getShape(ShapeId.from("example.namespace#Resource")).get().getType(),
                is(ShapeType.RESOURCE));
        assertThat(model.getShape(ShapeId.from("example.namespace#Operation")).get().getType(),
                is(ShapeType.OPERATION));
        assertThat(model.getShape(ShapeId.from("example.namespace#Service")).get().getType(),
                is(ShapeType.SERVICE));

        ShapeId stringId = ShapeId.from("example.namespace#String");
        Optional<SensitiveTrait> sensitiveTrait = model
                .getShape(stringId)
                .get()
                .getTrait(SensitiveTrait.class);
        assertTrue(sensitiveTrait.isPresent());

        assertThat(model.getMetadata(), hasKey("foo"));
        assertThat(model.getMetadata().get("foo"), equalTo(Node.from("baz")));
        assertThat(model.getMetadata(), hasKey("bar"));
        assertThat(model.getMetadata().get("bar"), equalTo(Node.from("qux")));
        assertThat(model.getMetadata(), hasKey("lorem"));
        assertThat(model.getMetadata().get("lorem"), equalTo(Node.from("ipsum")));
        assertThat(model.getMetadata(), hasKey("list"));
        assertThat(model.getMetadata().get("list").expectArrayNode().getElementsAs(StringNode::getValue),
                containsInAnyOrder("a", "b", "c"));

        // The String shape should have a documentation trait applied.
        assertTrue(model.getShape(ShapeId.from("example.namespace#String"))
                .flatMap(shape -> shape.getTrait(DocumentationTrait.class))
                .isPresent());
    }

    @Test
    public void importsSymlinkFileWithAllShapes() throws Exception {
        ValidatedResult<Model> result = new ModelAssembler()
                .addImport(getClass().getResource("main.json"))
                .addImport(createSymbolicLink(
                        Paths.get(getClass().getResource("nested/merges-2.json").toURI()),
                        "symlink-merges-2.json"))
                .assemble();
        assertThat(result.getValidationEvents(), empty());
        Model model = result.unwrap();
        // The String shape should have a documentation trait applied.
        assertTrue(model.getShape(ShapeId.from("example.namespace#String"))
                .flatMap(shape -> shape.getTrait(DocumentationTrait.class))
                .isPresent());
    }

    public Path createSymbolicLink(Path target, String linkName) throws IOException {
        Path link = outputDirectory.resolve(linkName);
        if (Files.exists(link)) {
            Files.delete(link);
        }
        try {
            return Files.createSymbolicLink(link, target);
        } catch (FileSystemException e) {
            // Skip tests if symlinks are unable to be created. This can happen on Windows, for instance, where the
            // permissions to create them are not enabled by default.
            Assumptions.assumeFalse(System.getProperty("os.name").toLowerCase().contains("windows"), e.getMessage());
            throw e;
        }
    }

    @Test
    public void importsFilesWithAllShapes() throws Exception {
        ValidatedResult<Model> result = new ModelAssembler()
                .addImport(getClass().getResource("main.json"))
                .addImport(Paths.get(getClass().getResource("nested").toURI()))
                .assemble();

        Model model = result.unwrap();
        assertTrue(model.getShape(ShapeId.from("example.namespace#String")).isPresent());
        assertThat(model.getShape(ShapeId.from("example.namespace#String")).get().getType(),
                is(ShapeType.STRING));
        assertThat(model.getShape(ShapeId.from("example.namespace#String2")).get().getType(),
                is(ShapeType.STRING));
        assertThat(model.getShape(ShapeId.from("example.namespace#String3")).get().getType(),
                is(ShapeType.STRING));
        assertThat(model.getShape(ShapeId.from("example.namespace#String")).get().getType(),
                is(ShapeType.STRING));
        assertThat(model.getShape(ShapeId.from("example.namespace#Integer")).get().getType(),
                is(ShapeType.INTEGER));
        assertThat(model.getShape(ShapeId.from("example.namespace#Long")).get().getType(),
                is(ShapeType.LONG));
        assertThat(model.getShape(ShapeId.from("example.namespace#Float")).get().getType(),
                is(ShapeType.FLOAT));
        assertThat(model.getShape(ShapeId.from("example.namespace#BigDecimal")).get().getType(),
                is(ShapeType.BIG_DECIMAL));
        assertThat(model.getShape(ShapeId.from("example.namespace#BigInteger")).get().getType(),
                is(ShapeType.BIG_INTEGER));
        assertThat(model.getShape(ShapeId.from("example.namespace#Blob")).get().getType(),
                is(ShapeType.BLOB));
        assertThat(model.getShape(ShapeId.from("example.namespace#Boolean")).get().getType(),
                is(ShapeType.BOOLEAN));
        assertThat(model.getShape(ShapeId.from("example.namespace#Timestamp")).get().getType(),
                is(ShapeType.TIMESTAMP));
        assertThat(model.getShape(ShapeId.from("example.namespace#List")).get().getType(),
                is(ShapeType.LIST));
        assertThat(model.getShape(ShapeId.from("example.namespace#Map")).get().getType(),
                is(ShapeType.MAP));
        assertThat(model.getShape(ShapeId.from("example.namespace#Structure")).get().getType(),
                is(ShapeType.STRUCTURE));
        assertThat(model.getShape(ShapeId.from("example.namespace#TaggedUnion")).get().getType(),
                is(ShapeType.UNION));
        assertThat(model.getShape(ShapeId.from("example.namespace#Resource")).get().getType(),
                is(ShapeType.RESOURCE));
        assertThat(model.getShape(ShapeId.from("example.namespace#Operation")).get().getType(),
                is(ShapeType.OPERATION));
        assertThat(model.getShape(ShapeId.from("example.namespace#Service")).get().getType(),
                is(ShapeType.SERVICE));

        ShapeId stringId = ShapeId.from("example.namespace#String");
        Optional<SensitiveTrait> sensitiveTrait = model
                .getShape(stringId)
                .get()
                .getTrait(SensitiveTrait.class);
        assertTrue(sensitiveTrait.isPresent());

        assertThat(model.getMetadata(), hasKey("foo"));
        assertThat(model.getMetadata().get("foo"), equalTo(Node.from("baz")));
        assertThat(model.getMetadata(), hasKey("bar"));
        assertThat(model.getMetadata().get("bar"), equalTo(Node.from("qux")));
        assertThat(model.getMetadata(), hasKey("lorem"));
        assertThat(model.getMetadata().get("lorem"), equalTo(Node.from("ipsum")));
        assertThat(model.getMetadata(), hasKey("list"));
        assertThat(model.getMetadata().get("list").expectArrayNode().getElementsAs(StringNode::getValue),
                containsInAnyOrder("a", "b", "c"));

        // The String shape should have a documentation trait applied.
        assertTrue(model.getShape(ShapeId.from("example.namespace#String"))
                .flatMap(shape -> shape.getTrait(DocumentationTrait.class))
                .isPresent());
    }

    @Test
    public void reportsSourceLocationOfShapesImportedFromFiles() throws Exception {
        Model model = new ModelAssembler()
                .addImport(getClass().getResource("main.json"))
                .addImport(Paths.get(getClass().getResource("nested").toURI()))
                .assemble()
                .unwrap();

        String expectedPath = Paths.get(getClass().getResource("main.json").toURI()).toString();
        assertThat(model.getShape(ShapeId.from("example.namespace#String")).get().getSourceLocation(),
                is(new SourceLocation(expectedPath, 4, 37)));
        assertThat(model.getShape(ShapeId.from("example.namespace#TaggedUnion$foo")).get().getSourceLocation(),
                is(new SourceLocation(expectedPath, 69, 24)));
    }

    @Test
    public void detectsMetadataConflicts() {
        ValidatedResult<Model> result = new ModelAssembler()
                .addImport(getClass().getResource("metadata-conflicts.json"))
                .addImport(getClass().getResource("metadata-conflicts-2.json"))
                .assemble();

        assertThat(result.getValidationEvents(), hasSize(1));
        assertThat(result.getValidationEvents().get(0).getMessage(),
                containsString("Metadata conflict for key `foo`"));
    }

    @Test
    public void metadataIsNotAffectedByTheSourceName() {
        Model model1 = new ModelAssembler()
                .addUnparsedModel("a1.smithy", "metadata items = [1]")
                .addUnparsedModel("a2.smithy", "metadata items = [2]")
                .addUnparsedModel("a3.smithy", "metadata items = [3]")
                .assemble()
                .unwrap();
        Model model2 = new ModelAssembler()
                .addUnparsedModel("b1.smithy", "metadata items = [1]")
                .addUnparsedModel("b2.smithy", "metadata items = [2]")
                .addUnparsedModel("b3.smithy", "metadata items = [3]")
                .assemble()
                .unwrap();
        List<Number> metadata1 = model1.getMetadata()
                .get("items")
                .expectArrayNode()
                .getElements()
                .stream()
                .map(s -> s.expectNumberNode().getValue())
                .collect(Collectors.toList());
        List<Number> metadata2 = model2.getMetadata()
                .get("items")
                .expectArrayNode()
                .getElements()
                .stream()
                .map(s -> s.expectNumberNode().getValue())
                .collect(Collectors.toList());
        assertThat(metadata1, is(metadata2));
    }

    @Test
    public void mergesMultipleModels() {
        Model model = new ModelAssembler()
                .addImport(getClass().getResource("merges-1.json"))
                .addImport(getClass().getResource("nested/merges-2.json"))
                .addImport(getClass().getResource("nested/merges-3.json"))
                .assemble()
                .unwrap();

        assertImportPathsWereLoaded(model);
    }

    @Test
    public void mergesDirectories() throws Exception {
        Model model = new ModelAssembler()
                .addImport(getClass().getResource("merges-1.json"))
                .addImport(Paths.get(getClass().getResource("nested").toURI()))
                .assemble()
                .unwrap();

        assertImportPathsWereLoaded(model);
    }

    private void assertImportPathsWereLoaded(Model model) {
        assertTrue(model.getShape(ShapeId.from("example.namespace#String"))
                .flatMap(shape -> shape.getTrait(DocumentationTrait.class))
                .isPresent());
        assertTrue(model.getShape(ShapeId.from("example.namespace#String"))
                .flatMap(shape -> shape.getTrait(MediaTypeTrait.class))
                .isPresent());
    }

    @Test
    public void canAddEntireModelsToAssembler() {
        Model model = new ModelAssembler().addImport(getClass().getResource("main.json")).assemble().unwrap();
        Model model2 = Model.assembler()
                .addModel(model)
                .addUnparsedModel("N/A",
                        "{\"smithy\": \"" + Model.MODEL_VERSION
                                + "\", \"shapes\": {\"example.namespace#String\": {\"type\": \"apply\", \"traits\": {\"smithy.api#documentation\": \"hi\"}}}}")
                .assemble()
                .unwrap();

        assertEquals("hi",
                model2.expectShape(ShapeId.from("example.namespace#String"))
                        .getTrait(DocumentationTrait.class)
                        .get()
                        .getValue());
    }

    @Test
    public void canAddExplicitMetadata() {
        Model model = new ModelAssembler().putMetadata("foo", Node.from("bar")).assemble().unwrap();

        assertThat(model.getMetadataProperty("foo"), equalTo(Optional.of(Node.from("bar"))));
    }

    @Test
    public void canLoadConfigurableValidators() {
        Map<String, ObjectNode> loaded = new HashMap<>();
        ValidatorFactory factory = new ValidatorFactory() {
            @Override
            public List<Validator> loadBuiltinValidators() {
                return ListUtils.of();
            }

            @Override
            public Optional<Validator> createValidator(String name, ObjectNode configuration) {
                loaded.put(name, configuration);
                return Optional.of(model -> ListUtils.of());
            }
        };

        new ModelAssembler()
                .addImport(getClass().getResource("loads-validators.smithy"))
                .validatorFactory(factory)
                .assemble()
                .unwrap();

        assertThat(loaded, hasKey("MyValidator"));
        assertThat(loaded.get("MyValidator"), equalTo(Node.parse("{\"foo\":\"baz\"}")));
    }

    @Test
    public void canSuppressCoreEvents() {
        Map<String, ObjectNode> loaded = new HashMap<>();
        ValidatorFactory factory = new ValidatorFactory() {
            @Override
            public List<Validator> loadBuiltinValidators() {
                return ListUtils.of();
            }

            @Override
            public Optional<Validator> createValidator(String name, ObjectNode configuration) {
                loaded.put(name, configuration);
                return Optional.of(model -> ListUtils.of());
            }
        };
        List<ValidationEvent> collectedEvents = Collections.synchronizedList(new ArrayList<>());
        ValidatedResult<Model> result = new ModelAssembler()
                .addImport(getClass().getResource("core-events.smithy"))
                .validatorFactory(factory)
                .validationEventListener(collectedEvents::add)
                .assemble();
        for (ValidationEvent event : result.getValidationEvents()) {
            assertEquals(event.getSeverity(), Severity.SUPPRESSED);
        }
        for (ValidationEvent event : collectedEvents) {
            assertEquals(event.getSeverity(), Severity.SUPPRESSED);
        }
    }

    @Test
    public void canIgnoreUnknownTraits() {
        String document =
                "{\"smithy\": \"" + Model.MODEL_VERSION + "\", "
                        + "\"shapes\": { "
                        + "\"ns.foo#String\": {"
                        + "\"type\": \"string\", \"traits\": {\"com.foo#invalidTrait\": true}}}}";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel(SourceLocation.NONE.getFilename(), document)
                .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                .assemble();

        assertThat(result.getValidationEvents(), not(empty()));
        assertFalse(result.isBroken());
    }

    @Test
    public void canLoadModelsFromJar() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("jar-import.jar"))
                .assemble()
                .unwrap();

        for (String id : ListUtils.of("foo.baz#A", "foo.baz#B", "foo.baz#C")) {
            ShapeId shapeId = ShapeId.from(id);
            assertTrue(model.getShape(shapeId).isPresent());
            assertThat(model.getShape(shapeId).get().getSourceLocation().getFilename(),
                    startsWith("jar:file:"));
        }
    }

    @Test
    public void canLoadTraitFromJarMultipleTimes() {
        URL jar = getClass().getResource("jar-traits-import.jar");
        URL file = getClass().getResource("loads-jar-traits.smithy");

        Supplier<Model> modelSupplier = () -> {
            URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {jar});
            return Model.assembler(urlClassLoader)
                    .discoverModels(urlClassLoader)
                    .addImport(file)
                    .assemble()
                    .unwrap();
        };

        Model model = modelSupplier.get();
        assertTrue(model.getShape(ShapeId.from("smithy.test#test")).isPresent());

        Model reloadedModel = modelSupplier.get();
        assertTrue(reloadedModel.getShape(ShapeId.from("smithy.test#test")).isPresent());
    }

    @Test
    public void canDisableValidation() {
        String document = "namespace foo.baz\n"
                + "@idempotent\n" // < this is invalid
                + "string MyString\n";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel("foo.smithy", document)
                .disableValidation()
                .assemble();

        assertFalse(result.isBroken());
    }

    @Test
    public void canHandleBadSuppressions() {
        String document = "namespace foo.baz\n"
                + "@idempotent\n" // < this is invalid
                + "string MyString\n";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel("foo.smithy", document)
                .putMetadata("suppressions", Node.parse("[{}]"))
                .assemble();
        assertTrue(result.isBroken());
    }

    @Test
    public void detectsDuplicateTraitsWithDifferentTypes() {
        // This test ensures that traits with different types are detected too.
        // This is picked up by normal trait collision, but this test is a good
        // regression test to ensure that trait specific stuff like coercion
        // is handled correctly.
        String document1 = "namespace foo.baz\n"
                + "@trait\n"
                + "structure myTrait {}\n";
        String document2 = "namespace foo.baz\n"
                + "@trait\n"
                + "integer myTrait\n";
        String document3 = "namespace foo.baz\n"
                + "@myTrait(10)\n"
                + "string MyShape\n";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel("1.smithy", document1)
                .addUnparsedModel("2.smithy", document2)
                .addUnparsedModel("3.smithy", document3)
                .assemble();

        assertTrue(result.isBroken());
    }

    @Test
    public void allowsConflictingShapesThatAreEqual() {
        // While these two shapes have different traits, the traits merge.
        // Since they are equivalent the conflicts are allowed.
        String document1 = "namespace foo.baz\n"
                + "@deprecated\n"
                + "structure Foo {\n"
                + "    foo: String,"
                + "}\n";
        String document2 = "namespace foo.baz\n"
                + "structure Foo {\n"
                + "    @internal\n"
                + "    foo: String,\n"
                + "}\n";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel("1.smithy", document1)
                .addUnparsedModel("2.smithy", document2)
                .assemble();

        assertFalse(result.isBroken());

        // Ensure that traits across each duplicate are all merged together.
        StructureShape shape = result.unwrap().expectShape(ShapeId.from("foo.baz#Foo"), StructureShape.class);
        assertTrue(shape.hasTrait(DeprecatedTrait.class));
        assertTrue(shape.getMember("foo").isPresent());
        assertTrue(shape.getMember("foo").get().hasTrait(InternalTrait.class));
    }

    @Test
    public void detectsConflictingDuplicateAggregates() {
        // Aggregate shapes have to have the same exact members.
        String document1 = "namespace foo.baz\n"
                + "structure Foo {\n"
                + "    foo: String,"
                + "}\n";
        String document2 = "namespace foo.baz\n"
                + "structure Foo {}\n";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel("1.smithy", document1)
                .addUnparsedModel("2.smithy", document2)
                .assemble();

        assertTrue(result.isBroken());
    }

    @Test
    public void allowsDuplicateEquivalentMetadata() {
        String document = "metadata foo = 10\n";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel("1.smithy", document)
                .addUnparsedModel("2.smithy", document)
                .assemble();

        assertFalse(result.isBroken());
        assertThat(result.unwrap().getMetadata().get("foo"), equalTo(Node.from(10)));
    }

    @Test
    public void mergesConflictingMetadataArrays() {
        String document = "metadata foo = [\"a\"]\n";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel("1.smithy", document)
                .addUnparsedModel("2.smithy", document)
                .assemble();

        assertFalse(result.isBroken());
        assertThat(result.unwrap().getMetadata().get("foo"), equalTo(Node.fromStrings("a", "a")));
    }

    @Test
    public void createsDynamicTraitWhenTraitFactoryReturnsEmpty() {
        ShapeId id = ShapeId.from("ns.foo#Test");
        ShapeId traitId = ShapeId.from("smithy.foo#baz");
        String document = "{\n"
                + "\"smithy\": \"" + Model.MODEL_VERSION + "\",\n"
                + "    \"shapes\": {\n"
                + "        \"" + id + "\": {\n"
                + "            \"type\": \"string\",\n"
                + "            \"traits\": {\"" + traitId + "\": true}\n"
                + "        }\n"
                + "    }\n"
                + "}";
        Model model = new ModelAssembler()
                .addUnparsedModel(SourceLocation.NONE.getFilename(), document)
                .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                .assemble()
                .unwrap();

        assertTrue(model.expectShape(id).findTrait(traitId).isPresent());
        assertThat(model.expectShape(id).findTrait(traitId).get(), instanceOf(DynamicTrait.class));
    }

    @Test
    public void dedupesExactSameTraitsAndMetadataFromSameLocation() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("dedupe-models.smithy"))
                .assemble()
                .unwrap();
        Model model2 = Model.assembler()
                .addModel(model)
                .addImport(getClass().getResource("dedupe-models.smithy"))
                .assemble()
                .unwrap();

        assertThat(model, equalTo(model2));
    }

    @Test
    public void canListenToEvents() {
        List<ValidationEvent> toEmit = new ArrayList<>();
        toEmit.add(ValidationEvent.builder().id("a").severity(Severity.WARNING).message("").build());
        toEmit.add(ValidationEvent.builder().id("b").severity(Severity.WARNING).message("").build());
        toEmit.add(ValidationEvent.builder().id("c").severity(Severity.WARNING).message("").build());
        List<ValidationEvent> collectedEvents = Collections.synchronizedList(new ArrayList<>());

        Model.assembler()
                .addValidator(model -> toEmit)
                .validationEventListener(collectedEvents::add)
                .assemble()
                .unwrap();

        assertThat(collectedEvents, equalTo(toEmit));
    }

    // Synthetic traits are used to add information to shapes that's not persisted on the
    // shape when serializing, and the trait might not be defined in the metamodel. This
    // requires that validators ignore synthetic traits, and that the model assembler doesn't
    // fail if an unknown synthetic trait is encountered.
    //
    // This validator ensures that synthetic traits don't trip up the assembler, and that
    // built-in validators don't care that the trait isn't defined in the semantic model.
    // They only need to be defined in code.
    @Test
    public void transientTraitsAreNotValidated() {
        ShapeId originalId = ShapeId.from("com.foo.nested#Str");
        StringShape stringShape = StringShape.builder()
                .id("com.foo#Str")
                .addTrait(new OriginalShapeIdTrait(originalId))
                .build();
        Model model = Model.assembler()
                .addShape(stringShape)
                .assemble()
                .unwrap();

        assertThat(model.expectShape(stringShape.getId()).expectTrait(OriginalShapeIdTrait.class).getOriginalId(),
                equalTo(originalId));
    }

    // Synthetic traits should not be parsed again. That will cause a
    // failure, for instance, if we import a 1.0 model that has @enum
    // traits. If we try to reparse them this will fail with an error
    // Unable to resolve trait `smithy.synthetic#enum`
    @Test
    public void doesNotReParsesSyntheticTraits() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("does-not-reparses-synthetic-traits.smithy"))
                .assemble()
                .unwrap();

        Model.assembler()
                .addModel(model)
                .addUnparsedModel("foo.smithy", "namespace smithy.example\napply Foo @sensitive\n")
                .assemble()
                .unwrap();
    }

    @Test
    public void resetDoesNotBarf() {
        ModelAssembler assembler = new ModelAssembler();
        assembler.assemble();
        assembler.reset();
        assembler.assemble();
    }

    // reset() affects multiple properties of the ModelAssembler. The test asserts one of them (shapes).
    @Test
    public void reset() {
        ModelAssembler assembler = new ModelAssembler();

        StringShape shape = StringShape.builder().id("ns.foo#Bar").build();
        assembler.addShape(shape);
        ValidatedResult<Model> result = assembler.assemble();
        assertThat(result.unwrap().getShape(ShapeId.from("ns.foo#Bar")), is(Optional.of(shape)));

        assembler.reset();
        result = assembler.assemble();
        assertThat(result.unwrap().getShape(ShapeId.from("ns.foo#Bar")), is(Optional.empty()));
    }

    public void loadsMixinMembersInCorrectOrderAndWithTraits() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("mixins/mixins-can-override-traits.smithy"))
                .assemble()
                .unwrap();

        StructureShape f = model.expectShape(ShapeId.from("smithy.example#F"), StructureShape.class);

        assertThat(f.getMemberNames(), contains("a", "b", "c", "d", "e", "f"));
        assertThat(f.getMember("a").get().expectTrait(DocumentationTrait.class).getValue(), equalTo("I've changed"));
        assertThat(f.getMember("c").get().expectTrait(DocumentationTrait.class).getValue(), equalTo("I've changed"));
        assertTrue(f.getMember("c").get().hasTrait(InternalTrait.class));
    }

    @Test
    public void ignoresAcceptableMixinConflicts() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("mixins/mixin-conflict-acceptable-1.smithy"))
                .addImport(getClass().getResource("mixins/mixin-conflict-acceptable-2.smithy"))
                .assemble()
                .unwrap();

        StructureShape a = model.expectShape(ShapeId.from("smithy.example#A"), StructureShape.class);

        assertThat(a.getMemberNames(), contains("b", "a"));
    }

    @Test
    public void failsWhenMixinsConflictAndAreNotEquivalent() {
        ValidatedResultException e = Assertions.assertThrows(ValidatedResultException.class, () -> {
            Model.assembler()
                    .addImport(getClass().getResource("mixins/mixin-conflict-acceptable-1.smithy"))
                    .addImport(getClass().getResource("mixins/mixin-conflict-error.smithy"))
                    .assemble()
                    .unwrap();
        });

        assertThat(e.getMessage(), containsString("Conflicting shape definition for `smithy.example#A`"));
    }

    @Test
    public void canLoadSetsUsingBuiltModel() {
        SetShape set = SetShape.builder()
                .id("smithy.example#Set")
                .member(ShapeId.from("smithy.api#String"))
                .build();
        Model model = Model.assembler()
                .addShape(set)
                .assemble()
                .unwrap();

        Model.assembler().addModel(model).assemble().unwrap();
    }

    @Test
    public void canIgnoreTraitConflictsWithBuiltShapes() {
        StringShape string1 = StringShape.builder()
                .id("smithy.example#String1")
                .addTrait(new DocumentationTrait("hi"))
                .build();
        ModelAssembler assembler = Model.assembler();
        assembler.addShape(string1);
        assembler.addUnparsedModel("foo.smithy",
                "$version: \"2.0\"\n"
                        + "namespace smithy.example\n\n"
                        + "@documentation(\"hi\")\n"
                        + "string String1\n");
        Model result = assembler.assemble().unwrap();

        assertThat(result.expectShape(string1.getId()).expectTrait(DocumentationTrait.class).getValue(), equalTo("hi"));
    }

    @Test
    public void canMergeTraitConflictsWithBuiltShapes() {
        StringShape string1 = StringShape.builder()
                .id("smithy.example#String1")
                .addTrait(TagsTrait.builder().addValue("a").build())
                .build();
        ModelAssembler assembler = Model.assembler();
        assembler.addShape(string1);
        assembler.addUnparsedModel("foo.smithy",
                "$version: \"2.0\"\n"
                        + "namespace smithy.example\n\n"
                        + "@tags([\"b\"])\n"
                        + "string String1\n");
        Model result = assembler.assemble().unwrap();

        assertThat(result.expectShape(string1.getId()).getTags(), contains("a", "b"));
    }

    @Test
    public void providesDiffWhenConflictsAreFound() {
        String a = "$version: \"2\"\n"
                + "namespace foo.baz\n"
                + "integer Foo\n";
        String b = "$version: \"2\"\n"
                + "namespace foo.baz\n"
                + "@default(0)\n"
                + "long Foo\n";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel("1.smithy", a)
                .addUnparsedModel("2.smithy", b)
                .assemble();

        assertTrue(result.isBroken());
        assertThat(result.getValidationEvents().get(0).getMessage(), containsString("Left is long, right is integer"));
    }

    @Test
    public void canRoundTripShapesWithMixinsThroughAssembler() {
        StructureShape mixin = StructureShape.builder()
                .id("smithy.example#Mixin")
                .addTrait(MixinTrait.builder().build())
                .build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Foo")
                .addMixin(mixin)
                .addMember("foo", ShapeId.from("smithy.api#String"))
                .build();
        Model model = Model.assembler().addShapes(mixin, struct).assemble().unwrap();

        assertThat(model.expectShape(struct.getId()), equalTo(struct));
        assertThat(model.expectShape(mixin.getId()), equalTo(mixin));
    }

    @Test
    public void mixinShapesNoticeDependencyChanges() {
        StructureShape mixin = StructureShape.builder()
                .id("smithy.example#Mixin")
                .addTrait(MixinTrait.builder().build())
                .build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Foo")
                .addMixin(mixin)
                .addMember("foo", ShapeId.from("smithy.api#String"))
                .build();
        Model model = Model.assembler()
                .addShapes(mixin, struct)
                .addTrait(mixin.getId(), new SensitiveTrait())
                .assemble()
                .unwrap();

        assertThat(model.expectShape(mixin.getId()).getAllTraits(), hasKey(SensitiveTrait.ID));
        assertThat(model.expectShape(mixin.getId()).getIntroducedTraits(), hasKey(SensitiveTrait.ID));
        assertThat(model.expectShape(struct.getId()).getAllTraits(), hasKey(SensitiveTrait.ID));
        assertThat(model.expectShape(struct.getId()).getIntroducedTraits(), not(hasKey(SensitiveTrait.ID)));
    }

    @Test
    public void nodeModelsDoNotInterfereWithManuallyAddedModels() {
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Foo")
                .addMember("foo", ShapeId.from("smithy.api#Integer"))
                .build();
        // Create an object node with a source location of none.
        ObjectNode node = Node.objectNodeBuilder()
                .withMember(new StringNode("smithy", SourceLocation.NONE), new StringNode("1.0", SourceLocation.NONE))
                .build();
        Model model = Model.assembler()
                .addShape(struct)
                // Add a Node with a 1.0 version and a SourceLocation.none() value.
                // This source location is the same as the manually given shape, but it should not
                // cause the manually given shape to also think it's a 1.0 shape.
                .addDocumentNode(node)
                .assemble()
                .unwrap();

        // Ensure that the upgrade process did not add a Box trait to the manually created shape
        // because it is not assumed to be a 1.0 shape.
        ShapeId memberCheck = struct.getMember("foo").get().getId();
        MemberShape createdMember = model.expectShape(memberCheck, MemberShape.class);

        assertThat(createdMember.getAllTraits(), not(hasKey(BoxTrait.ID)));
    }

    @Test
    public void canResolveTargetsWithoutPrelude() {
        ValidatedResult<Model> model = Model.assembler()
                .disablePrelude()
                .addUnparsedModel("foo.smithy",
                        "$version: \"2.0\"\n"
                                + "namespace smithy.example\n"
                                + "list Foo { member: String }\n")
                .assemble();

        assertThat(model.getValidationEvents(), hasSize(1));
        assertThat(model.getValidationEvents().get(0).getMessage(), containsString("unresolved shape"));
    }

    @Test
    public void findsBoxTraitOnPreludeShapes() {
        Model model = Model.assembler().assemble().unwrap();

        assertThat(model.expectShape(ShapeId.from("smithy.api#Boolean")).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(ShapeId.from("smithy.api#Byte")).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(ShapeId.from("smithy.api#Short")).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(ShapeId.from("smithy.api#Integer")).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(ShapeId.from("smithy.api#Long")).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(ShapeId.from("smithy.api#Float")).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(ShapeId.from("smithy.api#Double")).hasTrait(BoxTrait.class), is(true));
    }

    @Test
    public void forwardReferencesAreOrdered() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("forward-references-are-ordered.smithy"))
                .assemble()
                .unwrap();

        ShapeId service = ShapeId.from("smithy.example#Example");
        assertThat(model.expectShape(service, ServiceShape.class).getErrors(),
                contains(ShapeId.from("smithy.example#Error1"),
                        ShapeId.from("smithy.example#Error2"),
                        ShapeId.from("smithy.example#Error3"),
                        ShapeId.from("smithy.example#Error4"),
                        ShapeId.from("smithy.example#Error5"),
                        ShapeId.from("smithy.example#Error6")));
    }

    @Test
    public void upgrades1_0_documentNodesToo() {
        // Loads fine through import.
        Model model1 = Model.assembler()
                .addImport(getClass().getResource("needs-upgrade-document-node.json"))
                .assemble()
                .unwrap();

        assertionChecksFor_upgradesAndDowngrades(model1);

        // And through unparsed.
        String contents = IoUtils.readUtf8Resource(getClass(), "needs-upgrade-document-node.json");
        Model model2 = Model.assembler()
                .addUnparsedModel("foo.json", contents)
                .assemble()
                .unwrap();

        assertionChecksFor_upgradesAndDowngrades(model2);

        // And through document node.
        Node node = Node.parse(contents);
        Model model3 = Model.assembler()
                .addDocumentNode(node)
                .assemble()
                .unwrap();

        assertionChecksFor_upgradesAndDowngrades(model3);

        // Pathological case.
        Model model4 = Model.assembler()
                .addModel(model1)
                .addModel(model2)
                .addModel(model3)
                .addDocumentNode(node)
                .addUnparsedModel("foo.json", contents)
                .assemble()
                .unwrap();

        assertionChecksFor_upgradesAndDowngrades(model4);

        assertThat(model1, equalTo(model2));
        assertThat(model1, equalTo(model3));
        assertThat(model1, equalTo(model4));
    }

    private void assertionChecksFor_upgradesAndDowngrades(Model model) {
        ShapeId boxDouble = ShapeId.from("smithy.example#BoxDouble");
        ShapeId primitiveDouble = ShapeId.from("smithy.example#PrimitiveDouble");
        ShapeId fooMember = ShapeId.from("smithy.example#Struct$foo");
        ShapeId barMember = ShapeId.from("smithy.example#Struct$bar");
        ShapeId boxedMember = ShapeId.from("smithy.example#Struct$boxed");
        assertThat(model.expectShape(boxDouble).hasTrait(DefaultTrait.class), is(false));
        assertThat(model.expectShape(primitiveDouble).hasTrait(DefaultTrait.class), is(true));
        assertThat(model.expectShape(fooMember).hasTrait(DefaultTrait.class), is(true));
        assertThat(model.expectShape(barMember).hasTrait(DefaultTrait.class), is(false));
        assertThat(model.expectShape(boxedMember).hasTrait(DefaultTrait.class), is(true));
        assertThat(model.expectShape(boxedMember).expectTrait(DefaultTrait.class).toNode(), equalTo(Node.nullNode()));
        assertThat(model.expectShape(boxedMember).hasTrait(BoxTrait.class), is(true));
    }

    @Test
    public void patches2_0_documentNodesToo() {
        // Loads fine through import.
        Model model1 = Model.assembler()
                .addImport(getClass().getResource("needs-downgrade-document-node.json"))
                .assemble()
                .unwrap();

        assertionChecksFor_upgradesAndDowngrades(model1);

        // And through unparsed.
        String contents = IoUtils.readUtf8Resource(getClass(), "needs-downgrade-document-node.json");
        Model model2 = Model.assembler()
                .addUnparsedModel("foo.json", contents)
                .assemble()
                .unwrap();

        assertionChecksFor_upgradesAndDowngrades(model2);

        // And through document node.
        Node node = Node.parse(contents);
        Model model3 = Model.assembler()
                .addDocumentNode(node)
                .assemble()
                .unwrap();

        assertionChecksFor_upgradesAndDowngrades(model3);

        // Pathological case.
        Model model4 = Model.assembler()
                .addModel(model1)
                .addModel(model2)
                .addModel(model3)
                .addDocumentNode(node)
                .addUnparsedModel("foo.json", contents)
                .assemble()
                .unwrap();

        assertionChecksFor_upgradesAndDowngrades(model4);

        assertThat(model1, equalTo(model2));
        assertThat(model1, equalTo(model3));
        assertThat(model1, equalTo(model4));
    }

    @Test
    public void syntheticBoxingResultsInSameModelBetween1and2() {
        Model model1 = Model.assembler()
                .addImport(getClass().getResource("synthetic-boxing-1.smithy"))
                .assemble()
                .unwrap();

        Model model2 = Model.assembler()
                .addImport(getClass().getResource("synthetic-boxing-2.smithy"))
                .assemble()
                .unwrap();

        Model model3 = Model.assembler()
                .addImport(getClass().getResource("synthetic-boxing-1.smithy"))
                .addImport(getClass().getResource("synthetic-boxing-2.smithy"))
                .addModel(model1)
                .addModel(model2)
                .assemble()
                .unwrap();

        assertThat(model1, equalTo(model2));
        assertThat(model1, equalTo(model3));
    }

    @Test
    public void appliesBoxTraitsToMixinsToo() {
        Model model1 = Model.assembler()
                .addImport(getClass().getResource("synthetic-boxing-mixins.smithy"))
                .assemble()
                .unwrap();

        // MixedInteger and MixinInteger have synthetic box traits.
        assertThat(model1.expectShape(ShapeId.from("smithy.example#MixinInteger")).hasTrait(BoxTrait.class), is(true));
        assertThat(model1.expectShape(ShapeId.from("smithy.example#MixedInteger")).hasTrait(BoxTrait.class), is(true));

        // MixinStruct$bar and MixedStruct$bar have synthetic box traits.
        StructureShape mixinStruct = model1.expectShape(ShapeId.from("smithy.example#MixinStruct"),
                StructureShape.class);
        StructureShape mixedStruct = model1.expectShape(ShapeId.from("smithy.example#MixedStruct"),
                StructureShape.class);
        assertThat(mixinStruct.getAllMembers().get("bar").hasTrait(BoxTrait.class), is(true));
        assertThat(mixinStruct.getAllMembers().get("bar").hasTrait(DefaultTrait.class), is(true));
        assertThat(mixedStruct.getAllMembers().get("bar").hasTrait(BoxTrait.class), is(true));
        assertThat(mixedStruct.getAllMembers().get("bar").hasTrait(DefaultTrait.class), is(true));

        // Now ensure round-tripping results in the same model.
        Node serialized = ModelSerializer.builder().build().serialize(model1);
        Model model2 = Model.assembler().addDocumentNode(serialized).assemble().unwrap();

        assertThat(model1, equalTo(model2));
    }

    @Test
    public void versionTransformsAreAlwaysApplied() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(getClass().getResource("invalid-1.0-model-upgraded.smithy"))
                .assemble();

        // Ensure that the invalid trait caused the model to have errors.
        assertThat(result.getValidationEvents(Severity.ERROR), not(empty()));

        // Ensure that the model was created.
        assertThat(result.getResult().isPresent(), is(true));

        Model model = result.getResult().get();

        // Ensure that the model upgrade transformations happened.
        Shape myInteger = model.expectShape(ShapeId.from("smithy.example#MyInteger"));
        Shape fooBaz = model.expectShape(ShapeId.from("smithy.example#Foo$baz"));
        Shape fooBam = model.expectShape(ShapeId.from("smithy.example#Foo$bam"));

        // These shapes have a default zero value so have a default trait for 2.0.
        assertThat(myInteger.getAllTraits(), hasKey(DefaultTrait.ID));
        assertThat(fooBaz.getAllTraits(), hasKey(DefaultTrait.ID));

        // These members are considered boxed in 1.0.
        assertThat(fooBam.getAllTraits(), hasKey(BoxTrait.ID));
        assertThat(fooBam.expectTrait(DefaultTrait.class).toNode(), equalTo(Node.nullNode()));
    }

    @Test
    public void ignoresUnrecognizedFileExtensions() throws URISyntaxException {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(Paths.get(getClass().getResource("assembler-ignore-unrecognized-files").toURI()))
                .assemble();

        assertThat(result.getValidationEvents(Severity.DANGER), empty());
        assertThat(result.getValidationEvents(Severity.ERROR), empty());

        result.unwrap().expectShape(ShapeId.from("smithy.example#MyString"));
    }

    @Test
    public void ignoresUnrecognizedJsonFiles() throws URISyntaxException {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(Paths.get(getClass().getResource("assembler-ignore-unrecognized-json").toURI()))
                .assemble();

        assertThat(result.getValidationEvents(Severity.DANGER), empty());
        assertThat(result.getValidationEvents(Severity.ERROR), empty());

        result.unwrap().expectShape(ShapeId.from("smithy.example#MyString"));
    }

    @Test
    public void failsOnInvalidJarJsonFile() throws URISyntaxException, IOException {
        Path jar = JarUtils.createJarFromDir(Paths.get(getClass().getResource("assembler-fail-invalid-jar").toURI()));

        ModelImportException e = Assertions.assertThrows(ModelImportException.class, () -> {
            Model.assembler().addImport(jar).assemble();
        });

        assertThat(e.getMessage(), containsString("Invalid file referenced by Smithy JAR manifest"));
    }

    @Test
    public void doesNotThrowOnInvalidSuppression() {
        ObjectNode node = Node.objectNode()
                .withMember("smithy", "1.0")
                .withMember("metadata", Node.objectNode().withMember("suppressions", "hi!"));

        ValidatedResult<Model> result = new ModelAssembler().addDocumentNode(node).assemble();

        assertThat(result.getValidationEvents(Severity.ERROR), not(empty()));
    }

    @Test
    public void modelLoadingErrorsAreEmittedToListener() {
        ObjectNode node = Node.objectNode().withMember("smithy", Node.fromStrings("Hi", "there"));
        List<ValidationEvent> events = new ArrayList<>();

        ValidatedResult<Model> result = new ModelAssembler()
                .addDocumentNode(node)
                .validationEventListener(events::add)
                .assemble();

        assertThat(result.getValidationEvents(Severity.ERROR), hasSize(1));
        assertThat(events, equalTo(result.getValidationEvents()));
    }

    @Test
    public void exceptionsThrownWhenCreatingTraitsDontCrashSmithy() {
        String document = "{\n"
                + "\"smithy\": \"" + Model.MODEL_VERSION + "\",\n"
                + "    \"shapes\": {\n"
                + "        \"ns.foo#Test\": {\n"
                + "            \"type\": \"string\",\n"
                + "            \"traits\": {\"smithy.foo#baz\": true}\n"
                + "        }\n"
                + "    }\n"
                + "}";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel(SourceLocation.NONE.getFilename(), document)
                .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                .traitFactory((traitId, target, value) -> {
                    throw new RuntimeException("Oops!");
                })
                .assemble();

        assertThat(result.getValidationEvents(Severity.ERROR), not(empty()));
        assertThat(result.getValidationEvents(Severity.ERROR).get(0).getMessage(),
                equalTo("Error creating trait `smithy.foo#baz`: Oops!"));
    }

    @Test
    public void resolvesDuplicateTraitApplicationsToDuplicateMixedInMembers() throws Exception {
        String model =
                IoUtils.readUtf8File(Paths.get(getClass().getResource("mixins/apply-to-mixed-member.json").toURI()));
        // Should be able to de-conflict the apply statements when the same model is loaded multiple times.
        // See https://github.com/smithy-lang/smithy/issues/2004
        Model.assembler()
                .addUnparsedModel("test.json", model)
                .addUnparsedModel("test2.json", model)
                .addUnparsedModel("test3.json", model)
                .assemble()
                .unwrap();
    }

    @Test
    public void resolvesDuplicateTraitApplicationsToSameMixedInMember() throws Exception {
        String modelToApplyTo =
                IoUtils.readUtf8File(Paths.get(getClass().getResource("mixins/mixed-member.smithy").toURI()));
        String modelWithApply = IoUtils
                .readUtf8File(Paths.get(getClass().getResource("mixins/member-apply-other-namespace.smithy").toURI()));
        // Should be able to load when you have multiple identical apply statements to the same mixed in member.
        // See https://github.com/smithy-lang/smithy/issues/2004
        Model.assembler()
                .addUnparsedModel("mixed-member.smithy", modelToApplyTo)
                .addUnparsedModel("member-apply-1.smithy", modelWithApply)
                .addUnparsedModel("member-apply-2.smithy", modelWithApply)
                .assemble()
                .unwrap();
    }

    @Test
    public void handlesMultipleInheritanceForMixinMembers() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("mixins/multiple-inheritance-with-introduction.smithy"))
                .assemble()
                .unwrap();

        MemberShape shape = model.expectShape(ShapeId.from("com.example#FinalStructure$member"), MemberShape.class);

        assertThat(shape.getMixins(),
                contains(
                        ShapeId.from("com.example#MixinA$member"),
                        ShapeId.from("com.example#MixinB$member")));
        assertThat(shape.getAllTraits().keySet(),
                containsInAnyOrder(PatternTrait.ID, RequiredTrait.ID, InternalTrait.ID));
        String actualPattern = shape.expectTrait(PatternTrait.class).getValue();
        assertThat(actualPattern, equalTo("baz"));
    }

    @Test
    public void loadsShapesWhenThereAreUnresolvedMixins() {
        String modelText = "$version: \"2\"\n"
                + "namespace com.foo\n"
                + "\n"
                + "string Foo\n"
                + "@mixin\n"
                + "structure Bar {}\n"
                + "structure Baz with [Unknown] {}\n";
        ValidatedResult<Model> result = Model.assembler()
                .addUnparsedModel("foo.smithy", modelText)
                .assemble();

        assertThat(result.isBroken(), is(true));
        assertThat(result.getResult().isPresent(), is(true));
        Set<ShapeId> fooShapes = result.getResult()
                .get()
                .getShapeIds()
                .stream()
                .filter(id -> id.getNamespace().equals("com.foo"))
                .collect(Collectors.toSet());
        assertThat(fooShapes,
                containsInAnyOrder(
                        ShapeId.from("com.foo#Foo"),
                        ShapeId.from("com.foo#Bar")));
    }

    @Test
    public void handlesSameModelWhenBuiltAndImported() throws Exception {
        Path modelUri = Paths.get(getClass().getResource("mixin-and-apply-model.smithy").toURI());
        Model sourceModel = Model.assembler()
                .addImport(modelUri)
                .assemble()
                .unwrap();
        Model combinedModel = Model.assembler()
                .addModel(sourceModel)
                .addImport(modelUri)
                .assemble()
                .unwrap();

        assertTrue(combinedModel.expectShape(ShapeId.from("smithy.example#MachineData$machineId"), MemberShape.class)
                .hasTrait(RequiredTrait.ID));
    }
}
