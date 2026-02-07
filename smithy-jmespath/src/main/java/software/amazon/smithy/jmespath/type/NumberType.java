package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.EnumSet;

public class NumberType implements Type {

    public static final NumberType INSTANCE = new NumberType();

    private static final EnumSet<RuntimeType> TYPES = EnumSet.of(RuntimeType.NULL);

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NumberType;
    }

    @Override
    public int hashCode() {
        return NumberType.class.hashCode();
    }

    @Override
    public <T> boolean isInstance(T value, JmespathRuntime<T> runtime) {
        return runtime.is(value, RuntimeType.NUMBER);
    }

    @Override
    public EnumSet<RuntimeType> runtimeTypes() {
        return TYPES;
    }

    @Override
    public String toString() {
        return "number";
    }
}
