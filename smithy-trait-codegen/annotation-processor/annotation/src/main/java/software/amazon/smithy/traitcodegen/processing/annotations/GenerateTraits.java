/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.processing.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation used to trigger annotation processor generating traits.
 */
@Target(ElementType.PACKAGE)
public @interface GenerateTraits {
    /**
     * Package name for generated code.
     */
    String packageName();

    /**
     * File header to add to all generated code files.
     *
     * <p>For example, a license header that should be included in
     * all files.
     */
    String[] header();

    /**
     * List of tags to exclude from trait code generation.
     */
    String[] excludeTags() default {};
}
