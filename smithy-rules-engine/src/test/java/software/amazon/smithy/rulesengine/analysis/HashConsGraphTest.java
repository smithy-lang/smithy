/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.analysis;

import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;

public class HashConsGraphTest {
    @Test
    public void test() throws Exception {
        String[] regional = {
                "/Users/dowling/projects/aws-sdk-js-v3/codegen/sdk-codegen/aws-models/connect.json",
                "com.amazonaws.connect#AmazonConnectService"
        };
        String[] s3 = {
                "/Users/dowling/projects/smithy-java/aws/client/aws-client-rulesengine/src/shared-resources/software/amazon/smithy/java/aws/client/rulesengine/s3.json",
                "com.amazonaws.s3#AmazonS3"
        };
        String[] inputs = s3;

        Model model = Model.assembler()
                .addImport(Paths.get(inputs[0]))
                .discoverModels()
                .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                .assemble()
                .unwrap();

        ServiceShape service = model.expectShape(ShapeId.from(inputs[1]), ServiceShape.class);
        EndpointRuleSet ruleSet = service.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet();
        HashConsGraph graph = new HashConsGraph(ruleSet);

        double paths = graph.getPaths().size();
        double totalConditions = 0;
        int maxDepth = 0;
        for (HashConsGraph.BddPath path : graph.getPaths()) {
            totalConditions += path.getStatefulConditions().size() + path.getStatelessConditions().size();
            maxDepth = Math.max(maxDepth, path.getStatefulConditions().size() + path.getStatelessConditions().size());
            System.out.println(path);
        }

        System.out.println("Max depth: " + maxDepth);
        System.out.println("Average path conditions: " + (totalConditions / paths));
        System.out.println("BDD:");
        System.out.println(graph.getBdd());


        EndpointRuleSet updated = ruleSet.toBddForm();
        EndpointRuleSetTrait updatedTrait = service
                .expectTrait(EndpointRuleSetTrait.class)
                .toBuilder()
                .ruleSet(updated.toNode())
                .build();
        ServiceShape updatedService = service.toBuilder().addTrait(updatedTrait).build();
        Model updatedModel = model.toBuilder().addShape(updatedService).build();

        Files.write(Paths.get("/tmp/s3.json"), Node.prettyPrintJson(ModelSerializer.builder().build().serialize(updatedModel)).getBytes());
    }
}
