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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.InternalTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.SuppressTrait;
import software.amazon.smithy.model.traits.synthetic.OriginalShapeIdTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.model.validation.ValidatorFactory;
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
    public void addsExplicitDocumentNode_1_0_0() {
        ObjectNode node = Node.objectNode()
                .withMember("smithy", "1.0")
                .withMember("shapes", Node.objectNode()
                        .withMember("ns.foo#String", Node.objectNode()
                                .withMember("type", Node.from("string"))));
        ValidatedResult<Model> result = new ModelAssembler().addDocumentNode(node).assemble();

        assertThat(result.getValidationEvents(), empty());
        assertTrue(result.unwrap().getShape(ShapeId.from("ns.foo#String")).isPresent());
    }

    @Test
    public void addsExplicitUnparsedDocumentNode() {
        String document = "{\"smithy\": \"" + Model.MODEL_VERSION + "\", \"shapes\": { \"ns.foo#String\": { \"type\": \"string\"}}}";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel(SourceLocation.NONE.getFilename(), document)
                .assemble();

        assertThat(result.getValidationEvents(), empty());
        assertTrue(result.unwrap().getShape(ShapeId.from("ns.foo#String")).isPresent());
    }

    @Test
    public void addsExplicitValidators() {
        ValidationEvent event = ValidationEvent.builder()
                .severity(Severity.ERROR).id("Foo").message("bar").build();
        String document = "{\"smithy\": \"" + Model.MODEL_VERSION + "\"}";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel(SourceLocation.NONE.getFilename(), document)
                .addValidator(index -> Collections.singletonList(event))
                .assemble();

        assertThat(result.getValidationEvents(), contains(event));
    }

    @Test
    public void detectsTraitsOnUnknownShape() {
        String document = "{\"smithy\": \"" + Model.MODEL_VERSION + "\", \"shapes\": {\"ns.foo#Unknown\": {\"type\": \"apply\", \"traits\": {\"smithy.api#documentation\": \"foo\"}}}}";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel(SourceLocation.NONE.getFilename(), document)
                .assemble();

        assertThat(result.getValidationEvents(), hasSize(1));
        assertThat(result.getValidationEvents().get(0).getMessage(), containsString(
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
        assertTrue(result.getResult().get()
                           .getShape(ShapeId.from("example.namespace#String")).isPresent());
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
            .getShape(stringId).get()
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
                Paths.get(getClass().getResource("nested/merges-2.json").toURI()), "symlink-merges-2.json"))
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
                .getShape(stringId).get()
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
                .addUnparsedModel("N/A", "{\"smithy\": \"" + Model.MODEL_VERSION + "\", \"shapes\": {\"example.namespace#String\": {\"type\": \"apply\", \"traits\": {\"smithy.api#documentation\": \"hi\"}}}}")
                .assemble()
                .unwrap();

        assertEquals("hi", model2.expectShape(ShapeId.from("example.namespace#String"))
                .getTrait(DocumentationTrait.class).get().getValue());
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
}
