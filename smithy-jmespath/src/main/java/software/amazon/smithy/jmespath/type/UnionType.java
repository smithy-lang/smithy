package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UnionType implements Type {

    private final Set<Type> types;

    public UnionType(Type ... types) {
        this(new HashSet<>(Arrays.asList(types)));
    }

    public UnionType(Collection<? extends Type> types) {
        this.types = new HashSet<>();
        // TODO: Extract out of constructor, so we can collapse to a non-union type sometimes
        for (Type type : types) {
            if (type == null) {
                throw new NullPointerException();
            }
            if (type instanceof UnionType) {
                this.types.addAll(((UnionType)type).types);
            } else {
                this.types.add(type);
            }
        }
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
        return new UnionType(types.stream().map(f).collect(Collectors.toSet()));

    }

    @Override
    public String toString() {
        return types.stream().map(Type::toString).collect(Collectors.joining(" | "));
    }
}
