package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.JmespathException;

public interface Type {

    static Type optional(Type type) {
        return new UnionType(type, NullType.INSTANCE);
    }

    default boolean isArray() {
        return false;
    }

    default ArrayType expectArray() {
        throw new JmespathException("not an array");
    }
}
