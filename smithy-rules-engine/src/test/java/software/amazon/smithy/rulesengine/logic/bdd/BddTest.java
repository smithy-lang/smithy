/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.logic.TestHelpers;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;

class BddTest {

    @Test
    void testConstructorValidation() {
        // Should reject complemented root (except -1 which is FALSE terminal)
        assertThrows(IllegalArgumentException.class,
                () -> new Bdd(-2, 0, 0, 1, consumer -> consumer.accept(-1, 1, -1)));

        // Should accept positive root
        Bdd bdd = new Bdd(1, 0, 0, 1, consumer -> consumer.accept(-1, 1, -1));
        assertEquals(1, bdd.getRootRef());

        // Should accept FALSE terminal as root
        Bdd bdd2 = new Bdd(-1, 0, 0, 1, consumer -> consumer.accept(-1, 1, -1));
        assertEquals(-1, bdd2.getRootRef());
    }

    @Test
    void testBasicAccessors() {
        Bdd bdd = new Bdd(2, 2, 1, 3, consumer -> {
            consumer.accept(-1, 1, -1); // node 0: terminal
            consumer.accept(0, 3, -1); // node 1: var 0, high 3, low -1
            consumer.accept(1, 1, -1); // node 2: var 1, high 1, low -1
        });

        assertEquals(2, bdd.getConditionCount());
        assertEquals(1, bdd.getResultCount());
        assertEquals(3, bdd.getNodeCount());
        assertEquals(2, bdd.getRootRef());

        // Test node accessors
        assertEquals(-1, bdd.getVariable(0));
        assertEquals(1, bdd.getHigh(0));
        assertEquals(-1, bdd.getLow(0));

        assertEquals(0, bdd.getVariable(1));
        assertEquals(3, bdd.getHigh(1));
        assertEquals(-1, bdd.getLow(1));
    }

    @Test
    void testFromRuleSet() {
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder()
                        .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                        .build())
                .addRule(EndpointRule.builder()
                        .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                        .endpoint(TestHelpers.endpoint("https://example.com")))
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        Bdd bdd = new BddCompiler(cfg, ConditionOrderingStrategy.defaultOrdering(), new BddBuilder()).compile();

        assertTrue(bdd.getConditionCount() > 0);
        assertTrue(bdd.getResultCount() > 0);
        assertTrue(bdd.getNodeCount() > 1); // At least terminal + one node
    }

    @Test
    void testFromCfg() {
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder().build())
                .addRule(ErrorRule.builder().error("test error"))
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        Bdd bdd = new BddCompiler(cfg, ConditionOrderingStrategy.defaultOrdering(), new BddBuilder()).compile();

        assertEquals(0, bdd.getConditionCount()); // No conditions
        assertTrue(bdd.getResultCount() > 0);
    }

    @Test
    void testToString() {
        Bdd bdd = createSimpleBdd();
        String str = bdd.toString();

        assertTrue(str.contains("Bdd {"));
        assertTrue(str.contains("conditions:"));
        assertTrue(str.contains("results:"));
        assertTrue(str.contains("root:"));
        assertTrue(str.contains("nodes"));
    }

    @Test
    void testToStringWithDifferentNodeTypes() {
        // BDD structure referencing the correct indices
        Bdd bdd = new Bdd(2, 2, 3, 3, consumer -> {
            consumer.accept(-1, 1, -1); // node 0: terminal
            consumer.accept(0, 2, -1); // node 1: if Region is set, go to node 2, else FALSE
            consumer.accept(1, Bdd.RESULT_OFFSET + 2, Bdd.RESULT_OFFSET + 1); // node 2: if UseFips, return result 2, else result 1
        });

        String str = bdd.toString();

        assertTrue(str.contains("conditions: 2"));
        assertTrue(str.contains("results: 3"));
        assertTrue(str.contains("C0"));
        assertTrue(str.contains("C1"));
        assertTrue(str.contains("R1"));
        assertTrue(str.contains("R2"));
    }

    @Test
    void testReferenceHelperMethods() {
        // Test isNodeReference
        assertTrue(Bdd.isNodeReference(2));
        assertTrue(Bdd.isNodeReference(-2));
        assertFalse(Bdd.isNodeReference(0));
        assertFalse(Bdd.isNodeReference(1));
        assertFalse(Bdd.isNodeReference(-1));
        assertFalse(Bdd.isNodeReference(Bdd.RESULT_OFFSET));

        // Test isResultReference
        assertTrue(Bdd.isResultReference(Bdd.RESULT_OFFSET));
        assertTrue(Bdd.isResultReference(Bdd.RESULT_OFFSET + 1));
        assertFalse(Bdd.isResultReference(1));
        assertFalse(Bdd.isResultReference(-1));

        // Test isTerminal
        assertTrue(Bdd.isTerminal(1));
        assertTrue(Bdd.isTerminal(-1));
        assertFalse(Bdd.isTerminal(2));
        assertFalse(Bdd.isTerminal(Bdd.RESULT_OFFSET));

        // Test isComplemented
        assertTrue(Bdd.isComplemented(-2));
        assertTrue(Bdd.isComplemented(-3));
        assertFalse(Bdd.isComplemented(-1)); // FALSE terminal is not considered complemented
        assertFalse(Bdd.isComplemented(1));
        assertFalse(Bdd.isComplemented(2));
    }

    private Bdd createSimpleBdd() {
        return new Bdd(2, 1, 2, 2, consumer -> {
            consumer.accept(-1, 1, -1); // node 0: terminal
            consumer.accept(0, Bdd.RESULT_OFFSET + 1, -1); // node 1: if cond true, return result 1, else FALSE
        });
    }

    @Test
    void testStreamingConstructorValidation() {
        // Valid construction
        assertDoesNotThrow(() -> {
            new Bdd(1, 1, 1, 1, consumer -> {
                consumer.accept(-1, 1, -1);
            });
        });

        // Root cannot be complemented (except -1)
        assertThrows(IllegalArgumentException.class, () -> {
            new Bdd(-2, 1, 1, 1, consumer -> {
                consumer.accept(-1, 1, -1);
            });
        });

        // Root -1 (FALSE) is allowed
        assertDoesNotThrow(() -> {
            new Bdd(-1, 1, 1, 1, consumer -> {
                consumer.accept(-1, 1, -1);
            });
        });

        // Wrong node count
        assertThrows(IllegalStateException.class, () -> {
            new Bdd(1, 1, 1, 2, consumer -> {
                consumer.accept(-1, 1, -1); // Only provides 1 node, but claims 2
            });
        });
    }

    @Test
    void testArrayConstructorValidation() {
        int[] nodes = {-1, 1, -1};

        // Valid construction
        assertDoesNotThrow(() -> {
            new Bdd(1, 1, 1, 1, nodes);
        });

        // Wrong array length (not multiple of 3)
        int[] wrongLength = {-1, 1, -1, 0}; // 4 elements, not divisible by 3
        assertThrows(IllegalArgumentException.class, () -> {
            new Bdd(1, 1, 1, 1, wrongLength);
        });

        // Array length doesn't match nodeCount
        assertThrows(IllegalArgumentException.class, () -> {
            new Bdd(1, 1, 1, 2, nodes); // nodeCount=2 but array has 3 elements (1 node)
        });

        // Root cannot be complemented (except -1)
        assertThrows(IllegalArgumentException.class, () -> {
            new Bdd(-2, 1, 1, 1, nodes);
        });

        // Root -1 (FALSE) is allowed
        assertDoesNotThrow(() -> {
            new Bdd(-1, 1, 1, 1, nodes);
        });
    }

    @Test
    void testGetterBoundsChecking() {
        Bdd bdd = new Bdd(1, 1, 1, 2, consumer -> {
            consumer.accept(-1, 1, -1);
            consumer.accept(0, 1, -1);
        });

        // Valid indices
        assertDoesNotThrow(() -> bdd.getVariable(0));
        assertDoesNotThrow(() -> bdd.getVariable(1));

        // Out of bounds
        assertThrows(IndexOutOfBoundsException.class, () -> bdd.getVariable(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bdd.getVariable(2));

        // Same for high/low
        assertThrows(IndexOutOfBoundsException.class, () -> bdd.getHigh(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bdd.getHigh(2));
        assertThrows(IndexOutOfBoundsException.class, () -> bdd.getLow(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bdd.getLow(2));
    }

    @Test
    void testEquals() {
        // Create two identical BDDs
        Bdd bdd1 = new Bdd(2, 1, 1, 2, consumer -> {
            consumer.accept(-1, 1, -1);
            consumer.accept(0, 1, -1);
        });

        Bdd bdd2 = new Bdd(2, 1, 1, 2, consumer -> {
            consumer.accept(-1, 1, -1);
            consumer.accept(0, 1, -1);
        });

        // Same content should be equal
        assertEquals(bdd1, bdd2);
        assertEquals(bdd1.hashCode(), bdd2.hashCode());

        // Self equality
        assertEquals(bdd1, bdd1);

        // Different root ref (use TRUE terminal)
        Bdd bdd3 = new Bdd(1, 1, 1, 2, consumer -> {
            consumer.accept(-1, 1, -1);
            consumer.accept(0, 1, -1);
        });
        assertNotEquals(bdd1, bdd3);

        // Different root ref (use FALSE terminal)
        Bdd bdd4 = new Bdd(-1, 1, 1, 2, consumer -> {
            consumer.accept(-1, 1, -1);
            consumer.accept(0, 1, -1);
        });
        assertNotEquals(bdd1, bdd4);

        // Different node count
        Bdd bdd5 = new Bdd(2, 1, 1, 3, consumer -> {
            consumer.accept(-1, 1, -1);
            consumer.accept(0, 1, -1);
            consumer.accept(0, -1, 1);
        });
        assertNotEquals(bdd1, bdd5);

        // Different node content
        Bdd bdd6 = new Bdd(2, 1, 1, 2, consumer -> {
            consumer.accept(-1, 1, -1);
            consumer.accept(0, -1, 1); // Different high/low
        });
        assertNotEquals(bdd1, bdd6);

        // Different root ref (use result reference)
        Bdd bdd7 = new Bdd(Bdd.RESULT_OFFSET + 0, 1, 1, 2, consumer -> {
            consumer.accept(-1, 1, -1);
            consumer.accept(0, 1, -1);
        });
        assertNotEquals(bdd1, bdd7);

        // Null and different type
        assertNotEquals(bdd1, null);
        assertNotEquals(bdd1, "not a BDD");
    }

    // Used to regenerate BDD test cases for errorfiles
    //    @Test
    //    void generateValidBddEncoding() {
    //        Parameter region = Parameter.builder()
    //                .name("Region")
    //                .type(ParameterType.STRING)
    //                .required(true)
    //                .documentation("The AWS region")
    //                .build();
    //
    //        Parameter useFips = Parameter.builder()
    //                .name("UseFips")
    //                .type(ParameterType.BOOLEAN)
    //                .required(true)
    //                .defaultValue(software.amazon.smithy.rulesengine.language.evaluation.value.Value.booleanValue(false))
    //                .documentation("Use FIPS endpoints")
    //                .build();
    //
    //        Parameters params = Parameters.builder()
    //                .addParameter(region)
    //                .addParameter(useFips)
    //                .build();
    //
    //        Condition useFipsTrue = Condition.builder()
    //                .fn(BooleanEquals.ofExpressions(
    //                        Expression.getReference(Identifier.of("UseFips")),
    //                        Expression.of(true)))
    //                .build();
    //
    //        // Create endpoints
    //        Endpoint normalEndpoint = Endpoint.builder()
    //                .url(Expression.of("https://service.{Region}.amazonaws.com"))
    //                .build();
    //
    //        Endpoint fipsEndpoint = Endpoint.builder()
    //                .url(Expression.of("https://service-fips.{Region}.amazonaws.com"))
    //                .build();
    //
    //        Rule fipsRule = EndpointRule.builder()
    //                .condition(useFipsTrue)
    //                .endpoint(fipsEndpoint);
    //
    //        Rule normalRule = EndpointRule.builder()
    //                .endpoint(normalEndpoint);
    //
    //        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
    //                .parameters(params)
    //                .rules(Arrays.asList(fipsRule, normalRule))
    //                .build();
    //
    //        Cfg cfg = Cfg.from(ruleSet);
    //
    //        BddTrait trait = BddTrait.from(cfg);
    //        BddTraitValidator validator = new BddTraitValidator();
    //        ServiceShape service = ServiceShape.builder().id("foo#Bar").addTrait(trait).build();
    //        Model model = Model.builder().addShape(service).build();
    //        System.out.println(validator.validate(model));
    //
    //        // Get the base64 encoded nodes
    //        System.out.println(Node.prettyPrintJson(trait.toNode()));
    //    }
}
