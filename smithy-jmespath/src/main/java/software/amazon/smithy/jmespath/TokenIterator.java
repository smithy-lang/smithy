/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

final class TokenIterator implements Iterator<Token> {

    private final List<Token> tokens;
    private int position;

    TokenIterator(List<Token> tokens) {
        this.tokens = tokens;
    }

    @Override
    public boolean hasNext() {
        return position < tokens.size();
    }

    @Override
    public Token next() {
        if (!hasNext()) {
            throw new NoSuchElementException("Attempted to parse past token EOF");
        }

        return tokens.get(position++);
    }

    Token peek() {
        return peek(0);
    }

    Token peek(int offset) {
        return position + offset < tokens.size()
                ? tokens.get(position + offset)
                : null;
    }

    Token expectNotEof() {
        Token peeked = peek();
        if (peeked == null) {
            throw syntax("Expected more tokens but found EOF");
        }
        return peeked;
    }

    Token expectPeek(TokenType type) {
        Token peeked = peek();
        if (peeked == null) {
            throw syntax("Expected " + type + ", but found EOF");
        } else if (peeked.type != type) {
            throw syntax("Expected " + type + ", but found " + peeked);
        } else {
            return peeked;
        }
    }

    Token expectPeek(TokenType... types) {
        Token peeked = peek();
        if (peeked == null) {
            throw syntax("Expected " + Arrays.toString(types) + ", but found EOF");
        }

        for (TokenType type : types) {
            if (peeked.type == type) {
                return peeked;
            }
        }

        throw syntax("Expected " + Arrays.toString(types) + ", but found " + peeked);
    }

    Token expect(TokenType type) {
        Token peeked = expectPeek(type);
        next();
        return peeked;
    }

    Token expect(TokenType... types) {
        Token peeked = expectPeek(types);
        next();
        return peeked;
    }

    JmespathException syntax(String message) {
        return new JmespathException("Syntax error at line " + line() + " column " + column() + ": " + message);
    }

    int line() {
        Token peeked = peek();
        if (peeked != null) {
            return peeked.line;
        } else if (position > 0) {
            return tokens.get(position - 1).line;
        } else {
            return 1;
        }
    }

    int column() {
        Token peeked = peek();
        if (peeked != null) {
            return peeked.column;
        } else if (position > 0) {
            return tokens.get(position - 1).column;
        } else {
            return 1;
        }
    }
}
