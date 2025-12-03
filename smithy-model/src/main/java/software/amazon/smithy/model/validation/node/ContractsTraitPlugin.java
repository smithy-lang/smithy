package software.amazon.smithy.model.validation.node;

import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.evaluation.Evaluator;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeJmespathRuntime;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.ContractsTrait;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;

public class ContractsTraitPlugin extends MemberAndShapeTraitPlugin<Shape, Node, ContractsTrait> {

    ContractsTraitPlugin() {
        super(Shape.class, Node.class, ContractsTrait.class);
    }

    @Override
    protected void check(Shape shape, ContractsTrait trait, Node value, Context context, Emitter emitter) {
        for (ContractsTrait.Contract contract : trait.getValues()) {
            checkContract(shape, contract, value, context, emitter);
        }
    }

    private void checkContract(Shape shape, ContractsTrait.Contract contract, Node value, Context context, Emitter emitter) {
        JmespathExpression expression = JmespathExpression.parse(contract.getExpression());
        Evaluator<Node> evaluator = new Evaluator<>(value, new NodeJmespathRuntime());
        Node result = evaluator.visit(expression);
        // TODO: Or should it be isTruthy()?
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
