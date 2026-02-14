package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.RuntimeType;

public interface JmespathAbstractRuntime<T> {

    ///////////////////////////////
    // General Operations
    ///////////////////////////////

    T abstractTypeOf(T value);

    T abstractIs(T value, RuntimeType type);

    T abstractEqual(T a, T b);

    T abstractLessThan(T a, T b);

    T abstractToString(T value);

    ///////////////////////////////
    // Arbitrary values
    ///////////////////////////////

    // Throws unsupported if the runtime is concrete
    T createAny(RuntimeType runtimeType);

    // Throws unsupported if the runtime is concrete
    T either(T left, T right);

    ///////////////////////////////
    // NULLs
    ///////////////////////////////

    /**
     * Returns `null`.
     * <p>
     * Runtimes may or may not use a Java null value to represent a JSON null value.
     */
    T createNull();

    ///////////////////////////////
    // BOOLEANs
    ///////////////////////////////

    /**
     * Creates a BOOLEAN value.
     */
    T createBoolean(boolean b);

    ///////////////////////////////
    // STRINGs
    ///////////////////////////////

    /**
     * Creates a STRING value.
     */
    T createString(String string);

    ///////////////////////////////
    // NUMBERs
    ///////////////////////////////

    /**
     * Creates a NUMBER value.
     */
    T createNumber(Number value);

    ///////////////////////////////
    // ARRAYs
    ///////////////////////////////

    /**
     * Creates a new ArrayBuilder.
     */
    // TODO: Default implementation of wrapping an immutable array value and using append and concat?
    JmespathRuntime.ArrayBuilder<T> arrayBuilder();

    /**
     * A builder interface for new ARRAY values.
     */
    interface ArrayBuilder<T> {

        /**
         * Adds the given value to the array being built.
         */
        JmespathRuntime.ArrayBuilder<T> add(T value);

        /**
         * If the given value is an ARRAY, adds all the elements of the array.
         * If the given value is an OBJECT, adds all the keys of the object.
         * Otherwise, throws a JmespathException of type INVALID_TYPE.
         */
        JmespathRuntime.ArrayBuilder<T> addAll(T collection);

        /**
         * Builds the new ARRAY value being built.
         */
        T build();
    }

    /**
     * If the given value is an ARRAY, returns the element at the given index.
     * Otherwise, throws a JmespathException of type INVALID_TYPE.
     */
    T element(T array, int index);

    T abstractElement(T array, T index);

    ///////////////////////////////
    // OBJECTs
    ///////////////////////////////

    /**
     * Creates a new ObjectBuilder.
     */
    // TODO: Default implementation of wrapping an immutable object value and using merge?
    // Don't want any concrete runtime to use that though.
    JmespathRuntime.ObjectBuilder<T> objectBuilder();

    /**
     * A builder interface for new OBJECT values.
     */
    interface ObjectBuilder<T> {

        /**
         * Adds the given key/value pair to the object being built.
         */
        JmespathRuntime.ObjectBuilder<T> put(T key, T value);

        /**
         * If the given value is an OBJECT, adds all of its key/value pairs.
         * Otherwise, throws a JmespathException of type INVALID_TYPE.
         */
        JmespathRuntime.ObjectBuilder<T> putAll(T object);

        /**
         * Builds the new OBJECT value being built.
         */
        T build();
    }

    /**
     * If the given value is an OBJECT, returns the value mapped to the given key.
     * Otherwise, returns NULL.
     */
    T value(T object, T key);

    ///////////////////////////////
    // Common collection operations for ARRAYs and OBJECTs
    ///////////////////////////////34e

    T abstractLength(T value);

    ///////////////////////////////
    // Functions
    ///////////////////////////////

    /**
     * Resolve a function expression.
     * The runtime can provide more optimized implementations of specific functions,
     * or more abstracted versions for abstract runtimes.
     * It can also provide runtime-native functions.
     *
     * @return
     */
    default Function<T> resolveFunction(String name) {
        return null;
    }

    FunctionArgument<T> createFunctionArgument(T value);

    FunctionArgument<T> createFunctionArgument(JmespathExpression expression);

    ///////////////////////////////
    // Errors
    ///////////////////////////////

    // Throws the error immediately if the runtime is concrete
    T createError(JmespathExceptionType type, String message);

    // Throws unsupported if the runtime is concrete
    T createExpression(JmespathExpression expression);
}
