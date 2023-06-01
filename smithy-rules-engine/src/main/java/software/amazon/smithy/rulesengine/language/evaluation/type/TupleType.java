/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.evaluation.type;

import java.util.List;
import java.util.Objects;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;

public final class TupleType extends AbstractType {
    private final List<Type> memberTypes;

    public TupleType(List<Type> memberTypes) {
        this.memberTypes = memberTypes;
    }

    public List<Type> getMemberTypes() {
        return memberTypes;
    }

    @Override
    public TupleType expectTupleType() throws InnerParseError {
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
        TupleType that = (TupleType) obj;
        return Objects.equals(this.memberTypes, that.memberTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberTypes);
    }

    @Override
    public String toString() {
        return memberTypes.toString();
    }
}
