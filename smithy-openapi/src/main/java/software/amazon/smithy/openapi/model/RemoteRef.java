/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.model;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.openapi.OpenApiException;

final class RemoteRef<T extends ToNode> extends Ref<T> {
    private final String pointer;

    RemoteRef(String pointer) {
        this.pointer = pointer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T deref(ComponentsObject components) {
        if (!pointer.startsWith("#/")) {
            throw new OpenApiException("Cannot deref a remove pointer: " + pointer);
        }

        String[] paths = pointer.substring(2).split("/");

        if (!paths[0].equals("components")) {
            throw new OpenApiException(
                    "Cannot deref a path that does not start with components: " + pointer);
        } else if (paths.length != 2) {
            throw new OpenApiException(
                    "Cannot deref a path that does not have exactly two segments: " + pointer);
        }

        Object result;
        switch (paths[1]) {
            case "schemas":
                result = components.getSchemas().get(paths[1]);
                break;
            case "responses":
                result = components.getResponses().get(paths[1]);
                break;
            case "parameters":
                result = components.getParameters().get(paths[1]);
                break;
            case "requestBodies":
                result = components.getRequestBodies().get(paths[1]);
                break;
            case "headers":
                result = components.getHeaders().get(paths[1]);
                break;
            case "securitySchemes":
                result = components.getSecuritySchemes().get(paths[1]);
                break;
            case "links":
                result = components.getLinks().get(paths[1]);
                break;
            case "callbacks":
                result = components.getCallbacks().get(paths[1]);
                break;
            default:
                throw new OpenApiException(paths[1] + " is an unsupported component: " + pointer);
        }

        if (result == null) {
            throw new OpenApiException("$ref pointer reference targets an unreachable component: " + pointer);
        }

        try {
            return (T) result;
        } catch (ClassCastException e) {
            throw new OpenApiException(String.format(
                    "$ref pointer `%s` pointer to a value of an unexpected type, %s: %s",
                    pointer,
                    result.getClass().getName(),
                    result));
        }
    }

    @Override
    public Optional<String> getPointer() {
        return Optional.ofNullable(pointer);
    }

    public String toString() {
        return pointer;
    }

    @Override
    public Node toNode() {
        return Node.objectNode().withMember("$ref", Node.from(pointer));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof RemoteRef)) {
            return false;
        }

        RemoteRef ref = (RemoteRef) o;
        return Objects.equals(pointer, ref.pointer);
    }

    @Override
    public int hashCode() {
        return pointer.hashCode();
    }
}
