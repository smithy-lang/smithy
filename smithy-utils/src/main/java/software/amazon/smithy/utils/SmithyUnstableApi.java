/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
public @interface SmithyUnstableApi {
}
