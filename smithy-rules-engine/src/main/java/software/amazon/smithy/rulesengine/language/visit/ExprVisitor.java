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

import software.amazon.smithy.rulesengine.language.syntax.expr.Literal;
import software.amazon.smithy.rulesengine.language.syntax.expr.Ref;
import software.amazon.smithy.rulesengine.language.syntax.fn.Fn;
import software.amazon.smithy.rulesengine.language.syntax.fn.GetAttr;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public interface ExprVisitor<R> {
    R visitLiteral(Literal literal);

    R visitRef(Ref ref);

    R visitFn(Fn fn);

    R visitGetAttr(GetAttr getAttr);

    abstract class Default<R> implements ExprVisitor<R> {
        public abstract R getDefault();

        @Override
        public R visitLiteral(Literal literal) {
            return getDefault();
        }

        @Override
        public R visitRef(Ref ref) {
            return getDefault();
        }

        public R visitGetAttr(GetAttr getAttr) {
            return getDefault();
        }

        @Override
        public R visitFn(Fn fn) {
            return getDefault();
        }
    }
}
