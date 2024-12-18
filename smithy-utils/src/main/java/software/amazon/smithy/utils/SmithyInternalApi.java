/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation used to indicate that an API is considered internal to Smithy
 * and subject to change. Breaking changes can and will be introduced to
 * elements marked as {@link SmithyInternalApi}. Users of Smithy should not
 * depend on any packages, types, fields, constructors, or methods with this
 * annotation.
 */
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface SmithyInternalApi {}
