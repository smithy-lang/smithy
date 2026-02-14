package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.FunctionArgument;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.EnumSet;
import java.util.Set;

public interface Type extends FunctionArgument<Type> {

    static Type anyType() { return AnyType.INSTANCE; }

    static Type bottomType() {
        return BottomType.INSTANCE;
    }

    static Type nullType() { return new JustRuntimeType(RuntimeType.NULL); }

    static Type booleanType() { return new JustRuntimeType(RuntimeType.BOOLEAN); }

    static Type stringType() { return new JustRuntimeType(RuntimeType.STRING); }

    static Type numberType() { return new JustRuntimeType(RuntimeType.NUMBER); }

    static Type arrayType() { return new ArrayType(anyType()); }

    static Type arrayType(Type elementType) { return new ArrayType(elementType); }

    static Type objectType() { return new ObjectType(null); }

    static Type unionType(Type ... types) {
        return new UnionType(types);
    }

    <T> boolean isInstance(T value, JmespathRuntime<T> runtime);

    default Type elementType() {
        return Type.nullType();
    }

    default Type elementType(Type index) {
        return elementType();
    }

    default Type valueType(Type key) {
        return Type.nullType();
    }

    default boolean isArray() {
        return false;
    }

    @Override
    default Type expectValue() {
        return expectAnyOf(RuntimeType.valueTypes());
    }

    @Override
    default Type expectType(RuntimeType type) {
        return expectAnyOf(EnumSet.of(type));
    }
}
