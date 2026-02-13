package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.EnumSet;

public final class ArrayType implements Type {

    private static final EnumSet<RuntimeType> TYPES = EnumSet.of(RuntimeType.ARRAY);

    // Never null - array is equivalent to array<any>
    private final Type member;

    public ArrayType(Type member) {
        this.member = member;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArrayType) {
            ArrayType that = (ArrayType) obj;
            return this.member.equals(that.member);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return ArrayType.class.hashCode() + member.hashCode();
    }

    @Override
    public <T> boolean isInstance(T array, JmespathRuntime<T> runtime) {
        if (!runtime.is(array, RuntimeType.ARRAY)) {
            return false;
        }

        for (T value : runtime.asIterable(array)) {
            if (!member.isInstance(value, runtime)) {
                return false;
            }
        }

        return true;
    }


    @Override
    public EnumSet<RuntimeType> runtimeTypes() {
        return TYPES;
    }

    @Override
    public Type elementType(Type index) {
        return elementType();
    }

    @Override
    public Type elementType() {
        return Type.unionType(member, Type.nullType());
    }

    @Override
    public String toString() {
        return "array[" + member + "]";
    }
}
