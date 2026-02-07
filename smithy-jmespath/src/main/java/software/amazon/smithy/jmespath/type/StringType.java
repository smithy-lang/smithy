package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.EnumSet;

public class StringType implements Type {

    public static final StringType INSTANCE = new StringType();

    @Override
    public boolean equals(Object obj) {
        return obj instanceof StringType;
    }

    @Override
    public int hashCode() {
        return StringType.class.hashCode();
    }

    @Override
    public <T> boolean isInstance(T value, JmespathRuntime<T> runtime) {
        return runtime.is(value, RuntimeType.STRING);
    }

    private static final EnumSet<RuntimeType> TYPES = EnumSet.of(RuntimeType.STRING);

    @Override
    public EnumSet<RuntimeType> runtimeTypes() {
        return TYPES;
    }

    @Override
    public String toString() {
        return "string";
    }
}
