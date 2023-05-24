/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.syntax;

import software.amazon.smithy.model.loader.ModelSyntaxException;

// NamespaceStatement = %s"namespace" SP Namespace BR
final class NamespaceStatementParser {

    private final CapturingTokenizer tokenizer;

    NamespaceStatementParser(CapturingTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    void parse() {
        if (tokenizer.isCurrentLexeme("namespace")) {
            tokenizer.withState(TreeType.NAMESPACE_STATEMENT, () -> {
                tokenizer.next(); // skip "namespace"
                tokenizer.expectAndSkipSpaces();
                tokenizer.skipShapeIdNamespace();
                tokenizer.expectAndSkipBr();
            });
        } else if (tokenizer.hasNext()) {
            throw new ModelSyntaxException(
                    "Expected a namespace definition but found "
                    + tokenizer.getCurrentToken().getDebug(tokenizer.getCurrentTokenLexeme()),
                    tokenizer.getCurrentTokenLocation());
        }
    }
}
