/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Provides access to typed metadata by type.
 */
public final class TypedMetadataIndex implements KnowledgeIndex {
    private final Map<ShapeId, List<TypedMetadata>> byType;

    private TypedMetadataIndex(Model model) {
        this.byType = collect(model);
    }

    public static TypedMetadataIndex of(Model model) {
        return model.getKnowledge(TypedMetadataIndex.class, TypedMetadataIndex::new);
    }

    /**
     * Get all typed metadata by type.
     *
     * @return All typed metadata, keyed by type.
     */
    public Map<ShapeId, List<TypedMetadata>> getAllByType() {
        return byType;
    }

    private static Map<ShapeId, List<TypedMetadata>> collect(Model model) {
        Map<ShapeId, List<TypedMetadata>> byType = new HashMap<>();

        for (Map.Entry<String, Node> entry : model.getMetadata().entrySet()) {
            if (entry.getValue().isObjectNode()) {
                ObjectNode node = entry.getValue().expectObjectNode();
                String typeMember = node.getStringMemberOrDefault("__type__", null);
                if (typeMember != null) {
                    ShapeId typeId = ShapeId.from(typeMember);
                    TypedMetadata typedMetadata = new TypedMetadata(
                            entry.getKey(),
                            typeId,
                            node);
                    byType.computeIfAbsent(typeId, (ignored) -> new ArrayList<>())
                            .add(typedMetadata);
                }
            }
        }

        byType.replaceAll((shapeId, typedMetadata) -> Collections.unmodifiableList(typedMetadata));
        return Collections.unmodifiableMap(byType);
    }

    public static final class TypedMetadata {
        private final String key;
        private final ShapeId typeId;
        private final ObjectNode value;

        private TypedMetadata(String key, ShapeId typeId, ObjectNode value) {
            this.key = key;
            this.typeId = typeId;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public ShapeId getTypeId() {
            return typeId;
        }

        public ObjectNode getValue() {
            return value;
        }
    }
}
