/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.lang.reflect.Type;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;

/**
 * Exception encountered during deserialization.
 */
public class NodeDeserializationException extends SourceException {
    public NodeDeserializationException(String message, SourceLocation sourceLocation) {
        super(message, sourceLocation);
    }

    public NodeDeserializationException(String message, SourceLocation sourceLocation, Throwable previous) {
        super(message, sourceLocation, previous);
    }

    /**
     * Creates a DeserializationException exception from a caught ReflectiveOperationException.
     *
     * @param into The type being deserialized into.
     * @param pointer The JSON pointer to show the context where the error occurred.
     * @param node The Node that could not be deserialized.
     * @param previous The previous exception that was encountered (or null).
     * @param message The message to use in the exception (or null to include no extra message).
     * @return Returns the created exception.
     */
    static NodeDeserializationException fromReflectiveContext(
            Type into,
            String pointer,
            Node node,
            ReflectiveOperationException previous,
            String message
    ) {
        if (previous.getCause() instanceof NodeDeserializationException) {
            return (NodeDeserializationException) previous.getCause();
        } else {
            return fromContext(into, pointer, node, previous, message);
        }
    }

    /**
     * Creates a DeserializationException exception from context with a formatted message.
     *
     * @param into The type being deserialized into.
     * @param pointer The JSON pointer to show the context where the error occurred.
     * @param node The Node that could not be deserialized.
     * @param previous The previous exception that was encountered (or null).
     * @param message The message to use in the exception (or null to include no extra message).
     * @return Returns the created exception.
     */
    static NodeDeserializationException fromContext(
            Type into,
            String pointer,
            Node node,
            Throwable previous,
            String message
    ) {
        String formattedMessage = NodeMapper.createErrorMessage(into, pointer, node, message);
        return new NodeDeserializationException(formattedMessage, node.getSourceLocation(), previous);
    }
}
