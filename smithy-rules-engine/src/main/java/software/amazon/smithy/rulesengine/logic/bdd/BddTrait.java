/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
import software.amazon.smithy.rulesengine.logic.cfg.CfgNode;
import software.amazon.smithy.rulesengine.logic.cfg.ConditionData;
import software.amazon.smithy.rulesengine.logic.cfg.ResultNode;
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
        ConditionData conditionData = cfg.getConditionData();
        List<Condition> conditions = Arrays.asList(conditionData.getConditions());

        // Compile the BDD
        BddCompiler compiler = new BddCompiler(cfg, ConditionOrderingStrategy.defaultOrdering(), new BddBuilder());
        Bdd bdd = compiler.compile();

        List<Rule> results = extractResultsFromCfg(cfg, bdd);
        Parameters parameters = cfg.getRuleSet().getParameters();
        return builder().parameters(parameters).conditions(conditions).results(results).bdd(bdd).build();
    }

    private static List<Rule> extractResultsFromCfg(Cfg cfg, Bdd bdd) {
        // The BddCompiler always puts NoMatchRule at index 0
        List<Rule> results = new ArrayList<>();
        results.add(NoMatchRule.INSTANCE);

        Set<Rule> uniqueResults = new LinkedHashSet<>();
        for (CfgNode node : cfg) {
            if (node instanceof ResultNode) {
                Rule result = ((ResultNode) node).getResult();
                if (result != null && !(result instanceof NoMatchRule)) {
                    uniqueResults.add(result.withoutConditions());
                }
            }
        }

        results.addAll(uniqueResults);

        if (results.size() != bdd.getResultCount()) {
            throw new IllegalStateException(String.format(
                    "Result count mismatch: found %d results in CFG but BDD expects %d",
                    results.size(),
                    bdd.getResultCount()));
        }

        return results;
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

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = ObjectNode.builder();
        builder.withMember("parameters", parameters.toNode());

        List<Node> conditionNodes = new ArrayList<>();
        for (Condition c : conditions) {
            conditionNodes.add(c.toNode());
        }
        builder.withMember("conditions", Node.fromNodes(conditionNodes));

        // Results (skip NoMatchRule at index 0 for serialization)
        List<Node> resultNodes = new ArrayList<>();
        if (!results.isEmpty() && !(results.get(0) instanceof NoMatchRule)) {
            throw new IllegalStateException("BDD must always have a NoMatchRule as the first result");
        }
        for (int i = 1; i < results.size(); i++) {
            Rule result = results.get(i);
            if (result instanceof NoMatchRule) {
                throw new IllegalStateException("NoMatch rules can only appear at rule index 0. Found at index " + i);
            }
            resultNodes.add(result.toNode());
        }
        builder.withMember("results", Node.fromNodes(resultNodes));

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
                    writeVarInt(dos, varIdx);
                    writeVarInt(dos, high);
                    writeVarInt(dos, low);
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
        // Special case for empty BDD with just terminal (should never happen, but just in case).
        if (base64.isEmpty() || nodeCount == 0) {
            return new Bdd(rootRef, conditionCount, resultCount, 1, consumer -> {
                consumer.accept(-1, 1, -1);
            });
        }

        byte[] data = Base64.getDecoder().decode(base64);
        return new Bdd(rootRef, conditionCount, resultCount, nodeCount, consumer -> {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    DataInputStream dis = new DataInputStream(bais)) {
                for (int i = 0; i < nodeCount; i++) {
                    consumer.accept(readVarInt(dis), readVarInt(dis), readVarInt(dis));
                }
                if (bais.available() > 0) {
                    throw new IllegalArgumentException("Extra data found after decoding " + nodeCount +
                            " nodes. " + bais.available() + " bytes remaining.");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to decode BDD nodes", e);
            }
        });
    }

    // Zig-zag + varint encode of a signed int
    private static void writeVarInt(DataOutputStream dos, int value) throws IOException {
        int zz = (value << 1) ^ (value >> 31);
        while ((zz & ~0x7F) != 0) {
            dos.writeByte((zz & 0x7F) | 0x80);
            zz >>>= 7;
        }
        dos.writeByte(zz);
    }

    // Decode a signed int from varint + zig-zag
    private static int readVarInt(DataInputStream dis) throws IOException {
        int shift = 0, result = 0;
        while (true) {
            byte b = dis.readByte();
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;
        }
        // reverse zig-zag
        return (result >>> 1) ^ -(result & 1);
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
