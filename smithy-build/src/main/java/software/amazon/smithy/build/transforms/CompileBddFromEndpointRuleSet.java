/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;
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

/**
 * Compiles a Binary Decision Diagram (BDD) from a service's {@code @endpointRuleSet}
 * trait and attaches the compiled {@code @endpointBdd} trait to the service shape.
 */
public final class CompileBddFromEndpointRuleSet implements ProjectionTransformer {
    private static final ShapeId S3_SERVICE_ID = ShapeId.from("com.amazonaws.s3#AmazonS3");

    @Override
    public String getName() {
        return "compileBddFromEndpointRuleSet";
    }

    @Override
    public Model transform(TransformContext transformContext) {
        Model model = transformContext.getModel();
        Collection<Shape> shapes = new HashSet<>();
        Set<ServiceShape> serviceShapes = model.getServiceShapes();
        for (ServiceShape serviceShape : serviceShapes) {
            if (serviceShape.hasTrait(EndpointRuleSetTrait.ID)) {
                EndpointRuleSetTrait endpointRuleSetTrait = serviceShape.expectTrait(EndpointRuleSetTrait.class);
                EndpointRuleSet rules = endpointRuleSetTrait.getEndpointRuleSet();
                // S3 has special tree transformations that dramatically improve the BDD compiled result
                // both in size and performance
                if (serviceShape.getId().equals(S3_SERVICE_ID)) {
                    rules = transformRulesetForS3(rules, serviceShape.expectTrait(EndpointTestsTrait.class));
                }
                EndpointBddTrait bdd = compileBdd(rules);
                shapes.add(serviceShape.toBuilder().addTrait(bdd).build());
            }
        }
        return ModelTransformer.create().replaceShapes(model, shapes);
    }

    private EndpointRuleSet transformRulesetForS3(EndpointRuleSet rules, EndpointTestsTrait testsTrait) {
        EndpointRuleSet transformedRules = S3TreeRewriter.transform(rules);
        // Verify transforms preserve semantics by running all test cases
        for (EndpointTestCase testCase : testsTrait.getTestCases()) {
            TestEvaluator.evaluate(transformedRules, testCase);
        }
        return transformedRules;
    }

    private EndpointBddTrait compileBdd(EndpointRuleSet rules) {
        // Create the CFG to start BDD compilation process.
        Cfg cfg = Cfg.from(rules);
        EndpointBddTrait unoptimizedTrait = EndpointBddTrait.from(cfg);

        // Sift the BDD to shorten paths and reduce the BDD size.
        EndpointBddTrait siftedTrait = SiftingOptimization.builder().cfg(cfg).build().apply(unoptimizedTrait);

        // "cost optimize" the BDD to ensure cheap conditions come first with up to 10% size impact.
        EndpointBddTrait costOptimizedTrait = CostOptimization.builder().cfg(cfg).build().apply(siftedTrait);

        // Remove unreferenced conditions. This is destructive and further optimizations cannot be applied after this.
        return costOptimizedTrait.removeUnreferencedConditions();
    }
}
