/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocol.traits;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;

public class IdxTraitTest {

    private static final ShapeId TARGET = ShapeId.from("smithy.example#Example$member");

    @Test
    public void loadsAndRoundTripsTrait() {
        SourceLocation location = new SourceLocation("example.smithy", 10, 4);
        Node node = new NumberNode(42, location);
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(IdxTrait.ID, TARGET, node);

        Assertions.assertTrue(trait.isPresent());
        Assertions.assertInstanceOf(IdxTrait.class, trait.get());
        IdxTrait idx = (IdxTrait) trait.get();
        Assertions.assertEquals(42, idx.getValue());
        Assertions.assertEquals(location, idx.getSourceLocation());
        Assertions.assertSame(node, idx.toNode());
    }

    @Test
    public void roundTripsConstructedTrait() {
        SourceLocation location = new SourceLocation("example.smithy", 2, 8);
        IdxTrait trait = new IdxTrait(7, location);

        Assertions.assertEquals(Node.from(7), trait.toNode());
        Assertions.assertEquals(location, trait.toNode().getSourceLocation());
    }

    @Test
    public void rejectsNonIntegralValues() {
        IdxTrait.Provider provider = new IdxTrait.Provider();

        Assertions.assertThrows(
                ExpectationNotMetException.class,
                () -> provider.createTrait(TARGET, Node.from(new BigDecimal("1.5"))));
    }

    public static Stream<Integer> validIndexesNearIntegerMax() {
        return Stream.of(2147483644, 2147483645, 2147483646, 2147483647);
    }

    @ParameterizedTest
    @MethodSource("validIndexesNearIntegerMax")
    public void acceptsIndexesThroughIntegerMax(int index) {
        IdxTrait trait = new IdxTrait.Provider().createTrait(TARGET, Node.from(index));

        Assertions.assertEquals(index, trait.getValue());
    }

    public static Stream<Long> indexesAboveIntegerMax() {
        return Stream.of(2147483648L, 2147483649L, 2147483650L);
    }

    @ParameterizedTest
    @MethodSource("indexesAboveIntegerMax")
    public void rejectsConsecutiveIndexesAboveIntegerMax(long index) {
        Assertions.assertThrows(
                ExpectationNotMetException.class,
                () -> new IdxTrait.Provider().createTrait(TARGET, Node.from(index)));
    }

    public static Stream<Integer> indexesBelowMinimum() {
        return Stream.of(0, -1, Integer.MIN_VALUE);
    }

    @ParameterizedTest
    @MethodSource("indexesBelowMinimum")
    public void rejectsIndexesBelowOne(int index) {
        ValidatedResult<Model> result = Model.assembler()
                .discoverModels()
                .addUnparsedModel("invalid-index.smithy",
                        "$version: \"2.0\"\n"
                                + "namespace smithy.example\n"
                                + "use smithy.protocols#idx\n"
                                + "structure Example {\n"
                                + "    @idx(" + index + ")\n"
                                + "    member: String\n"
                                + "}\n")
                .assemble();

        Assertions.assertTrue(result.isBroken());
        Assertions.assertTrue(result.getValidationEvents(Severity.ERROR)
                .stream()
                .anyMatch(event -> event.getShapeId().filter(TARGET::equals).isPresent()
                        && event.getMessage().contains("must be greater than or equal to 1")));
    }

    @Test
    public void memberIndexSyntaxLoadsConcreteTrait() {
        Model model = Model.assembler()
                .discoverModels()
                .addUnparsedModel("indexed.smithy",
                        "$version: \"2.1\"\n"
                                + "namespace smithy.example\n"
                                + "structure Indexed {\n"
                                + "    1. value: String\n"
                                + "}\n")
                .assemble()
                .unwrap();

        IdxTrait trait = model.expectShape(ShapeId.from("smithy.example#Indexed$value"))
                .expectTrait(IdxTrait.class);
        Assertions.assertEquals(1, trait.getValue());
        Assertions.assertEquals("indexed.smithy", trait.getSourceLocation().getFilename());
        Assertions.assertEquals(4, trait.getSourceLocation().getLine());
        Assertions.assertEquals(5, trait.getSourceLocation().getColumn());
    }
}
