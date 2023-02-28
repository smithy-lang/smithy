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

public final class EmptyValue extends Value {
    public EmptyValue() {
        super(SourceLocation.none());
    }

    @Override
    public Type getType() {
        return Type.emptyType();
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public Node toNode() {
        return Node.nullNode();
    }
}
