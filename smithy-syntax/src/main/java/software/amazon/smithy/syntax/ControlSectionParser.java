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

import software.amazon.smithy.model.loader.IdlToken;

// ControlSection   = *(ControlStatement)
// ControlStatement = "$" NodeObjectKey [SP] ":" [SP] NodeValue BR
final class ControlSectionParser {

    private final CapturingTokenizer tokenizer;
    private final NodeParser nodeParser;

    ControlSectionParser(CapturingTokenizer tokenizer, NodeParser nodeParser) {
        this.tokenizer = tokenizer;
        this.nodeParser = nodeParser;
    }

    public void parse() {
        tokenizer.withState(TreeType.CONTROL_SECTION, () -> {
            while (tokenizer.getCurrentToken() == IdlToken.DOLLAR) {
                tokenizer.withState(TreeType.CONTROL_STATEMENT, controlStatement -> {
                    tokenizer.next(); // $
                    nodeParser.parseNodeObjectKey();
                    tokenizer.skipSpaces();
                    tokenizer.expect(IdlToken.COLON);
                    tokenizer.next();
                    tokenizer.skipSpaces();
                    nodeParser.parse();
                    tokenizer.expectAndSkipBr();
                });
            }
        });
    }
}
