package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.Function;
import software.amazon.smithy.jmespath.evaluation.FunctionRegistry;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;
import software.amazon.smithy.jmespath.evaluation.NumberType;

import java.util.EnumSet;

// POC of an abstract runtime based on a semi-arbitrary Type value
public class TypeJmespathRuntime implements JmespathRuntime<Type> {

    private final FunctionRegistry<Type> overrides = new FunctionRegistry<>();

    public TypeJmespathRuntime() {
        overrides.registerFunction(new FoldLeftFunction());
    }

    @Override
    public boolean isAbstract() {
        return true;
    }

    public JmespathException abstractException() {
        return new JmespathException("TypeJmespathRuntime is abstract and does not support this operation");
    }

    @Override
    public boolean is(Type value, RuntimeType type) {
        return value.runtimeTypes().equals(EnumSet.of(type));
    }

    @Override
    public RuntimeType typeOf(Type value) {
        throw abstractException();
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
        throw abstractException();
    }

    @Override
    public Type createString(String string) {
        return Type.stringType();
    }

    @Override
    public String asString(Type value) {
        throw abstractException();
    }

    @Override
    public Type createNumber(Number value) {
        return Type.stringType();
    }

    @Override
    public NumberType numberType(Type value) {
        throw abstractException();
    }

    @Override
    public Number asNumber(Type value) {
        throw abstractException();
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
            // TODO: what about map? Need keysOf operation on Type
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
        return new TypeObjectBuilder();
    }

    private static class TypeObjectBuilder implements ObjectBuilder<Type> {

        // Must always be an object type
        private Type type = Type.objectType();

        @Override
        public ObjectBuilder<Type> put(Type key, Type value) {
            // TODO: wrong
            type = Type.arrayType(Type.unionType(type.elementType(), value));
            return this;
        }

        @Override
        public ObjectBuilder<Type> putAll(Type object) {
            // TODO: wrong
            type = Type.unionType(type, object);
            return this;
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
        throw abstractException();
    }

    @Override
    public Iterable<? extends Type> asIterable(Type value) {
        throw abstractException();
    }

    @Override
    public Type createAny(RuntimeType runtimeType) {
        switch (runtimeType) {
            case STRING:
                return Type.stringType();
            case NUMBER:
                return Type.numberType();
            case BOOLEAN:
                return Type.booleanType();
            case NULL:
                return Type.nullType();
            case ARRAY:
                return Type.arrayType();
            case OBJECT:
                return Type.objectType();
            default:
                throw new IllegalArgumentException("Unexpected runtime type: " + runtimeType);
        }
    }

    @Override
    public Type either(Type left, Type right) {
        return Type.unionType(left, right);
    }

    @Override
    public Function<Type> resolveFunction(String name) {
        return overrides.lookup(name);
    }
}
