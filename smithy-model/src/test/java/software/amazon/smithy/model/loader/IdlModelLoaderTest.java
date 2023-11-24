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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.ExternalDocumentationTrait;
import software.amazon.smithy.model.traits.StringTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidatedResultException;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

public class IdlModelLoaderTest {
    @Test
    public void loadsAppropriateSourceLocations() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("valid/main-test.smithy"))
                .assemble()
                .unwrap();

        model.shapes().forEach(shape -> {
            if (!Prelude.isPreludeShape(shape.getId())) {
                assertThat(shape.getSourceLocation(), not(equalTo(SourceLocation.NONE)));
            }

            // Non-member shapes defined in the main-test.smithy file should
            // all have a source location column of 1. The endsWith check is
            // necessary to filter out the prelude.
            if (shape.getSourceLocation().getFilename().endsWith("main-test.smithy") && !shape.isMemberShape()) {
                assertThat(shape.getSourceLocation().getColumn(), equalTo(1));
            }
        });
    }

    @Test
    public void loadsAppropriateTraitSourceLocations() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("valid/trait-locations.smithy"))
                .assemble()
                .unwrap();

        Shape shape = model.expectShape(ShapeId.from("com.example#TraitBearer"));

        for (Trait trait : shape.getAllTraits().values()) {
            assertThat(trait.getSourceLocation().getColumn(), equalTo(1));
        }

        for (MemberShape member : shape.members()) {
            for (Trait trait : member.getAllTraits().values()) {
                assertThat(trait.getSourceLocation().getColumn(), equalTo(5));
            }
        }
    }

    @Test
    public void fallsBackToPublicPreludeShapes() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("valid/forward-reference-resolver.smithy"))
                .assemble()
                .unwrap();

        MemberShape baz = model.expectShape(ShapeId.from("smithy.example#Foo$baz"))
                .asMemberShape().get();
        MemberShape bar = model.expectShape(ShapeId.from("smithy.example#Foo$bar"))
                .asMemberShape().get();
        ResourceShape resource = model.expectShape(ShapeId.from("smithy.example#MyResource"))
                .asResourceShape().get();

        assertThat(baz.getTarget().toString(), equalTo("smithy.api#String"));
        assertThat(bar.getTarget().toString(), equalTo("smithy.example#Integer"));
        assertThat(resource.getIdentifiers().get("a"), equalTo(ShapeId.from("smithy.example#MyString")));
        assertThat(resource.getIdentifiers().get("b"), equalTo(ShapeId.from("smithy.example#AnotherString")));
        assertThat(resource.getIdentifiers().get("c"), equalTo(ShapeId.from("smithy.api#String")));
    }

    @Test
    public void canLoadAndAliasShapesAndTraits() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("first-namespace.smithy"))
                .addImport(getClass().getResource("second-namespace.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void defersApplyTargetAndTrait() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("apply-use-1.smithy"))
                .addImport(getClass().getResource("apply-use-2.smithy"))
                .addImport(getClass().getResource("apply-use-3.smithy"))
                .assemble()
                .unwrap();

        Shape shape = model.expectShape(ShapeId.from("smithy.example#Foo"));

        assertThat(shape.findTrait(ShapeId.from("smithy.example#bar")), not(Optional.empty()));
        assertThat(shape.findTrait(ShapeId.from("smithy.example.b#baz")), not(Optional.empty()));
    }

    @Test
    public void limitsRecursion() {
        StringBuilder nodeBuilder = new StringBuilder("metadata foo = ");
        for (int i = 0; i < 251; i++) {
            nodeBuilder.append('[');
        }
        nodeBuilder.append("true");
        for (int i = 0; i < 251; i++) {
            nodeBuilder.append(']');
        }
        nodeBuilder.append("\n");

        ValidatedResultException e = Assertions.assertThrows(ValidatedResultException.class, () -> {
            Model.assembler().addUnparsedModel("/foo.smithy", nodeBuilder.toString()).assemble().unwrap();
        });

        assertThat(e.getMessage(), containsString("Parser exceeded maximum allowed depth of"));
    }

    @Test
    public void handlesMultilineDocComments() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("multiline-comments.smithy"))
                .assemble()
                .unwrap();

        Shape shape = model.expectShape(ShapeId.from("smithy.example#MyStruct$myMember"));
        String docs = shape.getTrait(DocumentationTrait.class)
                .map(StringTrait::getValue)
                .orElse("")
                .replace("\r\n", "\n");

        assertThat(docs, equalTo("This is the first line.\nThis is the second line."));
    }

    @Test
    public void warnsWhenInvalidSyntacticShapeIdIsFound() {
        ValidatedResult<Model> result = Model.assembler()
                .addUnparsedModel("foo.smithy", "$version: \"2.0\"\n"
                                                + "namespace smithy.example\n"
                                                + "@tags([nonono])\n"
                                                + "string Foo\n")
                .assemble();

        assertThat(result.isBroken(), is(true));
        List<ValidationEvent> events = result.getValidationEvents(Severity.DANGER);
        assertThat(events.stream().filter(e -> e.getId().equals("SyntacticShapeIdTarget")).count(), equalTo(1L));
        assertThat(events.stream()
                           .filter(e -> e.getId().equals("SyntacticShapeIdTarget"))
                           .filter(e -> e.getMessage().contains("`nonono`"))
                           .count(),
                   equalTo(1L));
    }

    @Test
    public void properlyLoadsOperationsWithUseStatements() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("use-operations/service.smithy"))
                .addImport(getClass().getResource("use-operations/nested.smithy"))
                .addImport(getClass().getResource("use-operations/other.smithy"))
                .assemble()
                .unwrap();

        // Spot check for a specific "use" shape.
        assertThat(model.expectShape(ShapeId.from("smithy.example#Local"), OperationShape.class).getInput(),
                   equalTo(Optional.of(ShapeId.from("smithy.example.nested#A"))));

        assertThat(model.expectShape(ShapeId.from("smithy.example#Local"), OperationShape.class).getErrors(),
                   equalTo(ListUtils.of(ShapeId.from("smithy.example.nested#C"))));

        Map<String, ShapeId> identifiers = model.expectShape(
                ShapeId.from("smithy.example.nested#Resource"),
                ResourceShape.class
        ).getIdentifiers();

        assertThat(identifiers.get("s"), equalTo(ShapeId.from("smithy.api#String")));
        assertThat(identifiers.get("x"), equalTo(ShapeId.from("smithy.example.other#X")));
    }

    @Test
    public void addressesConfusingEdgeCaseForUnknownTraits() {
        TraitFactory defaultFactory = TraitFactory.createServiceFactory();

        // This trait factory ensures that the trait under test is coerced to
        // an object instead of a null. The definition of @foo can't be found,
        // but we know it's an annotation trait, so it's coerced to the most
        // common annotation trait type (an object). This makes the error
        // message for annotation traits that have no trait definition but do
        // have TraitService SPI more intelligible.
        TraitFactory factory = (id, target, value) -> {
            if (id.equals(ShapeId.from("smithy.example#foo"))) {
                assertThat("Did not coerce the unknown annotation trait to an object", value.isObjectNode(), is(true));
                return Optional.of(new DynamicTrait(id, value));
            } else {
                return defaultFactory.createTrait(id, target, value);
            }
        };

        List<ValidationEvent> events = Model.assembler()
                .traitFactory(factory)
                .addUnparsedModel("foo.smithy", "namespace smithy.example\n@foo\nstring MyString\n")
                .assemble()
                .getValidationEvents();

        // Ensure that there is also an event that the @foo trait couldn't be found.
        // This is a bit overkill, but ensures that it works end-to-end.
        for (ValidationEvent event : events) {
            if (event.getShapeId().filter(id -> id.equals(ShapeId.from("smithy.example#MyString"))).isPresent()) {
                if (event.getMessage().contains("Unable to resolve trait")) {
                    // Just break out of the test early when it's found. We're done.
                    return;
                }
            }
        }

        Assertions.fail("Did not find expected unknown trait event: " + events);
    }

    @Test
    public void loadsServiceRenames() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("valid/__shared.json"))
                .addImport(getClass().getResource("valid/service-with-rename.smithy"))
                .assemble()
                .unwrap();

        // Spot check for a specific "use" shape.
        assertThat(model.expectShape(ShapeId.from("smithy.example#MyService"), ServiceShape.class).getRename(),
                   equalTo(MapUtils.of(ShapeId.from("foo.example#Widget"), "FooWidget")));
    }

    @Test
    public void loadsServicesWithNonconflictingUnitTypes() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("valid/__shared.json"))
                .addImport(getClass().getResource("valid/service-with-nonconflicting-unit.smithy"))
                .assemble()
                .unwrap();

        // Make sure we can find our Unit type
        assertThat(model.expectShape(ShapeId.from("smithy.example#Unit")), Matchers.notNullValue());
    }

    @Test
    public void emitsVersionWhenNotSet() {
        List<LoadOperation> operations = new ArrayList<>();
        IdlModelLoader parser = new IdlModelLoader("foo.smithy", "namespace smithy.example\n", CharSequence::toString);
        parser.parse(operations::add);

        assertThat(operations, hasSize(1));
        assertThat(operations.get(0), instanceOf(LoadOperation.ModelVersion.class));
    }

    @Test
    public void defaultValueSugaringDoesNotEatSubsequentDocumentation() {
        Model model = Model.assembler()
            .addImport(getClass().getResource("default-subsequent-trait.smithy"))
            .assemble()
            .unwrap();

        StructureShape testShape = model.expectShape(ShapeId.from("smithy.example#TestShape"), StructureShape.class);
        MemberShape barMember = testShape.getMember("bar").orElseThrow(AssertionFailedError::new);

        assertEquals("bar", barMember.expectTrait(DocumentationTrait.class).getValue());
        assertEquals(3, barMember.getAllTraits().size());

        MemberShape bazMember = testShape.getMember("baz").orElseThrow(AssertionFailedError::new);

        assertEquals("baz", bazMember.expectTrait(DocumentationTrait.class).getValue());
        assertEquals(2, bazMember.getAllTraits().size());

        StringShape stringShape = model.expectShape(ShapeId.from("smithy.example#MyString"), StringShape.class);
        assertEquals(0, stringShape.getAllTraits().size());
    }

    @Test
    public void setsCorrectLocationForEnum() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("valid/enums/enums.smithy"))
                .addImport(getClass().getResource("valid/enums/enum-docs.smithy"))
                .assemble()
                .unwrap();

        EnumShape enumWithoutValueTraits = model.expectShape(ShapeId.from("smithy.example#EnumWithoutValueTraits"),
                                                             EnumShape.class);
        MemberShape barMember = enumWithoutValueTraits.getMember("BAR").orElseThrow(AssertionFailedError::new);

        assertThat(enumWithoutValueTraits.getSourceLocation().getLine(), is(5));
        assertThat(enumWithoutValueTraits.getSourceLocation().getColumn(), is(1));
        assertThat(barMember.getSourceLocation().getLine(), is(7));
        assertThat(barMember.getSourceLocation().getColumn(), is(5));

        EnumShape foo = model.expectShape(ShapeId.from("smithy.example#Foo"), EnumShape.class);
        MemberShape fooBarMember = foo.getMember("BAR").orElseThrow(AssertionFailedError::new);

        assertThat(foo.getSourceLocation().getLine(), is(5));
        assertThat(foo.getSourceLocation().getColumn(), is(1));
        assertThat(fooBarMember.getSourceLocation().getLine(), is(7));
        assertThat(fooBarMember.getSourceLocation().getColumn(), is(5));
    }

    @Test
    public void doesBasicErrorRecoveryToTrait() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(getClass().getResource("error-recovery/to-trait.smithy"))
                .assemble();

        assertThat(result.isBroken(), is(true));
        assertThat(result.getResult().isPresent(), is(true));

        Model model = result.getResult().get();

        assertThat(model.getShape(ShapeId.from("smithy.example#MyString")).isPresent(), is(true));
        assertThat(model.getShape(ShapeId.from("smithy.example#MyFooIsBroken")).isPresent(), is(false));
        assertThat(model.getShape(ShapeId.from("smithy.example#MyInteger")).isPresent(), is(false));
        assertThat(model.getShape(ShapeId.from("smithy.example#MyInteger2")).isPresent(), is(true));

        boolean foundSyntax = false;
        boolean foundTrait = false;
        for (ValidationEvent e : result.getValidationEvents()) {
            if (e.getSeverity() == Severity.ERROR && e.getMessage().contains(
                    "Syntax error at line 9, column 9: Expected COLON(':') but found IDENTIFIER('MyInteger')")) {
                foundSyntax = true;
            }
            if (e.getSeverity() == Severity.ERROR && e.getMessage().contains("Unable to resolve trait")) {
                foundTrait = true;
            }
        }

        assertThat(foundSyntax, is(true));
        assertThat(foundTrait, is(true));
    }

    @Test
    public void doesBasicErrorRecoveryToDocs() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(getClass().getResource("error-recovery/to-docs.smithy"))
                .assemble();

        assertThat(result.isBroken(), is(true));
        assertThat(result.getResult().isPresent(), is(true));

        Model model = result.getResult().get();

        ShapeId myInteger = ShapeId.from("smithy.example#MyInteger");
        assertThat(model.getShape(myInteger).isPresent(), is(true));
        // Ensure recovery happened on the docs trait, capturing it on the shape.
        assertThat(model.expectShape(myInteger).hasTrait(DocumentationTrait.class), is(true));

        boolean foundSyntax = false;
        boolean foundTrait = false;
        for (ValidationEvent e : result.getValidationEvents()) {
            if (e.getSeverity() == Severity.ERROR && e.getMessage().contains("Syntax error at line 6, column 28")) {
                foundSyntax = true;
            }
            if (e.getSeverity() == Severity.ERROR && e.getMessage().contains("Unable to resolve trait")) {
                foundTrait = true;
            }
        }

        assertThat(foundSyntax, is(true));
        assertThat(foundTrait, is(true));
    }

    @Test
    public void doesBasicErrorRecoveryToIdentifier() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(getClass().getResource("error-recovery/to-identifier.smithy"))
                .assemble();

        assertThat(result.isBroken(), is(true));
        assertThat(result.getResult().isPresent(), is(true));

        Model model = result.getResult().get();

        ShapeId myString = ShapeId.from("smithy.example#MyString");
        assertThat(model.getShape(myString).isPresent(), is(true));
        assertThat(model.expectShape(myString).hasTrait(ExternalDocumentationTrait.class), is(false));

        boolean foundSyntax = result.getValidationEvents().stream()
                .anyMatch(e -> e.getSeverity() == Severity.ERROR
                               && e.getMessage().contains("Syntax error at line 6, column 28"));

        assertThat(foundSyntax, is(true));
    }

    @Test
    public void doesBasicErrorRecoveryToControl() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(getClass().getResource("error-recovery/to-dollar.smithy"))
                .assemble();

        assertThat(result.isBroken(), is(true));
        assertThat(result.getResult().isPresent(), is(true));

        Model model = result.getResult().get();

        assertThat(model.getShape(ShapeId.from("smithy.example#MyInteger")).isPresent(), is(true));

        boolean foundSyntax = result.getValidationEvents().stream()
                .anyMatch(e -> e.getSeverity() == Severity.ERROR
                               && e.getMessage().contains("Syntax error at line 1, column 5"));

        assertThat(foundSyntax, is(true));
    }

    @Test
    public void doesBasicErrorRecoveryInMetadata() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(getClass().getResource("error-recovery/in-metadata.smithy"))
                .assemble();

        assertThat(result.isBroken(), is(true));
        assertThat(result.getResult().isPresent(), is(true));

        Model model = result.getResult().get();

        // Ensure the value keys were parsed and invalid keys were not.
        assertThat(model.getMetadata().keySet(), containsInAnyOrder("valid1", "valid2", "valid3", "valid4"));
        assertThat(model.getMetadata().get("valid1"), equalTo(Node.from("ok")));
        assertThat(model.getMetadata().get("valid2"), equalTo(Node.from("ok")));
        assertThat(model.getMetadata().get("valid3"), equalTo(Node.from("ok")));
        assertThat(model.getMetadata().get("valid4"), equalTo(Node.from("ok")));

        // Find all four invalid metadata keys.
        long foundSyntax = result.getValidationEvents().stream()
                .filter(e -> e.getSeverity() == Severity.ERROR && e.getMessage().contains("Syntax error"))
                .count();

        assertThat(foundSyntax, equalTo(4L));
    }

    @Test
    public void throwsWhenTooNested() {
        IdlModelLoader loader = new IdlModelLoader("foo.smithy", "", new StringTable());

        for (int i = 0; i < 64; i++) {
            loader.increaseNestingLevel();
        }

        ModelSyntaxException e = Assertions.assertThrows(ModelSyntaxException.class, loader::increaseNestingLevel);

        assertThat(e.getMessage(),
                   startsWith("Syntax error at line 1, column 1: Parser exceeded maximum allowed depth of 64"));
    }
}
