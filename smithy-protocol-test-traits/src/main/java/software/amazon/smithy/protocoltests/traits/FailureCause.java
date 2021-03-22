package software.amazon.smithy.protocoltests.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

public abstract class FailureCause {
    private static final String GENERIC = "generic";
    public static class Generic extends FailureCause {
        private final String message;
        public Generic(String message) {
            this.message = message;
        }

        public static Generic fromNode(Node node) {
            return new Generic(node.expectStringNode().getValue());
        }

        public String getMessage() {
            return message;
        }
    }

    public static FailureCause fromNode(Node node) {
        ObjectNode o = node.expectObjectNode();
        if (o.containsMember(GENERIC)) {
            return Generic.fromNode(o.expectMember(GENERIC));
        } else {
            // This should be unreachable if the input was a valid Smithy model
            throw new RuntimeException("Unreachable");
        }
    }
}
