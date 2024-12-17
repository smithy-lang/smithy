/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
    NULL {
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
