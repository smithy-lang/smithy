package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.EnumSet;

public interface Type {

    static Type optionalType(Type type) {
        return new UnionType(type, NullType.INSTANCE);
    }

    static Type anyType() { return AnyType.INSTANCE; }

    static Type bottomType() {
        return BottomType.INSTANCE;
    }

    static Type nullType() { return NullType.INSTANCE; }

    static Type booleanType() { return BooleanType.INSTANCE; }

    static Type stringType() { return StringType.INSTANCE; }

    static Type numberType() { return NumberType.INSTANCE; }

    static Type arrayType() { return new ArrayType(anyType()); }

    static Type arrayType(Type elementType) { return new ArrayType(elementType); }

    static Type objectType() { return new ObjectType(null); }

    static Type unionType(Type ... types) {
        return new UnionType(types);
    }

    <T> boolean isInstance(T value, JmespathRuntime<T> runtime);

    EnumSet<RuntimeType> runtimeTypes();

    default Type elementType() {
        return Type.nullType();
    }

    default Type elementType(int index) {
        return elementType();
    }

    default Type valueType(Type key) {
        return Type.nullType();
    }

    default boolean isArray() {
        return false;
    }

    default ArrayType expectArray() {
        throw new JmespathException("not an array");
    }
}
