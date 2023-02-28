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

public final class ArrayType extends AbstractType {
    private final Type member;

    ArrayType(Type member) {
        this.member = member;
    }

    public Type getMember() {
        return member;
    }

    @Override
    public ArrayType expectArrayType() {
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(member);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        ArrayType that = (ArrayType) obj;
        return Objects.equals(this.member, that.member);
    }

    @Override
    public String toString() {
        return String.format("[%s]", this.member);
    }

}
