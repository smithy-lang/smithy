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

package software.amazon.smithy.model.node;

import java.util.Locale;

/** The set of possible node types. */
public enum NodeType {
    OBJECT {
        @Override
        public Class<? extends Node> getNodeClass() {
            return ObjectNode.class;
        }
    },
    ARRAY {
        @Override
        public Class<? extends Node> getNodeClass() {
            return ArrayNode.class;
        }
    },
    STRING {
        @Override
        public Class<? extends Node> getNodeClass() {
            return StringNode.class;
        }
    },
    NUMBER {
        @Override
        public Class<? extends Node> getNodeClass() {
            return NumberNode.class;
        }
    },
    BOOLEAN {
        @Override
        public Class<? extends Node> getNodeClass() {
            return BooleanNode.class;
        }
    },
    NULL  {
        @Override
        public Class<? extends Node> getNodeClass() {
            return NullNode.class;
        }
    };

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.US);
    }

    /**
     * Gets the {@code Node} class associated with the type.
     *
     * @return Returns the Node class.
     */
    public abstract Class<? extends Node> getNodeClass();
}
