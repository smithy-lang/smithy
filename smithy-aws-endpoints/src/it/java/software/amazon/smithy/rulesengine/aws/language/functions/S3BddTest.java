/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.language.functions;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.rulesengine.aws.s3.S3TreeRewriter;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.evaluation.TestEvaluator;
import software.amazon.smithy.rulesengine.logic.bdd.CostOptimization;
import software.amazon.smithy.rulesengine.logic.bdd.SiftingOptimization;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.rulesengine.traits.EndpointTestCase;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;

class S3BddTest {
    private static final ShapeId S3_SERVICE_ID = ShapeId.from("com.amazonaws.s3#AmazonS3");
    private static Model model;
    private static ServiceShape s3Service;
    private static EndpointRuleSet originalRules;
    private static EndpointRuleSet rules;
    private static List<EndpointTestCase> testCases;

    @BeforeAll
    static void loadS3Model() {
        model = Model.assembler()
                .discoverModels()
                .assemble()
                .unwrap();

        s3Service = model.expectShape(S3_SERVICE_ID, ServiceShape.class);
        originalRules = s3Service.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet();
        rules = S3TreeRewriter.transform(originalRules);
        testCases = s3Service.expectTrait(EndpointTestsTrait.class).getTestCases();
    }

    @Test
    void compileToBddWithOptimizations() {
        // Verify transforms preserve semantics by running all test cases
        assertFalse(testCases.isEmpty(), "S3 model should have endpoint test cases");
        for (EndpointTestCase testCase : testCases) {
            TestEvaluator.evaluate(rules, testCase);
        }

        // Build CFG and compile to BDD
        Cfg cfg = Cfg.from(rules);
        EndpointBddTrait trait = EndpointBddTrait.from(cfg);

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== BDD STATS ===\n");
        sb.append("Conditions: ").append(trait.getConditions().size()).append("\n");
        sb.append("Results: ").append(trait.getResults().size()).append("\n");
        sb.append("Initial BDD nodes: ").append(trait.getBdd().getNodeCount()).append("\n");

        // Apply sifting optimization
        SiftingOptimization sifting = SiftingOptimization.builder().cfg(cfg).build();
        EndpointBddTrait siftedTrait = sifting.apply(trait);
        sb.append("After sifting - nodes: ").append(siftedTrait.getBdd().getNodeCount()).append("\n");

        // Apply cost optimization
        CostOptimization cost = CostOptimization.builder().cfg(cfg).build();
        EndpointBddTrait optimizedTrait = cost.apply(siftedTrait);
        sb.append("After cost opt - nodes: ").append(optimizedTrait.getBdd().getNodeCount()).append("\n");

        // Print conditions for analysis
        sb.append("\n=== CONDITIONS ===\n");
        for (int i = 0; i < trait.getConditions().size(); i++) {
            sb.append(i).append(": ").append(trait.getConditions().get(i)).append("\n");
        }

        // Print results (endpoints) for analysis
        sb.append("\n=== RESULTS ===\n");
        for (int i = 0; i < trait.getResults().size(); i++) {
            sb.append(i).append(": ").append(trait.getResults().get(i)).append("\n");
        }

        System.out.println(sb);

        // Write model with BDD trait to build directory
        writeModelWithBddTrait(optimizedTrait);
    }

    private void writeModelWithBddTrait(EndpointBddTrait bddTrait) {
        String buildDir = System.getProperty("buildDir");
        if (buildDir == null) {
            System.out.println("buildDir system property not set, skipping model output");
            return;
        }

        // Create updated service with BDD trait instead of RuleSet trait
        ServiceShape updatedService = s3Service.toBuilder()
                .removeTrait(EndpointRuleSetTrait.ID)
                .addTrait(bddTrait)
                .build();

        // Build updated model
        Model updatedModel = model.toBuilder()
                .removeShape(s3Service.getId())
                .addShape(updatedService)
                .build();

        // Serialize to JSON
        ModelSerializer serializer = ModelSerializer.builder().build();
        String json = Node.prettyPrintJson(serializer.serialize(updatedModel));

        // Write to build directory
        Path outputPath = Paths.get(buildDir, "s3-bdd-model.json");
        try {
            Path parentDir = outputPath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            Files.writeString(outputPath, json);
            System.out.println("Wrote S3 BDD model to: " + outputPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write S3 BDD model", e);
        }
    }
}
