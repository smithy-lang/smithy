package software.amazon.smithy.rulesengine.aws.language.functions;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
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
    private static EndpointRuleSet originalRules;
    private static EndpointRuleSet rules;
    private static List<EndpointTestCase> testCases;

    @BeforeAll
    static void loadS3Model() {
        Model model = Model.assembler()
                .discoverModels()
                .assemble()
                .unwrap();

        ServiceShape s3Service = model.expectShape(S3_SERVICE_ID, ServiceShape.class);
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
        
        System.out.println(sb);
    }
}
