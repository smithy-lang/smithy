package software.amazon.smithy.jmespath.functions;

import software.amazon.smithy.jmespath.evaluation.Runtime;
import software.amazon.smithy.jmespath.evaluation.NumberType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public class AbsFunction implements Function {
    @Override
    public String name() {
        return "abs";
    }

    @Override
    public <T> T apply(Runtime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectNumber();
        Number number = runtime.toNumber(value);

        switch (runtime.numberType(value)) {
            case BYTE:
            case SHORT:
            case INTEGER:
                return runtime.createNumber(Math.abs(number.intValue()));
            case LONG:
                return runtime.createNumber(Math.abs(number.longValue()));
            case FLOAT: return runtime.createNumber(Math.abs(number.floatValue()));
            case DOUBLE: return runtime.createNumber(Math.abs(number.doubleValue()));
            case BIG_INTEGER: return runtime.createNumber(((BigInteger)number).abs());
            case BIG_DECIMAL: return runtime.createNumber(((BigDecimal)number).abs());
            default:
                throw new IllegalArgumentException("`abs` only supports numeric arguments");
        }
    }
}
