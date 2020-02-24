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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.ReferencesTrait;
import software.amazon.smithy.model.traits.TagsTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidatedResultException;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.MapUtils;

public class LoaderVisitorTest {
    private static final TraitFactory FACTORY = TraitFactory.createServiceFactory();

    @Test
    public void callingOnEndTwiceIsIdempotent() {
        LoaderVisitor visitor = new LoaderVisitor(FACTORY);

        assertThat(visitor.onEnd(), is(visitor.onEnd()));
    }

    @Test
    public void cannotDuplicateTraitDefs() {
        LoaderVisitor visitor = new LoaderVisitor(FACTORY);
        StringShape def1 = StringShape.builder()
                .id("foo.baz#Bar")
                .addTrait(TraitDefinition.builder().build())
                .build();
        StringShape def2 = StringShape.builder()
                .id("foo.baz#Bar")
                .addTrait(TraitDefinition.builder().selector(Selector.parse("string")).build())
                .build();

        visitor.onShape(def1);
        visitor.onShape(def2);
        List<ValidationEvent> events = visitor.onEnd().getValidationEvents();

        assertThat(events, not(empty()));
    }

    @Test
    public void ignoresDuplicateTraitDefsFromPrelude() {
        LoaderVisitor visitor = new LoaderVisitor(FACTORY);
        Shape def1 = StructureShape.builder()
                .id("smithy.api#deprecated")
                .addTrait(TraitDefinition.builder().build())
                .build();
        Shape def2 = StructureShape.builder()
                .id("smithy.api#deprecated")
                .addTrait(TraitDefinition.builder().build())
                .build();

        visitor.onShape(def1);
        visitor.onShape(def2);
        List<ValidationEvent> events = visitor.onEnd().getValidationEvents();

        assertThat(events, empty());
    }

    @Test
    public void cannotDuplicateNonPreludeTraitDefs() {
        LoaderVisitor visitor = new LoaderVisitor(FACTORY);
        Shape def1 = StructureShape.builder()
                .id("smithy.example#deprecated")
                .addTrait(TraitDefinition.builder().build())
                .build();
        Shape def2 = StructureShape.builder()
                .id("smithy.example#deprecated")
                .addTrait(TraitDefinition.builder().build())
                .build();

        visitor.onShape(def1);
        visitor.onShape(def2);
        List<ValidationEvent> events = visitor.onEnd().getValidationEvents();

        assertThat(events, not(empty()));
    }

    @Test
    public void cannotDuplicateTraits() {
        LoaderVisitor visitor = new LoaderVisitor(FACTORY);
        ShapeId id = ShapeId.from("foo.bam#Boo");
        visitor.onShape(StringShape.builder().id(id));
        visitor.onTrait(id, DocumentationTrait.ID, Node.from("abc"));
        visitor.onTrait(id, DocumentationTrait.ID, Node.from("def"));
        List<ValidationEvent> events = visitor.onEnd().getValidationEvents();

        assertThat(events, not(empty()));
    }

    @Test
    public void createsDynamicTraitWhenTraitFactoryReturnsEmpty() {
        LoaderVisitor visitor = new LoaderVisitor(FACTORY);
        Shape def = StructureShape.builder()
                .id("foo.baz#Bar")
                .addTrait(TraitDefinition.builder().build())
                .build();
        visitor.onShape(def);
        ShapeId id = ShapeId.from("foo.bam#Boo");
        visitor.onShape(StringShape.builder().id(id));
        visitor.onTrait(id, ShapeId.from("foo.baz#Bar"), Node.from(true));
        Model model = visitor.onEnd().unwrap();

        assertThat(model.expectShape(id).findTrait("foo.baz#Bar").get(),
                   instanceOf(DynamicTrait.class));
    }

    @Test
    public void failsWhenTraitNotFound() {
        LoaderVisitor visitor = new LoaderVisitor(FACTORY);
        ShapeId id = ShapeId.from("foo.bam#Boo");
        visitor.onShape(StringShape.builder().id(id));
        visitor.onTrait(id, ShapeId.from("foo.baz#Bar"), Node.from(true));
        List<ValidationEvent> events = visitor.onEnd().getValidationEvents();

        assertThat(events, not(empty()));
    }

    @Test
    public void coercesNullTraitValues() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("null-coerce-traits.json"))
                .assemble()
                .unwrap();

        Shape shape = model.expectShape(ShapeId.from("ns.foo#Foo"));
        assertTrue(shape.getTrait(DeprecatedTrait.class).isPresent());
        assertTrue(shape.getTrait(TagsTrait.class).isPresent());
        assertTrue(shape.getTrait(ReferencesTrait.class).isPresent());
    }

    @Test
    public void coercesBooleanToStructureTraitValues() {
        Model model = Model.assembler()
                .addUnparsedModel("test.smithy", "namespace smithy.example\n"
                                                 + "@foo(true)\n"
                                                 + "string MyString\n"
                                                 + "@trait(selector: \"*\")\n"
                                                 + "structure foo {}\n")
                .assemble()
                .unwrap();
        Shape shape = model.expectShape(ShapeId.from("smithy.example#MyString"));

        assertTrue(shape.hasTrait("smithy.example#foo"));
    }

    private static Model createCoercionModel(String traitType) {
        return Model.assembler()
                .addUnparsedModel("test.smithy", "namespace smithy.example\n"
                                                 + "@foo\n"
                                                 + "string MyString\n"
                                                 + "@trait(selector: \"*\")"
                                                 + traitType + "\n")
                .assemble()
                .unwrap();
    }

    @Test
    public void coercesListTraitValues() {
        Model model = createCoercionModel("list foo { member: String }");
        Shape shape = model.expectShape(ShapeId.from("smithy.example#MyString"));

        assertTrue(shape.hasTrait("smithy.example#foo"));
    }

    @Test
    public void coercesBooleanTraitValuesToStructures() {
        Model model = createCoercionModel("structure foo {}");
        Shape shape = model.expectShape(ShapeId.from("smithy.example#MyString"));

        assertTrue(shape.hasTrait("smithy.example#foo"));
    }

    @Test
    public void supportsCustomProperties() {
        Map<String, Object> properties = MapUtils.of("a", true, "b", new HashMap<>());
        LoaderVisitor visitor = new LoaderVisitor(TraitFactory.createServiceFactory(), properties);

        assertThat(visitor.getProperty("a").get(), equalTo(true));
        assertThat(visitor.getProperty("b").get(), equalTo(new HashMap<>()));
        assertThat(visitor.getProperty("a", Boolean.class).get(), equalTo(true));
        assertThat(visitor.getProperty("b", Map.class).get(), equalTo(new HashMap<>()));
    }

    @Test
    public void assertsCorrectPropertyType() {
        Assertions.assertThrows(ClassCastException.class, () -> {
            Map<String, Object> properties = MapUtils.of("a", true);
            LoaderVisitor visitor = new LoaderVisitor(TraitFactory.createServiceFactory(), properties);

            visitor.getProperty("a", Integer.class).get();
        });
    }

    @Test
    public void cannotAddMemberToNonExistentShape() {
        Assertions.assertThrows(ValidatedResultException.class, () -> {
            LoaderVisitor visitor = new LoaderVisitor(FACTORY);
            visitor.onShape(MemberShape.builder().id("foo.baz#Bar$bam").target("foo.baz#Bam"));

            visitor.onEnd().unwrap();
        });
    }

    @Test
    public void errorWhenShapesConflict() {
        LoaderVisitor visitor = new LoaderVisitor(FACTORY);
        Shape shape = StringShape.builder().id("smithy.foo#Baz").build();
        visitor.onShape(shape);
        visitor.onShape(shape);
        ValidatedResult<Model> result = visitor.onEnd();

        assertThat(result.getValidationEvents(), not(empty()));
    }

    @Test
    public void ignoresDuplicateFiles() {
        URL file = getClass().getResource("valid/trait-definitions.smithy");
        Model model = Model.assembler().addImport(file).assemble().unwrap();
        Model.assembler().addModel(model).addImport(file).assemble().unwrap();
    }
}
