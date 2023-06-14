package software.amazon.smithy.rulesengine.language;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.utils.IoUtils;

public class TypeIntrospectionTest {
    @Test
    void introspectCorrectTypesForFunctions() {
        EndpointRuleSet endpointRuleSet = EndpointRuleSet.fromNode(Node.parse(IoUtils.readUtf8Resource(
                TypeIntrospectionTest.class, "substring.json")));
        List<Condition> conditions = endpointRuleSet.getRules().get(0).getConditions();
        // stringEquals({TestCaseId}, 1)
        assertEquals(conditions.get(0).getFn().type(), Type.booleanType());

        // output = substring({Input}, ...);
        assertEquals(conditions.get(1).getFn().type(), Type.optionalType(Type.stringType()));
    }

    @Test
    void introspectCorrectTypesForGetAttr() {
        EndpointRuleSet endpointRuleSet = EndpointRuleSet.fromNode(Node.parse(IoUtils.readUtf8Resource(
                TypeIntrospectionTest.class, "get-attr-type-inference.json")));
        // bucketUrl.authority
        Type actualType = endpointRuleSet.getRules().get(0).getConditions().get(2).getFn().type();
        assertEquals(Type.stringType(), actualType);
    }
}
