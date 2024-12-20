/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.syntax;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.loader.IdlTokenizer;

public class TokenTreeLeafTest {
    @Test
    public void cannotAppendChildren() {
        IdlTokenizer tokenizer = IdlTokenizer.create("foo bar");
        CapturedToken token = CapturedToken.from(tokenizer);
        TokenTreeLeaf leaf = new TokenTreeLeaf(token);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            tokenizer.next();
            leaf.appendChild(TokenTree.of(CapturedToken.from(tokenizer)));
        });
    }
}
