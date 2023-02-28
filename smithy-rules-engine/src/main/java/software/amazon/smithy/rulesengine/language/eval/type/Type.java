/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.rulesengine.language.eval.type;

import java.util.List;
import java.util.Map;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public interface Type {
    default boolean isA(Type type) {
        return type.equals(this);
    }

    /**
     * When used in the context of a condition, the condition can only match if the value was truthful. This means
     * that a certain expression can be a different type, for example, {@code OptionalType<T>} will become {@code T}.
     *
     * @return The type, given that it has been proven truthy
     */
    default Type provenTruthy() {
        return this;
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
