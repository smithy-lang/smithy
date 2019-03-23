/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;

/**
 * Trait implementation that expects a boolean value of true.
 */
public abstract class BooleanTrait extends AbstractTrait {
    public BooleanTrait(String name, SourceLocation sourceLocation) {
        super(name, sourceLocation);
    }

    @Override
    protected final Node createNode() {
        return new BooleanNode(true, getSourceLocation());
    }
}
