package software.amazon.smithy.rulesengine.language.syntax.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.GetAttr;

public class GetAttrTest {
    @Test
    public void getAttrManualEqualToTemplate() {
        Expression asTemplate = Expression.parseShortform("a#b[5]", SourceLocation.none());
        Expression asGetAttr = GetAttr.fromNode(ObjectNode
                .builder()
                .withMember("fn", Node.from("getAttr"))
                .withMember("argv", Node.fromNodes(
                        ObjectNode.builder().withMember("ref", "a").build(),
                        Node.from("b[5]"))
                ).build());
        assertEquals(asTemplate, asGetAttr);
        assertEquals(asTemplate.hashCode(), asGetAttr.hashCode());
    }
}
