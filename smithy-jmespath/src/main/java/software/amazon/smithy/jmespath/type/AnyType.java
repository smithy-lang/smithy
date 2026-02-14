package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.EnumSet;
import java.util.Set;

public class AnyType implements Type {

    public static final AnyType INSTANCE = new AnyType();

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AnyType;
    }

    @Override
    public int hashCode() {
        return AnyType.class.hashCode();
    }

    @Override
    public <T> boolean isInstance(T value, JmespathRuntime<T> runtime) {
        return true;
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
        return this;
    }
}
