package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.EnumSet;

public class AnyType implements Type {

    public static final AnyType INSTANCE = new AnyType();

    private static final EnumSet<RuntimeType> TYPES = EnumSet.allOf(RuntimeType.class);

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BooleanType;
    }

    @Override
    public int hashCode() {
        return BooleanType.class.hashCode();
    }

    @Override
    public <T> boolean isInstance(T value, JmespathRuntime<T> runtime) {
        return true;
    }

    @Override
    public EnumSet<RuntimeType> runtimeTypes() {
        return TYPES;
    }

    @Override
    public Type elementType() {
        return INSTANCE;
    }

    @Override
    public Type valueType(Type key) {
        return INSTANCE;
    }
}
