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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.Suppression;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.model.validation.ValidatorFactory;
import software.amazon.smithy.utils.ListUtils;

public class ModelAssemblerTest {

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
        assertThat(result.unwrap().getShapeIndex().getShape(ShapeId.from("ns.foo#Bar")), is(Optional.of(shape)));
    }

    @Test
    public void addsExplicitDocumentNode() {
        ObjectNode node = Node.objectNode()
                .withMember("smithy", Node.from("1.0"))
                .withMember("ns.foo", Node.objectNode()
                        .withMember("shapes", Node.objectNode()
                                .withMember("String", Node.objectNode()
                                        .withMember("type", Node.from("string")))));
        ValidatedResult<Model> result = new ModelAssembler().addDocumentNode(node).assemble();

        assertThat(result.getValidationEvents(), empty());
        assertTrue(result.unwrap().getShapeIndex().getShape(ShapeId.from("ns.foo#String")).isPresent());
    }

    @Test
    public void addsExplicitUnparsedDocumentNode() {
        String document = "{\"smithy\": \"1.0\", \"ns.foo\": { \"shapes\": { \"String\": { \"type\": \"string\"}}}}";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel(SourceLocation.NONE.getFilename(), document)
                .assemble();

        assertThat(result.getValidationEvents(), empty());
        assertTrue(result.unwrap().getShapeIndex().getShape(ShapeId.from("ns.foo#String")).isPresent());
    }

    @Test
    public void addsExplicitValidators() {
        ValidationEvent event = ValidationEvent.builder()
                .severity(Severity.ERROR).eventId("Foo").message("bar").build();
        String document = "{\"smithy\": \"1.0\"}";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel(SourceLocation.NONE.getFilename(), document)
                .addValidator(index -> Collections.singletonList(event))
                .assemble();

        assertThat(result.getValidationEvents(), contains(event));
    }

    @Test
    public void detectsTraitsOnUnknownShape() {
        String document = "{\"smithy\": \"1.0\", \"ns.foo\": {\"traits\": {\"Unknown\": {\"documentation\": \"foo\"}}}}";
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
        assertTrue(result.getResult().get().getShapeIndex()
                           .getShape(ShapeId.from("example.namespace#String")).isPresent());
    }

    @Test
    public void detectsUnresolvedImports() {
        Assertions.assertThrows(RuntimeException.class, () -> new ModelAssembler().addImport("/bad/path"));
    }

    @Test
    public void importsFilesWithAllShapes() throws Exception {
        ValidatedResult<Model> result = new ModelAssembler()
                .addImport(getClass().getResource("main.json"))
                .addImport(Paths.get(getClass().getResource("nested").toURI()))
                .assemble();

        assertThat(result.getValidationEvents(), empty());
        Model model = result.unwrap();
        assertTrue(model.getShapeIndex().getShape(ShapeId.from("example.namespace#String")).isPresent());
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#String")).get().getType(),
                   is(ShapeType.STRING));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#String2")).get().getType(),
                   is(ShapeType.STRING));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#String3")).get().getType(),
                   is(ShapeType.STRING));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#String")).get().getType(),
                   is(ShapeType.STRING));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#Integer")).get().getType(),
                   is(ShapeType.INTEGER));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#Long")).get().getType(),
                   is(ShapeType.LONG));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#Float")).get().getType(),
                   is(ShapeType.FLOAT));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#BigDecimal")).get().getType(),
                   is(ShapeType.BIG_DECIMAL));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#BigInteger")).get().getType(),
                   is(ShapeType.BIG_INTEGER));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#Blob")).get().getType(),
                   is(ShapeType.BLOB));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#Boolean")).get().getType(),
                   is(ShapeType.BOOLEAN));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#Timestamp")).get().getType(),
                   is(ShapeType.TIMESTAMP));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#List")).get().getType(),
                    is(ShapeType.LIST));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#Map")).get().getType(),
                   is(ShapeType.MAP));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#Structure")).get().getType(),
                   is(ShapeType.STRUCTURE));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#TaggedUnion")).get().getType(),
                   is(ShapeType.UNION));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#Resource")).get().getType(),
                   is(ShapeType.RESOURCE));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#Operation")).get().getType(),
                   is(ShapeType.OPERATION));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#Service")).get().getType(),
                   is(ShapeType.SERVICE));

        ShapeId stringId = ShapeId.from("example.namespace#String");
        Optional<SensitiveTrait> sensitiveTrait = model.getShapeIndex()
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
        assertThat(model.getMetadata().get("list"), equalTo(Node.fromStrings("a", "b", "c")));

        // The String shape should have a documentation trait applied.
        assertTrue(model.getShapeIndex().getShape(ShapeId.from("example.namespace#String"))
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

        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#String")).get().getSourceLocation(),
                is(new SourceLocation(getClass().getResource("main.json").toString(), 13, 23)));
        assertThat(model.getShapeIndex().getShape(ShapeId.from("example.namespace#TaggedUnion$foo")).get().getSourceLocation(),
                is(new SourceLocation(getClass().getResource("main.json").toString(), 76, 28)));
    }

    @Test
    public void supportsMultiNamespaceDocuments() {
        ValidatedResult<Model> result = new ModelAssembler()
                .addImport(getClass().getResource("multiple-namespaces.json"))
                .disablePrelude()
                .assemble();

        assertThat(result.getValidationEvents(), empty());
        // Each namespace had a separate name.
        assertThat(result.unwrap().getShapeIndex().shapes().count(), equalTo(3L));
        // Each shape had a documentation trait in each namespace.
        assertThat(result.unwrap().getShapeIndex().shapes()
                           .filter(shape -> shape.findTrait("ns.shared#customTrait").isPresent())
                           .count(), equalTo(3L));
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
        assertTrue(model.getShapeIndex().getShape(ShapeId.from("example.namespace#String"))
                           .flatMap(shape -> shape.getTrait(DocumentationTrait.class))
                           .isPresent());
        assertTrue(model.getShapeIndex().getShape(ShapeId.from("example.namespace#String"))
                           .flatMap(shape -> shape.getTrait(MediaTypeTrait.class))
                           .isPresent());
    }

    @Test
    public void canAddEntireModelsToAssembler() {
        Model model = new ModelAssembler().addImport(getClass().getResource("main.json")).assemble().unwrap();
        Model model2 = Model.assembler()
                .addModel(model)
                .addUnparsedModel("N/A", "{\"smithy\": \"1.0\", \"example.namespace\": {\"traits\": {\"String\": {\"documentation\": \"hi\"}}}}")
                .assemble()
                .unwrap();

        assertEquals("hi", model2.getShapeIndex().getShape(ShapeId.from("example.namespace#String")).get()
                .getTrait(DocumentationTrait.class).get().getValue());
    }

    @Test
    public void canAddExplicitTraitDefs() {
        TraitDefinition def = TraitDefinition.builder()
                .name("foo.baz#bar")
                .selector(Selector.IDENTITY)
                .build();
        Model model = new ModelAssembler().addTraitDefinition(def).assemble().unwrap();

        assertThat(model.getTraitDefinition("foo.baz#bar"), equalTo(Optional.of(def)));
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
        String document = "{\"smithy\": \"1.0\", \"ns.foo\": { \"shapes\": { \"String\": { \"type\": \"string\", \"invalidTrait\": true}}}}";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel(SourceLocation.NONE.getFilename(), document)
                .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                .assemble();

        assertThat(result.getValidationEvents(), not(empty()));
        assertFalse(result.isBroken());
    }
}
