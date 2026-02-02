package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.EnumSet;
import java.util.stream.Collectors;

public class ArrayType implements Type {

    private static final EnumSet<RuntimeType> TYPES = EnumSet.of(RuntimeType.ARRAY);

    // Never null - array is equivalent to array<any>
    private final Type member;

    public ArrayType(Type member) {
        this.member = member;
    }

    @Override
    public EnumSet<RuntimeType> runtimeTypes() {
        return TYPES;
    }

    @Override
    public Type elementType(int index) {
        // TODO: make sure if member is any that this reduces to just any, not any | null
        return Type.unionType(member, Type.nullType());
    }

    @Override
    public String toString() {
        return "array[" + member + "]";
    }
}
