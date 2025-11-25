package software.amazon.smithy.model.validation.node;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.ContractsTrait;
import software.amazon.smithy.model.traits.LengthTrait;

import java.util.Map;

public class ContractsTraitPlugin extends MemberAndShapeTraitPlugin<Shape, Node, ContractsTrait> {

    ContractsTraitPlugin() {
        super(Shape.class, Node.class, ContractsTrait.class);
    }

    @Override
    protected void check(Shape shape, ContractsTrait trait, Node value, Context context, Emitter emitter) {
        for (Map.Entry<String, ContractsTrait.Contract> entry : trait.getValues().entrySet()) {
            events.addAll(validatePath(model, shape, constraints, entry.getValue().getExpression()));
        }
    }
}
