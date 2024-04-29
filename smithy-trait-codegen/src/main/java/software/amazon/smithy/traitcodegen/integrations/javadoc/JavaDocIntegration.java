/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.integrations.javadoc;

import java.util.List;
import software.amazon.smithy.traitcodegen.TraitCodegenContext;
import software.amazon.smithy.traitcodegen.integrations.TraitCodegenIntegration;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds all built-in Javadoc-generating interceptors.
 *
 * <p>This integration adds all the required documentation interceptors that ensure
 * that methods, classes, and properties all have JavaDocs added. This integration also
 * adds Annotations such as {@code @Deprecated} that serve as documentation.
 */
@SmithyInternalApi
public final class JavaDocIntegration implements TraitCodegenIntegration  {

    @Override
    public String name() {
        return "javadoc";
    }

    @Override
    public List<? extends CodeInterceptor<? extends CodeSection, TraitCodegenWriter>> interceptors(
            TraitCodegenContext codegenContext) {
        return ListUtils.of(
                new SmithyGeneratedInterceptor(),
                new JavadocInjectorInterceptor(),
                new ExternalDocumentationInterceptor(),
                new SinceInterceptor(),
                new DeprecatedInterceptor(),
                new DocumentationTraitInterceptor()
        );
    }
}
