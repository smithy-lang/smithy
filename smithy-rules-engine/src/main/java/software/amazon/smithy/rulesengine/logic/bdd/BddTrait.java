/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.NoMatchRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Trait containing a precompiled BDD with full context for endpoint resolution.
 */
public final class BddTrait extends AbstractTrait implements ToSmithyBuilder<BddTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.rules#bdd");

    private static final Set<String> ALLOWED_PROPERTIES = SetUtils.of(
            "parameters",
            "conditions",
            "results",
            "root",
            "nodes",
            "nodeCount");

    private final Parameters parameters;
    private final List<Condition> conditions;
    private final List<Rule> results;
    private final Bdd bdd;

    private BddTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.parameters = SmithyBuilder.requiredState("parameters", builder.parameters);
        this.conditions = SmithyBuilder.requiredState("conditions", builder.conditions);
        this.results = SmithyBuilder.requiredState("results", builder.results);
        this.bdd = SmithyBuilder.requiredState("bdd", builder.bdd);
    }

    /**
     * Creates a BddTrait from a control flow graph.
     *
     * @param cfg the control flow graph to compile
     * @return the BddTrait containing the compiled BDD and all context
     */
    public static BddTrait from(Cfg cfg) {
        BddCompiler compiler = new BddCompiler(cfg, new BddBuilder());
        Bdd bdd = compiler.compile();

        if (compiler.getOrderedConditions().size() != bdd.getConditionCount()) {
            throw new IllegalStateException("Mismatch between BDD var count and orderedConditions size");
        }

        return builder()
                .parameters(cfg.getRuleSet().getParameters())
                .conditions(compiler.getOrderedConditions())
                .results(compiler.getIndexedResults())
                .bdd(bdd)
                .build();
    }

    /**
     * Gets the parameters for the endpoint rules.
     *
     * @return the parameters
     */
    public Parameters getParameters() {
        return parameters;
    }

    /**
     * Gets the ordered list of conditions.
     *
     * @return the conditions in evaluation order
     */
    public List<Condition> getConditions() {
        return conditions;
    }

    /**
     * Gets the ordered list of results.
     *
     * @return the results (index 0 is always NoMatchRule)
     */
    public List<Rule> getResults() {
        return results;
    }

    /**
     * Gets the BDD structure.
     *
     * @return the BDD
     */
    public Bdd getBdd() {
        return bdd;
    }

    /**
     * Transform this BDD using the given function and return the updated BddTrait.
     *
     * @param transformer Transformer used to modify the trait.
     * @return the updated trait.
     */
    public BddTrait transform(Function<BddTrait, BddTrait> transformer) {
        return transformer.apply(this);
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = ObjectNode.builder();
        builder.withMember("parameters", parameters.toNode());

        ArrayNode.Builder conditionBuilder = ArrayNode.builder();
        for (Condition c : conditions) {
            conditionBuilder.withValue(c);
        }
        builder.withMember("conditions", conditionBuilder.build());

        // Results (skip NoMatchRule at index 0 for serialization)
        ArrayNode.Builder resultBuilder = ArrayNode.builder();
        if (!results.isEmpty() && !(results.get(0) instanceof NoMatchRule)) {
            throw new IllegalStateException("BDD must always have a NoMatchRule as the first result");
        }
        for (int i = 1; i < results.size(); i++) {
            Rule result = results.get(i);
            if (result instanceof NoMatchRule) {
                throw new IllegalStateException("NoMatch rules can only appear at rule index 0. Found at index " + i);
            }
            resultBuilder.withValue(result);
        }
        builder.withMember("results", resultBuilder.build());

        builder.withMember("root", bdd.getRootRef());
        builder.withMember("nodeCount", bdd.getNodeCount());
        builder.withMember("nodes", encodeNodes(bdd));

        return builder.build();
    }

    /**
     * Creates a BddTrait from a Node representation.
     *
     * @param node the node to parse
     * @return the BddTrait
     */
    public static BddTrait fromNode(Node node) {
        ObjectNode obj = node.expectObjectNode();
        obj.warnIfAdditionalProperties(ALLOWED_PROPERTIES);
        Parameters params = Parameters.fromNode(obj.expectObjectMember("parameters"));
        List<Condition> conditions = obj.expectArrayMember("conditions").getElementsAs(Condition::fromNode);

        List<Rule> serializedResults = obj.expectArrayMember("results").getElementsAs(Rule::fromNode);
        List<Rule> results = new ArrayList<>();
        results.add(NoMatchRule.INSTANCE); // Always add no-match at index 0
        results.addAll(serializedResults);

        String nodesBase64 = obj.expectStringMember("nodes").getValue();
        int nodeCount = obj.expectNumberMember("nodeCount").getValue().intValue();
        int rootRef = obj.expectNumberMember("root").getValue().intValue();

        Bdd bdd = decodeBdd(nodesBase64, nodeCount, rootRef, conditions.size(), results.size());

        BddTrait trait = builder()
                .sourceLocation(node)
                .parameters(params)
                .conditions(conditions)
                .results(results)
                .bdd(bdd)
                .build();
        trait.setNodeCache(node);
        return trait;
    }

    private static String encodeNodes(Bdd bdd) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos)) {
            bdd.getNodes((varIdx, high, low) -> {
                try {
                    dos.writeInt(varIdx);
                    dos.writeInt(high);
                    dos.writeInt(low);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            dos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode BDD nodes", e);
        } catch (UncheckedIOException e) {
            throw new RuntimeException("Failed to encode BDD nodes", e.getCause());
        }
    }

    private static Bdd decodeBdd(String base64, int nodeCount, int rootRef, int conditionCount, int resultCount) {
        byte[] data = Base64.getDecoder().decode(base64);
        if (data.length != nodeCount * 12) {
            throw new IllegalArgumentException("Expected " + (nodeCount * 12) + " bytes for " + nodeCount +
                    " nodes, but got " + data.length);
        }

        int[] nodes = new int[nodeCount * 3];
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = buffer.getInt();
        }

        return new Bdd(rootRef, conditionCount, resultCount, nodeCount, nodes);
    }

    /**
     * Creates a new builder for BddTrait.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .parameters(parameters)
                .conditions(conditions)
                .results(results)
                .bdd(bdd);
    }

    /**
     * Builder for BddTrait.
     */
    public static final class Builder extends AbstractTraitBuilder<BddTrait, Builder> {
        private Parameters parameters;
        private List<Condition> conditions;
        private List<Rule> results;
        private Bdd bdd;

        private Builder() {}

        /**
         * Sets the parameters.
         *
         * @param parameters the parameters
         * @return this builder
         */
        public Builder parameters(Parameters parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Sets the conditions.
         *
         * @param conditions the conditions in evaluation order
         * @return this builder
         */
        public Builder conditions(List<Condition> conditions) {
            this.conditions = conditions;
            return this;
        }

        /**
         * Sets the results.
         *
         * @param results the results (must have NoMatchRule at index 0)
         * @return this builder
         */
        public Builder results(List<Rule> results) {
            this.results = results;
            return this;
        }

        /**
         * Sets the BDD structure.
         *
         * @param bdd the BDD
         * @return this builder
         */
        public Builder bdd(Bdd bdd) {
            this.bdd = bdd;
            return this;
        }

        @Override
        public BddTrait build() {
            return new BddTrait(this);
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            BddTrait trait = BddTrait.fromNode(value);
            trait.setNodeCache(value);
            return trait;
        }
    }
}
