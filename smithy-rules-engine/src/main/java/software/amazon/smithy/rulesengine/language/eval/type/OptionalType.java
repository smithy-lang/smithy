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

import java.util.Objects;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;

public final class OptionalType extends AbstractType {
    private final Type inner;

    public OptionalType(Type inner) {
        this.inner = inner;
    }

    @Override
    public StringType expectStringType() throws InnerParseError {
        throw new InnerParseError(String.format("Expected string but found %s. hint: use `assign` in a condition "
                + "or `isSet` to prove that this value is non-null", this));
    }

    @Override
    public BooleanType expectBooleanType() throws InnerParseError {
        throw new InnerParseError(String.format("Expected boolean but found %s. hint: use `isSet` to convert "
                + "OptionalType<BooleanType> to bool", this));
    }

    @Override
    public OptionalType expectOptionalType() {
        return this;
    }

    @Override
    public boolean isA(Type type) {
        if (!(type instanceof OptionalType)) {
            return false;
        }
        return ((OptionalType) type).inner.isA(inner);
    }

    @Override
    public Type provenTruthy() {
        return inner;
    }

    public Type inner() {
        return inner;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inner);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        OptionalType that = (OptionalType) obj;
        return Objects.equals(this.inner, that.inner);
    }

    @Override
    public String toString() {
        return String.format("OptionalType<%s>", inner);
    }

}
