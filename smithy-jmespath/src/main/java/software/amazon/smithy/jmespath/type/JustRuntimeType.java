package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.EnumSet;

public class JustRuntimeType extends AbstractType {

    private final RuntimeType runtimeType;

    public JustRuntimeType(RuntimeType runtimeType) {
        this.runtimeType = runtimeType;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JustRuntimeType)) {
            return false;
        }

        JustRuntimeType other = (JustRuntimeType) obj;
        return runtimeType.equals(other.runtimeType);
    }

    @Override
    public int hashCode() {
        return runtimeType.hashCode();
    }

    @Override
    protected RuntimeType runtimeType() {
        return runtimeType;
    }

    @Override
    public <T> boolean isInstance(T value, JmespathRuntime<T> runtime) {
        return runtime.is(value, runtimeType);
    }

    @Override
    public String toString() {
        return "null";
    }
}
