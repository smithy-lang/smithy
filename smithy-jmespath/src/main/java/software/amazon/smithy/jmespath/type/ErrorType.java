package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.EnumSet;
import java.util.Objects;

public class ErrorType implements Type {

    private static final EnumSet<RuntimeType> TYPES = EnumSet.noneOf(RuntimeType.class);

    // The type of error, or null if unknown
    private final JmespathExceptionType errorType;

    public ErrorType(JmespathExceptionType errorType) {
        this.errorType = errorType;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ErrorType)) {
            return false;
        }

        ErrorType that = (ErrorType)obj;
        return Objects.equals(errorType, that.errorType);
    }

    @Override
    public int hashCode() {
        return ErrorType.class.hashCode() + Objects.hashCode(errorType);
    }

    @Override
    public <T> boolean isInstance(T value, JmespathRuntime<T> runtime) {
        // Errors are not actually runtime values
        return false;
    }

    @Override
    public EnumSet<RuntimeType> runtimeTypes() {
        return TYPES;
    }
}
