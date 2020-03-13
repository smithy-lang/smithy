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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.Suppression;
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
    public void addsExplicitSuppressions() {
        Suppression suppression = Suppression.builder().addValidatorId("foo").build();
        ValidatedResult<Model> result = new ModelAssembler()
                .addSuppression(suppression)
                .assemble();

        assertThat(result.getValidationEvents(), empty());
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
    public void addsExplicitDocumentNode_0_5_0() {
        ObjectNode node = Node.objectNode()
                .withMember("smithy", "0.5.0")
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
                .severity(Severity.ERROR).eventId("Foo").message("bar").build();
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
        assertTrue(result.getResult().get().getShape(ShapeId.from("example.namespace#String")).isPresent());
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
        assertThat(result.getValidationEvents(), empty());
        Model model = result.unwrap();
        assertTrue(model.getShape(ShapeId.from("example.namespace#String")).isPresent());
        assertThat(model.expectShape(ShapeId.from("example.namespace#String")).getType(),
            is(ShapeType.STRING));
        assertThat(model.expectShape(ShapeId.from("example.namespace#String2")).getType(),
            is(ShapeType.STRING));
        assertThat(model.expectShape(ShapeId.from("example.namespace#String3")).getType(),
            is(ShapeType.STRING));
        assertThat(model.expectShape(ShapeId.from("example.namespace#String")).getType(),
            is(ShapeType.STRING));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Integer")).getType(),
            is(ShapeType.INTEGER));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Long")).getType(),
            is(ShapeType.LONG));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Float")).getType(),
            is(ShapeType.FLOAT));
        assertThat(model.expectShape(ShapeId.from("example.namespace#BigDecimal")).getType(),
            is(ShapeType.BIG_DECIMAL));
        assertThat(model.expectShape(ShapeId.from("example.namespace#BigInteger")).getType(),
            is(ShapeType.BIG_INTEGER));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Blob")).getType(),
            is(ShapeType.BLOB));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Boolean")).getType(),
            is(ShapeType.BOOLEAN));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Timestamp")).getType(),
            is(ShapeType.TIMESTAMP));
        assertThat(model.expectShape(ShapeId.from("example.namespace#List")).getType(),
            is(ShapeType.LIST));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Map")).getType(),
            is(ShapeType.MAP));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Structure")).getType(),
            is(ShapeType.STRUCTURE));
        assertThat(model.expectShape(ShapeId.from("example.namespace#TaggedUnion")).getType(),
            is(ShapeType.UNION));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Resource")).getType(),
            is(ShapeType.RESOURCE));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Operation")).getType(),
            is(ShapeType.OPERATION));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Service")).getType(),
            is(ShapeType.SERVICE));

        ShapeId stringId = ShapeId.from("example.namespace#String");
        Optional<SensitiveTrait> sensitiveTrait = model
            .expectShape(stringId)
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
        return Files.createSymbolicLink(link, target);
    }

    @Test
    public void importsFilesWithAllShapes() throws Exception {
        ValidatedResult<Model> result = new ModelAssembler()
                .addImport(getClass().getResource("main.json"))
                .addImport(Paths.get(getClass().getResource("nested").toURI()))
                .assemble();

        assertThat(result.getValidationEvents(), empty());
        Model model = result.unwrap();
        assertTrue(model.getShape(ShapeId.from("example.namespace#String")).isPresent());
        assertThat(model.expectShape(ShapeId.from("example.namespace#String")).getType(),
                   is(ShapeType.STRING));
        assertThat(model.expectShape(ShapeId.from("example.namespace#String2")).getType(),
                   is(ShapeType.STRING));
        assertThat(model.expectShape(ShapeId.from("example.namespace#String3")).getType(),
                   is(ShapeType.STRING));
        assertThat(model.expectShape(ShapeId.from("example.namespace#String")).getType(),
                   is(ShapeType.STRING));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Integer")).getType(),
                   is(ShapeType.INTEGER));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Long")).getType(),
                   is(ShapeType.LONG));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Float")).getType(),
                   is(ShapeType.FLOAT));
        assertThat(model.expectShape(ShapeId.from("example.namespace#BigDecimal")).getType(),
                   is(ShapeType.BIG_DECIMAL));
        assertThat(model.expectShape(ShapeId.from("example.namespace#BigInteger")).getType(),
                   is(ShapeType.BIG_INTEGER));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Blob")).getType(),
                   is(ShapeType.BLOB));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Boolean")).getType(),
                   is(ShapeType.BOOLEAN));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Timestamp")).getType(),
                   is(ShapeType.TIMESTAMP));
        assertThat(model.expectShape(ShapeId.from("example.namespace#List")).getType(),
                    is(ShapeType.LIST));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Map")).getType(),
                   is(ShapeType.MAP));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Structure")).getType(),
                   is(ShapeType.STRUCTURE));
        assertThat(model.expectShape(ShapeId.from("example.namespace#TaggedUnion")).getType(),
                   is(ShapeType.UNION));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Resource")).getType(),
                   is(ShapeType.RESOURCE));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Operation")).getType(),
                   is(ShapeType.OPERATION));
        assertThat(model.expectShape(ShapeId.from("example.namespace#Service")).getType(),
                   is(ShapeType.SERVICE));

        ShapeId stringId = ShapeId.from("example.namespace#String");
        Optional<SensitiveTrait> sensitiveTrait = model
                .expectShape(stringId)
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

        assertThat(model.expectShape(ShapeId.from("example.namespace#String")).getSourceLocation(),
                is(new SourceLocation(getClass().getResource("main.json").toString(), 4, 37)));
        assertThat(model.expectShape(ShapeId.from("example.namespace#TaggedUnion$foo")).getSourceLocation(),
                is(new SourceLocation(getClass().getResource("main.json").toString(), 69, 24)));
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
                .expectTrait(DocumentationTrait.class).getValue());
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

        System.out.println(result.getValidationEvents());
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
            assertThat(model.expectShape(shapeId).getSourceLocation().getFilename(),
                       startsWith("jar:file:"));
        }
    }

    @Test
    public void gracefullyParsesPartialDocuments() {
        String document = "namespace foo.baz\n"
                          + "@required\n" // < this trait is invalid, but that's not validated due to the syntax error
                          + "string MyString\n"
                          + "str"; // < syntax error here
        ValidatedResult<Model> result = new ModelAssembler().addUnparsedModel("foo.smithy", document).assemble();

        assertTrue(result.isBroken());
        assertThat(result.getValidationEvents(Severity.ERROR), hasSize(1));
        assertTrue(result.getResult().isPresent());
        assertTrue(result.getResult().get().getShape(ShapeId.from("foo.baz#MyString")).isPresent());
    }
}
