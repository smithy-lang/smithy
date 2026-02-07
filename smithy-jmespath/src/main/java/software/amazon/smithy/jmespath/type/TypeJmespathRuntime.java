package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;
import software.amazon.smithy.jmespath.evaluation.NumberType;

import java.util.Arrays;
import java.util.function.BiFunction;

public class TypeJmespathRuntime implements JmespathRuntime<Type> {
    @Override
    public RuntimeType typeOf(Type value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type ifThenElse(Type condition, Type then, Type otherwise) {
        // TODO: If we have a LiteralType, we can sometimes determine the result is just then or otherwise
        return Type.unionType(then, otherwise);
    }

    @Override
    public Type createNull() {
        return Type.nullType();
    }

    @Override
    public Type createBoolean(boolean b) {
        return Type.booleanType();
    }

    @Override
    public boolean asBoolean(Type value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type createString(String string) {
        return Type.stringType();
    }

    @Override
    public String asString(Type value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type createNumber(Number value) {
        return Type.stringType();
    }

    @Override
    public NumberType numberType(Type value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Number asNumber(Type value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArrayBuilder<Type> arrayBuilder() {
        return new TypeArrayBuilder();
    }

    private static class TypeArrayBuilder implements ArrayBuilder<Type> {

        // Must always be an array type
        private Type type = Type.arrayType(Type.bottomType());

        @Override
        public ArrayBuilder<Type> add(Type value) {
            type = Type.arrayType(Type.unionType(type.elementType(), value));
            return this;
        }

        @Override
        public ArrayBuilder<Type> addAll(Type collection) {
            // TODO: what about map?
            type = Type.unionType(type, collection);
            return this;
        }

        @Override
        public Type build() {
            return type;
        }
    }

    @Override
    public Type element(Type array, int index) {
        return array.elementType(index);
    }

    @Override
    public ObjectBuilder<Type> objectBuilder() {
        return null;
    }

    private static class TypeObjectBuilder implements ObjectBuilder<Type> {

        // Must always be an object type
        private Type type = Type.objectType();

        @Override
        public void put(Type key, Type value) {
            // TODO: wrong
            type = Type.arrayType(Type.unionType(type.elementType(), value));
        }

        @Override
        public void putAll(Type object) {
            // TODO: wrong
            type = Type.unionType(type, object);
        }

        @Override
        public Type build() {
            return type;
        }
    }

    @Override
    public Type value(Type object, Type key) {
        return object.valueType(key);
    }

    @Override
    public int length(Type value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<? extends Type> asIterable(Type value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type foldLeft(Type init, JmespathExpression f, Type array) {
        // "evaluate" f in a typing context of [init, array.elementType()]
        // and determine the fix point
        // TODO: If `array` is more specific (say a @length limit) we may not need to do that.
        // TODO: This may actually not terminate in some cases, say if init is a TupleType
        // and f extends the tuple.
        // But we could detect that and convert it to an ArrayType first, which won't grow in the same way.
        Type result = init;
        Type prevResult = null;
        while (!result.equals(prevResult)) {
            Type fContextType = new TupleType(Arrays.asList(prevResult, array.elementType()));
            prevResult = result;
            result = f.evaluate(fContextType, this);
        }
        return result;
    }
}
