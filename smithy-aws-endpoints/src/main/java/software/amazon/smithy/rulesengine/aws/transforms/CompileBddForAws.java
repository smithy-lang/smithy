/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.transforms;

import static software.amazon.smithy.rulesengine.transforms.CompileBdd.compileBdd;

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
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.rulesengine.traits.EndpointTestCase;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;

/**
 * A dedicated transform to compile Binary Decision Diagram (BDD) from AWS services.
 */
public final class CompileBddForAws implements ProjectionTransformer {

    private static final ShapeId S3_SERVICE_ID = ShapeId.from("com.amazonaws.s3#AmazonS3");

    @Override
    public String getName() {
        return "compileBddForAws";
    }

    @Override
    public Model transform(TransformContext transformContext) {
        Model model = transformContext.getModel();
        Set<Shape> shapes = new HashSet<>();
        for (ServiceShape serviceShape : model.getServiceShapes()) {
            if (serviceShape.hasTrait(EndpointRuleSetTrait.ID)
                    && !serviceShape.hasTrait(EndpointBddTrait.ID)) {
                EndpointRuleSet rules = getEndpointRuleSet(serviceShape);
                EndpointBddTrait bdd = compileBdd(rules);
                shapes.add(serviceShape.toBuilder().addTrait(bdd).build());
            }
        }
        return ModelTransformer.create().replaceShapes(model, shapes);
    }

    private EndpointRuleSet getEndpointRuleSet(ServiceShape serviceShape) {
        EndpointRuleSet rules = serviceShape.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet();
        if (serviceShape.getId().equals(S3_SERVICE_ID)) {
            return applyS3Transform(rules, serviceShape);
        }
        return rules;
    }

    private EndpointRuleSet applyS3Transform(EndpointRuleSet rules, ServiceShape serviceShape) {
        EndpointRuleSet transformedRules = S3TreeRewriter.transform(rules);
        serviceShape.getTrait(EndpointTestsTrait.class).ifPresent(testsTrait -> {
            for (EndpointTestCase testCase : testsTrait.getTestCases()) {
                TestEvaluator.evaluate(transformedRules, testCase);
            }
        });
        return transformedRules;
    }
}
