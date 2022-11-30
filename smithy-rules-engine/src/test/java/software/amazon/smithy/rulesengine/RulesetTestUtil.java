package software.amazon.smithy.rulesengine;

import java.io.IOException;
import java.io.InputStream;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;

public class RulesetTestUtil {
    public static EndpointRuleSet loadRuleSet(String resourceId) {
        try(InputStream is = RulesetTestUtil.class.getClassLoader().getResourceAsStream(resourceId)) {
            Node node = ObjectNode.parse(is);
            return EndpointRuleSet.fromNode(node);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static EndpointRuleSet minimalRuleSet() {
        return loadRuleSet("software/amazon/smithy/rulesengine/testutil/valid-rules/minimal-ruleset.json");
    }

}
