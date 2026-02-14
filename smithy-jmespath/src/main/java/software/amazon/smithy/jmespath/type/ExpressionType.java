package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public class ExpressionType extends AbstractType {

    private static final EnumSet<RuntimeType> TYPES = EnumSet.of(RuntimeType.EXPRESSION);

    private final JmespathExpression expression;

    public ExpressionType(JmespathExpression expression) {
        this.expression = expression;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ExpressionType)) {
            return false;
        }

        ExpressionType that = (ExpressionType)obj;
        return Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return ExpressionType.class.hashCode() + Objects.hashCode(expression);
    }

    @Override
    protected RuntimeType runtimeType() {
        return RuntimeType.EXPRESSION;
    }

    @Override
    public <T> boolean isInstance(T value, JmespathRuntime<T> runtime) {
        // Expressions are not actually runtime values
        return false;
    }

    @Override
    public Type expectAnyOf(Set<RuntimeType> types) {
        return new ErrorType(JmespathExceptionType.INVALID_TYPE);
    }

    @Override
    public JmespathExpression expectExpression() {
        return expression;
    }
}
