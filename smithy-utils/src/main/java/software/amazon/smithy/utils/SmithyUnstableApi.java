/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation used to indicate that an API is considered unstable and subject
 * to change.
 *
 * <p>Breaking changes may be introduced to elements marked as {@link SmithyUnstableApi}.
 * Users of Smithy may implement and use APIs marked as unstable with the caveat that
 * they may need to make changes to their implementations as unstable APIs evolve.
 */
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface SmithyUnstableApi {}
