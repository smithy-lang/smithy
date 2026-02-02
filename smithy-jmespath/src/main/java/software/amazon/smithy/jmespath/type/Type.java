package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.RuntimeType;

import java.util.Arrays;
import java.util.EnumSet;

public interface Type {

    static Type optionalType(Type type) {
        return new UnionType(type, NullType.INSTANCE);
    }

    static Type anyType() { return AnyType.INSTANCE; }

    static Type nullType() { return NullType.INSTANCE; }

    static Type unionType(Type ... types) {
        return new UnionType(types);
    }

    EnumSet<RuntimeType> runtimeTypes();

    default Type elementType(int index) {
        return Type.nullType();
    }

    default Type valueType(String key) {
        return Type.nullType();
    }

    default boolean isArray() {
        return false;
    }

    default ArrayType expectArray() {
        throw new JmespathException("not an array");
    }
}
