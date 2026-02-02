package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;

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
    public EnumSet<RuntimeType> runtimeTypes() {
        return runtimeTypes;
    }

    @Override
    public String toString() {
        return types.stream().map(Type::toString).collect(Collectors.joining(" | "));
    }
}
