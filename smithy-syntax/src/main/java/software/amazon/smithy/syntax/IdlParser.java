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

import software.amazon.smithy.model.loader.IdlTokenizer;

// idl = [WS] ControlSection MetadataSection ShapeSection
final class IdlParser {

    private final CapturingTokenizer tokenizer;
    private final NodeParser nodeParser;

    IdlParser(IdlTokenizer tokenizer) {
        this.tokenizer = new CapturingTokenizer(tokenizer);
        this.nodeParser = new NodeParser(this.tokenizer);
    }

    TokenTree parse() {
        new ControlSectionParser(tokenizer, nodeParser).parse();
        new MetadataParser(tokenizer, nodeParser).parse();
        parseShapeSection();
        return tokenizer.getRoot();
    }

    // ShapeSection = [NamespaceStatement UseSection [ShapeStatements]]
    private void parseShapeSection() {
        tokenizer.withState(TreeType.SHAPE_SECTION, () -> {
            new NamespaceStatementParser(tokenizer).parse();
            new UseSectionParser(tokenizer).parse();
            new ShapeStatementsParser(tokenizer, nodeParser).parse();
        });
    }
}
