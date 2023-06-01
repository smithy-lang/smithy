/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.evaluation.type;

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
    public int hashCode() {
        return Objects.hash(member);
    }

    @Override
    public String toString() {
        return String.format("[%s]", member);
    }
}
