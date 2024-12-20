/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation used to indicate that a package or class was generated and
 * should not be edited directly.
 */
@Target({ElementType.PACKAGE, ElementType.TYPE})
public @interface SmithyGenerated {}
