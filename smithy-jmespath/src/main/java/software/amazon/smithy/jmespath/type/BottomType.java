package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BottomType implements Type {

    public static final BottomType INSTANCE = new BottomType();

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BottomType;
    }

    @Override
    public int hashCode() {
        return BottomType.class.hashCode();
    }

    @Override
    public <T> boolean isInstance(T value, JmespathRuntime<T> runtime) {
        return false;
    }

    @Override
    public Type elementType() {
        return INSTANCE;
    }

    @Override
    public Type valueType(Type key) {
        return INSTANCE;
    }

    @Override
    public Type expectAnyOf(Set<RuntimeType> types) {
        return new ErrorType(JmespathExceptionType.INVALID_TYPE);
    }

    @Override
    public String toString() {
        return "bottom";
    }
}
