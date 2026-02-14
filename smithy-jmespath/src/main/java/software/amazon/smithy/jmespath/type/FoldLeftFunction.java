package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.evaluation.AbstractEvaluator;
import software.amazon.smithy.jmespath.evaluation.Evaluator;
import software.amazon.smithy.jmespath.evaluation.Function;
import software.amazon.smithy.jmespath.evaluation.FunctionArgument;
import software.amazon.smithy.jmespath.evaluation.FunctionRegistry;
import software.amazon.smithy.jmespath.evaluation.JmespathAbstractRuntime;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoldLeftFunction implements Function<Type> {
    @Override
    public String name() {
        return "fold_left";
    }

    @Override
    public Type abstractApply(AbstractEvaluator<Type> evaluator, List<FunctionArgument<Type>> functionArguments) {
        Type init = functionArguments.get(0).expectValue();
        JmespathExpression f = functionArguments.get(1).expectExpression();
        Type array = functionArguments.get(2).expectArray();

        // "evaluate" f in a typing context of {acc: result, element: array.elementType()}
        // and determine the fix point
        // TODO: If `array` is more specific (say a @length limit) we may not need to do that.
        // TODO: This may actually not terminate in some cases, say if init is a TupleType and f extends the tuple.
        // But we could detect that and widen to an ArrayType first, which won't grow in the same way.
        Type result = init;
        Type prevResult = null;
        while (!result.equals(prevResult)) {
            Map<Type, Type> contextTypes = new HashMap<>();
            contextTypes.put(evaluator.runtime().createString("acc"), result);
            contextTypes.put(evaluator.runtime().createString("element"), array.elementType());
            Type fContextType = new ObjectType(contextTypes);

            prevResult = result;
            // TODO: Not passing along the function registry
            result = evaluator.runtime().either(result, f.evaluate(fContextType, evaluator.runtime()));
        }
        return result;
    }
}
