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
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.DeprecatedTrait;
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
    public void versionMustBeConsistent() {
        Assertions.assertThrows(SourceException.class, () -> {
            LoaderVisitor visitor = new LoaderVisitor(FACTORY);
            visitor.onVersion(SourceLocation.NONE, Model.MODEL_VERSION);
            visitor.onVersion(SourceLocation.NONE, "1.1");
        });
    }

    @Test
    public void versionMustBeSupported() {
        Assertions.assertThrows(SourceException.class, () -> {
            LoaderVisitor visitor = new LoaderVisitor(FACTORY);
            visitor.onVersion(SourceLocation.NONE, "9.99");
        });
    }

    @Test
    public void versionDefaults() {
        LoaderVisitor visitor = new LoaderVisitor(FACTORY);
        visitor.onMetadata("foo", Node.from("bar"));
    }

    @Test
    public void acceptableVersionDifference() {
        LoaderVisitor visitor = new LoaderVisitor(FACTORY);
        visitor.onVersion(SourceLocation.NONE, "0.1.0");
        visitor.onVersion(SourceLocation.NONE, "0.1.1");
        visitor.onVersion(SourceLocation.NONE, "0.1.3");
    }

    @Test
    public void cannotMutateAfterOnEnd() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            LoaderVisitor visitor = new LoaderVisitor(FACTORY);
            visitor.onVersion(SourceLocation.NONE, Model.MODEL_VERSION);
            visitor.onEnd();
            visitor.onMetadata("foo", Node.from("bar"));
        });
    }

    @Test
    public void cannotCallOnEndTwice() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            LoaderVisitor visitor = new LoaderVisitor(FACTORY);
            visitor.onVersion(SourceLocation.NONE, Model.MODEL_VERSION);
            visitor.onEnd();
            visitor.onEnd();
        });
    }

    @Test
    public void cannotDuplicateTraitDefs() {
        LoaderVisitor visitor = new LoaderVisitor(FACTORY);
        visitor.onVersion(SourceLocation.NONE, Model.MODEL_VERSION);
        TraitDefinition.Builder def1 = TraitDefinition.builder()
                .name("foo.baz#Bar")
                .selector(Selector.IDENTITY);
        TraitDefinition.Builder def2 = TraitDefinition.builder()
                .name("foo.baz#Bar")
                .selector(Selector.parse("string"));
        visitor.onTraitDef(def1);
        visitor.onTraitDef(def2);
        List<ValidationEvent> events = visitor.onEnd().getValidationEvents();

        assertThat(events, not(empty()));
    }

    @Test
    public void ignoresDuplicateTraitDefsFromPrelude() {
        LoaderVisitor visitor = new LoaderVisitor(FACTORY);
        visitor.onVersion(SourceLocation.NONE, Model.MODEL_VERSION);
        TraitDefinition.Builder def = TraitDefinition.builder().name("smithy.api#deprecated")
                .selector(Selector.IDENTITY);
        visitor.onTraitDef(def);
        visitor.onTraitDef(def);
        List<ValidationEvent> events = visitor.onEnd().getValidationEvents();

        assertThat(events, empty());
    }

    @Test
    public void cannotDuplicateNonPreludeTraitDefs() {
        LoaderVisitor visitor = new LoaderVisitor(FACTORY);
        visitor.onVersion(SourceLocation.NONE, Model.MODEL_VERSION);
        TraitDefinition.Builder def = TraitDefinition.builder().name("smithy.example#deprecated")
                .selector(Selector.IDENTITY);
        visitor.onTraitDef(def);
        visitor.onTraitDef(def);
        List<ValidationEvent> events = visitor.onEnd().getValidationEvents();

        assertThat(events, not(empty()));
    }

    @Test
    public void cannotDuplicateTraits() {
        LoaderVisitor visitor = new LoaderVisitor(FACTORY);
        visitor.onOpenFile("/foo/baz");
        visitor.onVersion(SourceLocation.NONE, Model.MODEL_VERSION);
        visitor.onNamespace("foo.bam", SourceLocation.NONE);
        ShapeId id = ShapeId.from("foo.bam#Boo");
        visitor.onShape(StringShape.builder().id(id));
        visitor.onTrait(id, "documentation", Node.from("abc"));
        visitor.onTrait(id, "documentation", Node.from("def"));
        List<ValidationEvent> events = visitor.onEnd().getValidationEvents();

        assertThat(events, not(empty()));
    }

    @Test
    public void createsModeledTraitWhenTraitFactoryReturnsEmpty() {
        LoaderVisitor visitor = new LoaderVisitor(FACTORY);
        visitor.onOpenFile("/foo/baz.smithy");
        visitor.onVersion(SourceLocation.NONE, Model.MODEL_VERSION);
        visitor.onNamespace("foo.bam", SourceLocation.none());
        TraitDefinition.Builder def = TraitDefinition.builder().name("foo.baz#Bar").selector(Selector.IDENTITY);
        visitor.onTraitDef(def);
        ShapeId id = ShapeId.from("foo.bam#Boo");
        visitor.onShape(StringShape.builder().id(id));
        visitor.onTrait(id, "foo.baz#Bar", Node.from(true));
        Model model = visitor.onEnd().unwrap();

        assertThat(model.getShapeIndex().getShape(id).get().findTrait("foo.baz#Bar").get(),
                   instanceOf(DynamicTrait.class));
    }

    @Test
    public void failsWhenTraitNotFound() {
        LoaderVisitor visitor = new LoaderVisitor(FACTORY);
        visitor.onOpenFile("/foo/baz.smithy");
        visitor.onVersion(SourceLocation.NONE, Model.MODEL_VERSION);
        visitor.onNamespace("foo.bam", SourceLocation.none());
        ShapeId id = ShapeId.from("foo.bam#Boo");
        visitor.onShape(StringShape.builder().id(id));
        visitor.onTrait(id, "foo.baz#Bar", Node.from(true));
        List<ValidationEvent> events = visitor.onEnd().getValidationEvents();

        assertThat(events, not(empty()));
    }

    @Test
    public void coercesNullTraitValues() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("null-coerce-traits.json"))
                .assemble()
                .unwrap();

        Shape shape = model.getShapeIndex().getShape(ShapeId.from("ns.foo#Foo")).get();
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
                                                 + "trait foo { shape: TraitValue, selector: '*'}\n"
                                                 + "structure TraitValue {}\n")
                .assemble()
                .unwrap();
        Shape shape = model.getShapeIndex().getShape(ShapeId.from("smithy.example#MyString")).get();

        assertTrue(shape.hasTrait("smithy.example#foo"));
    }

    private static Model createCoercionModel(String traitType) {
        return Model.assembler()
                .addUnparsedModel("test.smithy", "namespace smithy.example\n"
                                                 + "@foo\n"
                                                 + "string MyString\n"
                                                 + "trait foo { shape: TraitValue, selector: '*'}\n"
                                                 + traitType + "\n")
                .assemble()
                .unwrap();
    }

    @Test
    public void coercesListTraitValues() {
        Model model = createCoercionModel("list TraitValue { member: String }");
        Shape shape = model.getShapeIndex().getShape(ShapeId.from("smithy.example#MyString")).get();

        assertTrue(shape.hasTrait("smithy.example#foo"));
    }

    @Test
    public void coercesBooleanTraitValuesToStructures() {
        Model model = createCoercionModel("structure TraitValue {}");
        Shape shape = model.getShapeIndex().getShape(ShapeId.from("smithy.example#MyString")).get();

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
