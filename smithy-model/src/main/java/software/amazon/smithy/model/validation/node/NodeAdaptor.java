package software.amazon.smithy.model.validation.node;

import software.amazon.smithy.jmespath.evaluation.Adaptor;
import software.amazon.smithy.model.node.Node;

public class NodeAdaptor implements Adaptor<Node> {

    @Override
    public Node createNull() {
        return null;
    }
}
