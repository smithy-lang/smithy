package software.amazon.smithy.model.validation.node;

import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.evaluation.Evaluator;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.ContractsTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;

import java.util.Map;

public class ContractsTraitPlugin extends MemberAndShapeTraitPlugin<Shape, Node, ContractsTrait> {

    ContractsTraitPlugin() {
        super(Shape.class, Node.class, ContractsTrait.class);
    }

    @Override
    protected void check(Shape shape, ContractsTrait trait, Node value, Context context, Emitter emitter) {
        for (Map.Entry<String, ContractsTrait.Contract> entry : trait.getValues().entrySet()) {
            checkContract(shape, entry.getValue(), value, context, emitter);
        }
    }

    private void checkContract(Shape shape, ContractsTrait.Contract contract, Node value, Context context, Emitter emitter) {
        JmespathExpression expression = JmespathExpression.parse(contract.getExpression());
        Evaluator<Node> evaluator = new Evaluator<>(value, new NodeAdaptor());
        Node result = evaluator.visit(expression);
        if (!result.expectBooleanNode().getValue()) {
            emitter.accept(value,
                    getSeverity(context),
                    String.format(
                            "Value provided for `%s` must match contract expression: %s",
                            shape.getId(),
                            contract.getExpression()));
        }
    }

    private Severity getSeverity(Context context) {
        return context.hasFeature(NodeValidationVisitor.Feature.ALLOW_CONSTRAINT_ERRORS)
                ? Severity.WARNING
                : Severity.ERROR;
    }
}
