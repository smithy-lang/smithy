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

package software.amazon.smithy.rulesengine.language.eval.value;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.eval.type.Type;

public final class IntegerValue extends Value {
    private final int value;

    IntegerValue(int value) {
        super(SourceLocation.none());
        this.value = value;
    }

    @Override
    public Type getType() {
        return Type.integerType();
    }

    @Override
    public IntegerValue expectIntegerValue() {
        return this;
    }

    public int getValue() {
        return value;
    }

    @Override
    public Node toNode() {
        return Node.from(value);
    }
}
