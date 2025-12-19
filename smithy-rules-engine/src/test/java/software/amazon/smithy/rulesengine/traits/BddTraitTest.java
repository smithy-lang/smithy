/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.traits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Coalesce;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.NoMatchRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.TestHelpers;
import software.amazon.smithy.rulesengine.logic.bdd.Bdd;
import software.amazon.smithy.utils.ListUtils;

public class BddTraitTest {
    @Test
    void testBddTraitSerialization() {
        // Create a BddTrait with full context
        Parameter regionParam = Parameter.builder()
                .name("Region")
                .type(ParameterType.STRING)
                .build();
        Parameters params = Parameters.builder().addParameter(regionParam).build();
        Condition cond = Condition.builder().fn(TestHelpers.isSet("Region")).build();
        Rule endpoint = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example.com"));

        List<Rule> results = new ArrayList<>();
        results.add(NoMatchRule.INSTANCE);
        results.add(endpoint);

        Bdd bdd = createSimpleBdd();

        EndpointBddTrait original = EndpointBddTrait.builder()
                .parameters(params)
                .conditions(ListUtils.of(cond))
                .results(results)
                .bdd(bdd)
                .build();

        // Serialize to Node
        Node node = original.toNode();
        assertTrue(node.isObjectNode());
        assertTrue(node.expectObjectNode().containsMember("parameters"));
        assertTrue(node.expectObjectNode().containsMember("conditions"));
        assertTrue(node.expectObjectNode().containsMember("results"));

        // Serialized should only have 1 result (the endpoint, not NoMatch)
        int serializedResultCount = node.expectObjectNode()
                .expectArrayMember("results")
                .getElements()
                .size();
        assertEquals(1, serializedResultCount);

        // Deserialize from Node
        EndpointBddTrait restored = EndpointBddTrait.fromNode(node);

        assertEquals(original.getParameters(), restored.getParameters());
        assertEquals(original.getConditions().size(), restored.getConditions().size());
        assertEquals(original.getResults().size(), restored.getResults().size());
        assertEquals(original.getBdd().getRootRef(), restored.getBdd().getRootRef());
        assertEquals(original.getBdd().getConditionCount(), restored.getBdd().getConditionCount());
        assertEquals(original.getBdd().getResultCount(), restored.getBdd().getResultCount());

        // Verify NoMatchRule was restored at index 0
        assertInstanceOf(NoMatchRule.class, restored.getResults().get(0));
    }

    private Bdd createSimpleBdd() {
        int[] nodes = new int[] {
                -1,
                1,
                -1, // node 0: terminal
                0,
                Bdd.RESULT_OFFSET + 1,
                -1 // node 1
        };
        return new Bdd(2, 1, 2, 2, nodes);
    }

    @Test
    void testEmptyBddTrait() {
        Parameters params = Parameters.builder().build();
        int[] nodes = new int[] {-1, 1, -1};
        Bdd bdd = new Bdd(-1, 0, 1, 1, nodes);

        EndpointBddTrait trait = EndpointBddTrait.builder()
                .parameters(params)
                .conditions(ListUtils.of())
                .results(ListUtils.of(NoMatchRule.INSTANCE))
                .bdd(bdd)
                .build();

        assertEquals(0, trait.getConditions().size());
        assertEquals(1, trait.getResults().size());
        assertEquals(-1, trait.getBdd().getRootRef()); // FALSE terminal
    }

    @Test
    void testBuildTypeChecksExpressionsForCodegen() {
        // Verify that after building an EndpointBddTrait, expression.type() works
        // This is important for codegen to infer types without a scope
        Parameter regionParam = Parameter.builder()
                .name("Region")
                .type(ParameterType.STRING)
                .build();
        Parameters params = Parameters.builder().addParameter(regionParam).build();

        // Create a condition with a coalesce that infers to String
        Expression regionRef = Expression.getReference(Identifier.of("Region"));
        Expression fallback = Literal.of("us-east-1");
        Coalesce coalesce = Coalesce.ofExpressions(regionRef, fallback);
        Condition cond = Condition.builder().fn(coalesce).result(Identifier.of("resolvedRegion")).build();

        Rule endpoint = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example.com"));

        List<Rule> results = new ArrayList<>();
        results.add(NoMatchRule.INSTANCE);
        results.add(endpoint);

        EndpointBddTrait trait = EndpointBddTrait.builder()
                .parameters(params)
                .conditions(ListUtils.of(cond))
                .results(results)
                .bdd(createSimpleBdd())
                .build();

        // After build(), type() should work on the coalesce expression
        // Region is Optional<String>, fallback is String, so result is String (non-optional)
        Coalesce builtCoalesce = (Coalesce) trait.getConditions().get(0).getFunction();
        assertEquals(Type.stringType(), builtCoalesce.type());
    }
}
