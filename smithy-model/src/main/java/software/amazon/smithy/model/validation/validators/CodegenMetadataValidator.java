/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.knowledge.TypedMetadataIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates metadata with the {@code smithy.api#Codegen} type.
 */
public final class CodegenMetadataValidator extends AbstractValidator {
    private static final ShapeId CODEGEN_METADATA = ShapeId.from("smithy.api#Codegen");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        TypedMetadataIndex index = TypedMetadataIndex.of(model);

        // NOTE: The validation here is limited. Notably, it doesn't check for conflicting names across namespaces,
        //       which is usually desirable for codegen. However, we haven't decided if we actually want to 'flatten'
        //       all shapes and have the code generator generate them all in the same 'namespace' (or language
        //       equivalent, where applicable). This decision could affect what validation we need to do here.
        for (TypedMetadataIndex.TypedMetadata metadata : index.getAllByType()
                .getOrDefault(CODEGEN_METADATA, ListUtils.of())) {
            if (metadata.getValue().containsMember("includeNamespaces")) {
                for (Node node : metadata.getValue().expectArrayMember("includeNamespaces")) {
                    String namespace = node.expectStringNode().getValue();
                    if (!ShapeId.isValidNamespace(namespace)) {
                        events.add(ValidationEvent.builder()
                                .id("CodegenMetadata.InvalidNamespace")
                                .shapeId(CODEGEN_METADATA)
                                .severity(Severity.ERROR)
                                .sourceLocation(node)
                                .message("`" + namespace + "` is not a valid namespace.")
                                .build());
                    }
                }
            }

            if (metadata.getValue().containsMember("includeBySelector")) {
                try {
                    Selector ignored = Selector.fromNode(metadata.getValue().expectStringMember("includeBySelector"));
                } catch (SourceException e) {
                    events.add(ValidationEvent.fromSourceException(e));
                }
            }
        }

        return events;
    }
}
