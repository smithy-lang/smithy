package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UnionType implements Type {

    private final List<Type> types;

    public UnionType(Type ... types) {
        this(Arrays.asList(types));
    }

    public UnionType(List<Type> types) {
        this.types = types;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UnionType)) {
            return false;
        }
        UnionType other = (UnionType) obj;
        return types.equals(other.types);
    }

    @Override
    public int hashCode() {
        return UnionType.class.hashCode() + types.hashCode();
    }

    @Override
    public <T> boolean isInstance(T value, JmespathRuntime<T> runtime) {
        for (Type type : types) {
            if (type.isInstance(value, runtime)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Type elementType() {
        return map(Type::elementType);
    }

    public Type valueType(Type key) {
        return map(t -> t.valueType(key));
    }

    public Type expectType(RuntimeType type) {
        return map(t -> t.expectType(type));
    }

    @Override
    public Type expectAnyOf(Set<RuntimeType> runtimeTypes) {
        return map(t -> t.expectAnyOf(runtimeTypes));
    }

    private Type map(Function<Type, Type> f) {
        return new UnionType(types.stream().map(f).collect(Collectors.toList()));

    }

    @Override
    public String toString() {
        return types.stream().map(Type::toString).collect(Collectors.joining(" | "));
    }
}
