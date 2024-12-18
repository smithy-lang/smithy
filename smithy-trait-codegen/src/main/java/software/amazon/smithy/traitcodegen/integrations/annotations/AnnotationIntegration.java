/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.integrations.annotations;

import java.util.List;
import software.amazon.smithy.traitcodegen.TraitCodegenContext;
import software.amazon.smithy.traitcodegen.integrations.TraitCodegenIntegration;
import software.amazon.smithy.traitcodegen.integrations.javadoc.JavaDocIntegration;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.ListUtils;

/**
 * Adds Java annotations to generated Java classes.
 */
public class AnnotationIntegration implements TraitCodegenIntegration {
    @Override
    public String name() {
        return "annotations";
    }

    @Override
    public List<String> runBefore() {
        return ListUtils.of(JavaDocIntegration.NAME);
    }

    @Override
    public List<? extends CodeInterceptor<? extends CodeSection, TraitCodegenWriter>> interceptors(
            TraitCodegenContext codegenContext
    ) {
        return ListUtils.of(
                new SmithyGeneratedAnnotationInterceptor(),
                new DeprecatedAnnotationInterceptor(),
                new UnstableAnnotationInterceptor());
    }
}
