/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.integrations.core;

import software.amazon.smithy.traitcodegen.sections.JavaDocSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;

/**
 * Formats any populated Javadoc comment sections as documentation comments.
 *
 * <p>This interceptor will run after all other Javadoc interceptors to ensure it picks up all content
 * added to Jav doc sections. Javadoc sections with no content are discarded.
 */
public class JavadocFormatterInterceptor implements CodeInterceptor<JavaDocSection, TraitCodegenWriter> {

    @Override
    public Class<JavaDocSection> sectionType() {
        return JavaDocSection.class;
    }

    @Override
    public void write(TraitCodegenWriter writer, String previousText, JavaDocSection section) {
        if (!previousText.isEmpty()) {
            writer.writeDocString(previousText);
        }
    }
}
