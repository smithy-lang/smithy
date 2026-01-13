/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.language.functions;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.rulesengine.aws.s3.S3TreeRewriter;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.evaluation.TestEvaluator;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.rulesengine.traits.EndpointTestCase;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;

/**
 * Runs the endpoint test cases against the transformed S3 model. We're fixed to a specific version for this test,
 * but could periodically bump the version if needed.
 */
class S3TreeRewriterTest {
    private static final ShapeId S3_SERVICE_ID = ShapeId.from("com.amazonaws.s3#AmazonS3");

    private static EndpointRuleSet originalRules;
    private static List<EndpointTestCase> testCases;

    @BeforeAll
    static void loadS3Model() {
        Model model = Model.assembler()
                .discoverModels()
                .assemble()
                .unwrap();

        ServiceShape s3Service = model.expectShape(S3_SERVICE_ID, ServiceShape.class);
        originalRules = s3Service.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet();
        testCases = s3Service.expectTrait(EndpointTestsTrait.class).getTestCases();
    }

    @Test
    void transformPreservesEndpointTestSemantics() {
        assertFalse(testCases.isEmpty(), "S3 model should have endpoint test cases");

        EndpointRuleSet transformed = S3TreeRewriter.transform(originalRules);
        for (EndpointTestCase testCase : testCases) {
            TestEvaluator.evaluate(transformed, testCase);
        }
    }
}
