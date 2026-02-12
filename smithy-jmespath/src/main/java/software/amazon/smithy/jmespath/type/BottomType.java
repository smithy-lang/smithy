package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class BottomType implements Type {

    public static final BottomType INSTANCE = new BottomType();

    private static final EnumSet<RuntimeType> TYPES = EnumSet.noneOf(RuntimeType.class);

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

    @Override
    public String toString() {
        return "bottom";
    }
}
