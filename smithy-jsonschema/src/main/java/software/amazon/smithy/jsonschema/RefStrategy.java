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

package software.amazon.smithy.jsonschema;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.utils.StringUtils;

/**
 * Defines a strategy for converting Shape IDs to JSON schema $ref values.
 */
@FunctionalInterface
public interface RefStrategy {
    String DEFAULT_POINTER = "#/definitions";

    /**
     * Given a shape ID, returns the value used in a $ref to refer to it.
     *
     * <p>The return value is expected to be a JSON pointer.
     *
     * @param id Shape ID to convert to a $ref string.
     * @param config JSON schema configuration to use.
     * @return Returns the $ref string (e.g., "#/responses/MyShape").
     */
    String toPointer(ShapeId id, ObjectNode config);

    /**
     * Creates a default ref strategy that calls a delegate and deconflicts
     * pointers by appending an incrementing number to conflicting
     * IDs.
     *
     * @param index Shape index to use to deconflict shapes.
     * @param config JSON schema configuration to use.
     * @return Returns the created strategy.
     * @see #createDefaultStrategy()
     * @see #createDeconflictingStrategy(ShapeIndex, ObjectNode, RefStrategy)
     */
    static RefStrategy createDefaultDeconflictingStrategy(ShapeIndex index, ObjectNode config) {
        return createDeconflictingStrategy(index, config, createDefaultStrategy());
    }

    /**
     * Creates a default strategy for converting shape IDs to $refs.
     *
     * <p>This default strategy will make the created value consist
     * of only alphanumeric characters. When a namespace is included
     * (because "stripNamespaces" is not set), the namespace is added
     * to the beginning of the created name by capitalizing the first
     * letter of each part of the namespace, removing the "."
     * (for example, "smithy.example" becomes "SmithyExample"). Next,
     * the shape name is appended. Finally, if a member name is present,
     * the member name with a capitalized first letter is appended
     * followed by the literal string "Member" if the member name does
     * not already case-insensitively end in "member".
     *
     * <p>For example, given the following shape ID "smithy.example#Foo$baz",
     * the following ref is created "#/definitions/SmithyExampleFooBazMember".
     *
     * <p>This implementation honors the value configured in
     * {@link JsonSchemaConstants#DEFINITION_POINTER} to create a $ref
     * pointer to a shape.
     *
     * @return Returns the created strategy.
     */
    static RefStrategy createDefaultStrategy() {
        return (id, config) -> {
            StringBuilder builder = new StringBuilder();

            String pointer = config.getStringMemberOrDefault(JsonSchemaConstants.DEFINITION_POINTER, DEFAULT_POINTER);
            builder.append(pointer);
            if (!pointer.endsWith("/")) {
                builder.append('/');
            }

            if (!config.containsMember(JsonSchemaConstants.SMITHY_STRIP_NAMESPACES)) {
                // Append each namespace part, capitalizing each segment.
                // For example, "smithy.example" becomes "SmithyExample".
                for (String part : id.getNamespace().split("\\.")) {
                    builder.append(StringUtils.capitalize(part));
                }
            }

            builder.append(id.getName());
            id.getMember().ifPresent(memberName -> {
                // Append the capitalized member name followed by "Member" IFF
                // the member name doesn't already end in "member".
                builder.append(StringUtils.capitalize(memberName));
                if (!memberName.toLowerCase(Locale.US).endsWith("member")) {
                    builder.append("Member");
                }
            });

            return builder.toString();
        };
    }

    /**
     * Creates a ref strategy that calls a delegate and de-conflicts
     * pointers by appending an incrementing number to conflicting
     * IDs.
     *
     * <p>To make the generated IDs deterministic, shapes are sorted by shape
     * ID when performing comparisons.
     *
     * @param index Shape index to use to de-conflict shapes.
     * @param config JSON schema configuration to use.
     * @param delegate Ref strategy to call to and then de-conflict.
     * @return Returns the created strategy.
     */
    static RefStrategy createDeconflictingStrategy(ShapeIndex index, ObjectNode config, RefStrategy delegate) {
        Map<ShapeId, String> pointers = new HashMap<>();
        Map<String, ShapeId> reversePointers = new HashMap<>();

        index.shapes().sorted().forEach(shape -> {
            String pointer = delegate.toPointer(shape.getId(), config);

            if (!reversePointers.containsKey(pointer)) {
                pointers.put(shape.getId(), pointer);
                reversePointers.put(pointer, shape.getId());
                return;
            }

            for (int i = 2; ; i++) {
                String incrementedPointer = pointer + i;
                if (!reversePointers.containsKey(incrementedPointer)) {
                    pointers.put(shape.getId(), incrementedPointer);
                    reversePointers.put(incrementedPointer, shape.getId());
                    return;
                }
            }
        });

        return (id, cfg) -> pointers.computeIfAbsent(id, i -> delegate.toPointer(i, cfg));
    }
}
