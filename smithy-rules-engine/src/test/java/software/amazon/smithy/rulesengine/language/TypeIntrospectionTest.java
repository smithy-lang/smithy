package software.amazon.smithy.rulesengine.language;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.eval.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;

public class TypeIntrospectionTest {
    @Test
    void introspectCorrectTypesForFunctions() throws IOException {
        EndpointRuleSet actual = TestDiscovery.getValidRuleSets().get("substring.json");
        List<Condition> conditions = actual.getRules().get(0).getConditions();
        // stringEquals({TestCaseId}, 1)
        assertEquals(conditions.get(0).getFn().type(), Type.booleanType());

        // output = substring({Input}, ...);
        assertEquals(conditions.get(1).getFn().type(), Type.optionalType(Type.stringType()));
    }

    @Test
    void introspectCorrectTypesForGetAttr() throws IOException {
        EndpointRuleSet actual = TestDiscovery.getValidRuleSets().get("get-attr-type-inference.json");
        // bucketArn.resourceId[2]
        assertEquals(actual.getRules().get(0).getConditions().get(2).getFn().type(), Type.optionalType(Type.stringType()));
    }
}
