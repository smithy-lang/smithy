/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static software.amazon.smithy.utils.FunctionalUtils.alwaysTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.traits.TraitDefinition;

public class DeconflictingStrategyTest {
    @Test
    public void canDeconflictNamesWhereListsAreActuallyDifferent() {
        StringShape str = StringShape.builder().id("com.bar#String").build();
        MemberShape memberA = MemberShape.builder().id("com.bar#Page$member").target("com.bar#String").build();
        ListShape a = ListShape.builder().id("com.bar#Page").member(memberA).build();
        IntegerShape integer = IntegerShape.builder().id("com.foo#Int").build();
        MemberShape memberB = MemberShape.builder().id("com.foo#Page$member").target("com.foo#Int").build();
        ListShape b = ListShape.builder().id("com.foo#Page").member(memberB).build();
        Model model = Model.builder().addShapes(str, integer, a, b, memberA, memberB).build();

        PropertyNamingStrategy propertyNamingStrategy = PropertyNamingStrategy.createDefaultStrategy();
        RefStrategy strategy = RefStrategy
                .createDefaultStrategy(model, new JsonSchemaConfig(), propertyNamingStrategy, alwaysTrue());
        assertThat(strategy.toPointer(a.getId()), equalTo("#/definitions/Page"));
        assertThat(strategy.toPointer(b.getId()), equalTo("#/definitions/PageComFoo"));
    }

    @Test
    public void detectsUnsupportedConflicts() {
        StructureShape a = StructureShape.builder().id("com.bar#Page").build();
        StructureShape b = StructureShape.builder().id("com.foo#Page").build();
        Model model = Model.builder().addShapes(a, b).build();
        PropertyNamingStrategy propertyNamingStrategy = PropertyNamingStrategy.createDefaultStrategy();

        Assertions.assertThrows(ConflictingShapeNameException.class, () -> {
            RefStrategy.createDefaultStrategy(model, new JsonSchemaConfig(), propertyNamingStrategy, alwaysTrue());
        });
    }

    @Test
    public void deconflictingStrategyPassesThroughToDelegate() {
        Model model = Model.builder().build();
        PropertyNamingStrategy propertyNamingStrategy = PropertyNamingStrategy.createDefaultStrategy();
        RefStrategy strategy = RefStrategy
                .createDefaultStrategy(model, new JsonSchemaConfig(), propertyNamingStrategy, alwaysTrue());

        assertThat(strategy.toPointer(ShapeId.from("com.foo#Nope")), equalTo("#/definitions/Nope"));
    }

    @Test
    public void detectsUnitConflictsWhenPreludeUnitIsNotFiltered() {
        StructureShape a = StructureShape.builder().id("com.foo#Unit").build();
        Model model = Model.assembler().addShapes(a).assemble().unwrap();
        PropertyNamingStrategy propertyNamingStrategy = PropertyNamingStrategy.createDefaultStrategy();

        Assertions.assertThrows(ConflictingShapeNameException.class, () -> {
            RefStrategy.createDefaultStrategy(model, new JsonSchemaConfig(), propertyNamingStrategy, alwaysTrue());
        });
    }

    @Test
    public void doesNotDetectUnitConflictsWhenPreludeUnitIsFiltered() {
        StructureShape a = StructureShape.builder().id("com.foo#Unit").build();
        Model model = Model.assembler().addShapes(a).assemble().unwrap();
        PropertyNamingStrategy propertyNamingStrategy = PropertyNamingStrategy.createDefaultStrategy();

        RefStrategy.createDefaultStrategy(model,
                new JsonSchemaConfig(),
                propertyNamingStrategy,
                new JsonSchemaConverter.FilterPreludeUnit(false));
    }

    @Test
    public void detectsUnitConflictsWithNonPreludeUnitsNoMatterWhat() {
        StructureShape a = StructureShape.builder().id("com.foo#Unit").build();
        StructureShape b = StructureShape.builder().id("com.bar#Unit").build();
        Model model = Model.assembler().addShapes(a, b).assemble().unwrap();
        PropertyNamingStrategy propertyNamingStrategy = PropertyNamingStrategy.createDefaultStrategy();

        Assertions.assertThrows(ConflictingShapeNameException.class, () -> {
            RefStrategy.createDefaultStrategy(model,
                    new JsonSchemaConfig(),
                    propertyNamingStrategy,
                    new JsonSchemaConverter.FilterPreludeUnit(false));
        });
    }

    @Test
    public void excludesPrivatePreludeShapes() {
        StructureShape a = StructureShape.builder().id("com.foo#Severity").build();
        Model model = Model.assembler().addShapes(a).assemble().unwrap();
        PropertyNamingStrategy propertyNamingStrategy = PropertyNamingStrategy.createDefaultStrategy();
        RefStrategy strategy = RefStrategy
                .createDefaultStrategy(model, new JsonSchemaConfig(), propertyNamingStrategy, alwaysTrue());
        assertThat(strategy.toPointer(a.getId()), equalTo("#/definitions/Severity"));
    }

    @Test
    public void excludesTraitDefinitions() {
        StringShape member = StringShape.builder().id("com.foo#String").build();
        StructureShape matcher = StructureShape.builder()
                .id("com.foo#Matcher")
                .addMember("member", member.getId())
                .build();
        StructureShape matcherForTrait = StructureShape.builder()
                .id("com.bar#Matcher")
                .addTrait(new PrivateTrait())
                .build();
        StructureShape trait = StructureShape.builder()
                .id("com.bar#Trait")
                .addTrait(TraitDefinition.builder().build())
                .addMember("matcher", matcherForTrait.toShapeId())
                .build();
        Model model = Model.assembler().addShapes(trait, matcherForTrait, matcher, member).assemble().unwrap();
        PropertyNamingStrategy propertyNamingStrategy = PropertyNamingStrategy.createDefaultStrategy();
        RefStrategy strategy = RefStrategy
                .createDefaultStrategy(model, new JsonSchemaConfig(), propertyNamingStrategy, alwaysTrue());
        assertThat(strategy.toPointer(matcher.getId()), equalTo("#/definitions/Matcher"));
    }
}
