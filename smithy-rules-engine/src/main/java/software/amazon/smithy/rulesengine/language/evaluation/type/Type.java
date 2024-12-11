/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.evaluation.type;

import java.util.List;
import java.util.Map;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Types used to construct conditions out of values in the rules engine.
 */
@SmithyUnstableApi
public interface Type {
    /**
     * Returns true if this type matches the target type.
     *
     * @param type the target type to match.
     * @return true if the types match, false otherwise.
     */
    default boolean isA(Type type) {
        return type.equals(this);
    }

    /**
     * When used in the context of a condition, the condition can only match if the
     * value was truthful. This means that a certain expression can be a different
     * type, for example, {@code OptionalType<T>} will become {@code T}.
     *
     * @return The type, given that it has been proven truthy
     */
    default Type provenTruthy() {
        return this;
    }

    static Type fromParameterType(ParameterType parameterType) {
        if (parameterType == ParameterType.STRING) {
            return stringType();
        }
        if (parameterType == ParameterType.BOOLEAN) {
            return booleanType();
        }
        if (parameterType == ParameterType.STRING_ARRAY) {
            return arrayType(stringType());
        }
        throw new IllegalArgumentException("Unexpected parameter type: " + parameterType);
    }

    static AnyType anyType() {
        return new AnyType();
    }

    static ArrayType arrayType(Type inner) {
        return new ArrayType(inner);
    }

    static BooleanType booleanType() {
        return new BooleanType();
    }

    static EmptyType emptyType() {
        return new EmptyType();
    }

    static EndpointType endpointType() {
        return new EndpointType();
    }

    static IntegerType integerType() {
        return new IntegerType();
    }

    static OptionalType optionalType(Type type) {
        return new OptionalType(type);
    }

    static RecordType recordType(Map<Identifier, Type> inner) {
        return new RecordType(inner);
    }

    static StringType stringType() {
        return new StringType();
    }

    static TupleType tupleType(List<Type> members) {
        return new TupleType(members);
    }

    default AnyType expectAnyType() throws InnerParseError {
        throw new InnerParseError("Expected any but found " + this);
    }

    default ArrayType expectArrayType() throws InnerParseError {
        throw new InnerParseError("Expected array but found " + this);
    }

    default BooleanType expectBooleanType() throws InnerParseError {
        throw new InnerParseError("Expected boolean but found " + this);
    }

    default EmptyType expectEmptyType() throws InnerParseError {
        throw new InnerParseError("Expected empty but found " + this);
    }

    default EndpointType expectEndpointType() throws InnerParseError {
        throw new InnerParseError("Expected endpoint but found " + this);
    }

    default IntegerType expectIntegerType() throws InnerParseError {
        throw new InnerParseError("Expected integer but found " + this);
    }

    default OptionalType expectOptionalType() throws InnerParseError {
        throw new InnerParseError("Expected optional but found " + this);
    }

    default RecordType expectRecordType(String message) throws InnerParseError {
        throw new InnerParseError(String.format("Expected record but found %s%n == hint: %s", this, message));
    }

    default StringType expectStringType() throws InnerParseError {
        throw new InnerParseError("Expected string but found " + this);
    }

    default TupleType expectTupleType() throws InnerParseError {
        throw new InnerParseError("Expected tuple but found " + this);
    }
}
