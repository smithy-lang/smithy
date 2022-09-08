/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.language.visit;

import software.amazon.smithy.rulesengine.language.syntax.fn.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.fn.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.fn.Not;
import software.amazon.smithy.rulesengine.language.syntax.fn.StandardLibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.fn.StringEquals;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public interface FnVisitor<R> {

    R visitBoolEquals(BooleanEquals fn);

    R visitStringEquals(StringEquals fn);

    R visitIsSet(IsSet fn);

    R visitNot(Not not);

    R visitGenericFunction(StandardLibraryFunction fn);
}
