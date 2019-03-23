package software.amazon.smithy.codegen.freemarker;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NullNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

final class SmithyObjectWrapper extends DefaultObjectWrapper {
    SmithyObjectWrapper(Version incompatibleImprovements) {
        super(incompatibleImprovements);
    }

    @Override
    protected TemplateModel handleUnknownType(Object obj) throws TemplateModelException {
        if (obj instanceof Optional) {
            Optional optional = (Optional) obj;
            return optional.isPresent() ? wrap(optional.get()) : null;
        } else if (obj instanceof StringNode) {
            return wrap(((StringNode) obj).getValue());
        } else if (obj instanceof BooleanNode) {
            return wrap(((BooleanNode) obj).getValue());
        } else if (obj instanceof NullNode) {
            return wrap(null);
        } else if (obj instanceof NumberNode) {
            return wrap(((NumberNode) obj).getValue());
        } else if (obj instanceof ArrayNode) {
            return wrap(((ArrayNode) obj).getElements());
        } else if (obj instanceof ObjectNode) {
            var objectNode = (ObjectNode) obj;
            Map<String, Node> map = new HashMap<>(objectNode.size());
            objectNode.getMembers().forEach((k, v) -> map.put(k.getValue(), v));
            return wrap(map);
        } else if (obj instanceof Stream) {
            return wrap(((Stream) obj).iterator());
        }

        return super.handleUnknownType(obj);
    }
}
