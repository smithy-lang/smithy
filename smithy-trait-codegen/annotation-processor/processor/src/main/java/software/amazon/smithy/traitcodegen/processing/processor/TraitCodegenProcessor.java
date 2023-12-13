/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.processing.processor;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.processor.SmithyAnnotationProcessor;
import software.amazon.smithy.traitcodegen.processing.annotations.GenerateTraits;

/**
 * Annotation processor for executing the trait-codegen smithy build plugin.
 */
@SupportedAnnotationTypes("software.amazon.smithy.traitcodegen.processing.annotations.GenerateTraits")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TraitCodegenProcessor extends SmithyAnnotationProcessor<GenerateTraits> {
    @Override
    protected String getPluginName() {
        return "trait-codegen";
    }

    @Override
    protected Class<GenerateTraits> getAnnotationClass() {
        return GenerateTraits.class;
    }

    @Override
    protected ObjectNode createPluginNode(GenerateTraits annotation) {
        return Node.objectNodeBuilder()
                .withMember("package", annotation.packageName())
                .withMember("header", Node.fromStrings(annotation.header()))
                .withMember("excludeTags", Node.fromStrings(annotation.excludeTags()))
                .build();
    }
}
