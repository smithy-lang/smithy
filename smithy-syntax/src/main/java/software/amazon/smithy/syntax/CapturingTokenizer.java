/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.syntax;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;
import software.amazon.smithy.model.loader.IdlToken;
import software.amazon.smithy.model.loader.IdlTokenizer;
import software.amazon.smithy.model.loader.ModelSyntaxException;
import software.amazon.smithy.model.loader.StringTable;

/**
 * Captures tokens to a stack of token trees.
 */
final class CapturingTokenizer implements IdlTokenizer {

    private final IdlTokenizer delegate;
    private final TokenTree root = TokenTree.of(TreeType.IDL);
    private final Deque<TokenTree> trees = new ArrayDeque<>();
    private final Function<CharSequence, String> stringTable = new StringTable(10); // 1024 entries
    private final List<CapturedToken> tokens = new ArrayList<>();
    private int cursor = 0;

    CapturingTokenizer(IdlTokenizer delegate) {
        this.delegate = delegate;
        trees.add(root);

        while (delegate.hasNext()) {
            delegate.next();
            tokens.add(CapturedToken.from(delegate, stringTable));
        }
    }

    TokenTree getRoot() {
        return root;
    }

    @Override
    public String getSourceFilename() {
        return delegate.getSourceFilename();
    }

    @Override
    public CharSequence getModel() {
        return delegate.getModel();
    }

    private CapturedToken getToken() {
        return tokens.get(cursor);
    }

    @Override
    public int getPosition() {
        return getToken().getPosition() + getToken().getSpan();
    }

    @Override
    public int getLine() {
        return getToken().getEndLine();
    }

    @Override
    public int getColumn() {
        return getToken().getEndColumn();
    }

    @Override
    public IdlToken getCurrentToken() {
        return getToken().getIdlToken();
    }

    @Override
    public int getCurrentTokenLine() {
        return getToken().getStartLine();
    }

    @Override
    public int getCurrentTokenColumn() {
        return getToken().getStartColumn();
    }

    @Override
    public int getCurrentTokenStart() {
        return getToken().getPosition();
    }

    @Override
    public int getCurrentTokenEnd() {
        return getPosition();
    }

    @Override
    public CharSequence getCurrentTokenStringSlice() {
        return getToken().getStringContents();
    }

    @Override
    public Number getCurrentTokenNumberValue() {
        return getToken().getNumberValue();
    }

    @Override
    public String getCurrentTokenError() {
        return getToken().getErrorMessage();
    }

    @Override
    public boolean hasNext() {
        return getToken().getIdlToken() != IdlToken.EOF;
    }

    @Override
    public IdlToken next() {
        if (getToken().getIdlToken() == IdlToken.EOF) {
            throw new NoSuchElementException();
        }
        appendCurrentTokenToFirstTree();
        return tokens.get(++cursor).getIdlToken();
    }

    void eof() {
        appendCurrentTokenToFirstTree();
    }

    private void appendCurrentTokenToFirstTree() {
        trees.getFirst().appendChild(TokenTree.of(CapturedToken.from(this, this.stringTable)));
    }

    CapturedToken peekPastSpaces() {
        return peekWhile(0, token -> token == IdlToken.SPACE);
    }

    CapturedToken peekWhile(int offsetFromPosition, Predicate<IdlToken> predicate) {
        int position = cursor + offsetFromPosition;
        // If the start position is out of bounds, return the EOF token.
        if (position >= tokens.size()) {
            return tokens.get(tokens.size() - 1);
        }
        CapturedToken token = tokens.get(position);
        while (token.getIdlToken() != IdlToken.EOF && predicate.test(token.getIdlToken())) {
            token = tokens.get(++position);
        }
        return token;
    }

    String internString(CharSequence charSequence) {
        return stringTable.apply(charSequence);
    }

    TokenTree withState(TreeType state, Runnable parser) {
        return withState(state, this::defaultErrorRecovery, parser);
    }

    TokenTree withState(TreeType state, Runnable errorRecovery, Runnable parser) {
        TokenTree tree = TokenTree.of(state);
        trees.getFirst().appendChild(tree);
        // Temporarily make this tree the current tree to capture tokens.
        trees.addFirst(tree);
        try {
            parser.run();
        } catch (ModelSyntaxException e) {
            TokenTree errorTree = TokenTree.fromError(e.getMessageWithoutLocation());
            tree.appendChild(errorTree);
            if (getCurrentToken() != IdlToken.EOF) {
                // Temporarily make the error tree the current tree to capture error recovery tokens.
                trees.addFirst(errorTree);
                next();
                errorRecovery.run();
                trees.removeFirst();
            }
        } finally {
            trees.removeFirst();
        }
        return tree;
    }

    // Performs basic error recovery by skipping tokens until a $, identifier, or @ is found at column 1.
    private void defaultErrorRecovery() {
        while (hasNext()) {
            if (getCurrentTokenColumn() == 1) {
                IdlToken token = getCurrentToken();
                if (token == IdlToken.DOLLAR || token == IdlToken.IDENTIFIER
                        || token == IdlToken.AT
                        || token == IdlToken.RBRACE) {
                    return;
                }
            }
            next();
        }
    }
}
