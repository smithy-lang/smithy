package software.amazon.smithy.rulesengine.language;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.RulesetTestUtil;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;

public class TypeIntrospectionTest {
    @Test
    void introspectCorrectTypesForFunctions() {
        EndpointRuleSet actual = RulesetTestUtil.loadRuleSet(
                "software/amazon/smithy/rulesengine/testutil/valid-rules/substring.json");
        List<Condition> conditions = actual.getRules().get(0).getConditions();
        // stringEquals({TestCaseId}, 1)
        assertEquals(conditions.get(0).getFn().type(), Type.bool());

        // output = substring({Input}, ...);
        assertEquals(conditions.get(1).getFn().type(), Type.optional(Type.string()));
    }

    @Test
    void introspectCorrectTypesForGetAttr() {
        EndpointRuleSet actual = RulesetTestUtil.loadRuleSet(
                "software/amazon/smithy/rulesengine/testutil/valid-rules/get-attr-type-inference.json");
        // bucketArn.resourceId[2]
        assertEquals(actual.getRules().get(0).getConditions().get(2).getFn().type(), Type.optional(Type.string()));
    }
}
