package software.amazon.smithy.rulesengine;

import java.io.InputStream;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;

public class RulesetTestUtil {
    public static EndpointRuleSet loadRuleSet(String resourceId) {
        InputStream is = RulesetTestUtil.class.getClassLoader().getResourceAsStream(resourceId);
        assert is != null;
        Node node = ObjectNode.parse(is);
        return EndpointRuleSet.fromNode(node);
    }

    public static EndpointRuleSet minimalRuleSet() {
        return loadRuleSet("software/amazon/smithy/rulesengine/testutil/valid-rules/minimal-ruleset.json");
    }

}
