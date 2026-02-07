package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.EnumSet;

public class NullType implements Type {

    public static final NullType INSTANCE = new NullType();

    private static final EnumSet<RuntimeType> TYPES = EnumSet.of(RuntimeType.NULL);

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NullType;
    }

    @Override
    public int hashCode() {
        return NullType.class.hashCode();
    }

    @Override
    public <T> boolean isInstance(T value, JmespathRuntime<T> runtime) {
        return runtime.is(value, RuntimeType.NULL);
    }

    @Override
    public EnumSet<RuntimeType> runtimeTypes() {
        return TYPES;
    }

    @Override
    public String toString() {
        return "null";
    }
}
