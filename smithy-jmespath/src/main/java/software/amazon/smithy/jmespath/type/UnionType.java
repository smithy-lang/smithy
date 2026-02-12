package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class UnionType implements Type {

    private final List<Type> types;
    private final EnumSet<RuntimeType> runtimeTypes;

    public UnionType(Type ... types) {
        this(Arrays.asList(types));
    }

    public UnionType(List<Type> types) {
        this.types = types;
        this.runtimeTypes = EnumSet.noneOf(RuntimeType.class);
        types.forEach(type -> runtimeTypes.addAll(type.runtimeTypes()));
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
    public EnumSet<RuntimeType> runtimeTypes() {
        return runtimeTypes;
    }

    @Override
    public Type elementType() {
        return types.stream().map(Type::elementType).reduce(Type.bottomType(), Type::unionType);
    }

    public Type valueType(Type key) {
        return types.stream().map(t -> t.valueType(key)).reduce(Type.bottomType(), Type::unionType);
    }

    @Override
    public String toString() {
        return types.stream().map(Type::toString).collect(Collectors.joining(" | "));
    }
}
