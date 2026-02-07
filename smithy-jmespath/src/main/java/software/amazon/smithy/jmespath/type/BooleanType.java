package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.EnumSet;

public final class BooleanType implements Type {

    public static final BooleanType INSTANCE = new BooleanType();

    private static final EnumSet<RuntimeType> TYPES = EnumSet.of(RuntimeType.BOOLEAN);

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
        return runtime.is(value, RuntimeType.BOOLEAN);
    }

    @Override
    public EnumSet<RuntimeType> runtimeTypes() {
        return TYPES;
    }

    @Override
    public String toString() {
        return "boolean";
    }
}
