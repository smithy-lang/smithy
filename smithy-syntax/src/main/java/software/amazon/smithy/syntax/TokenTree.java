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

import java.util.List;
import java.util.stream.Stream;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.loader.IdlToken;
import software.amazon.smithy.model.loader.IdlTokenizer;

/**
 * Provides a labeled tree of tokens returned from {@link IdlTokenizer}.
 *
 * <p>This abstraction is a kind of parse tree based on lexer tokens, with the key difference being it bottoms-out at
 * {@link IdlToken} values rather than the more granular grammar productions for identifiers, strings, etc.
 *
 * <p>Each consumed IDL token is present in the tree, and grouped together into nodes with labels defined by its
 * {@link TreeType}.
 */
public interface TokenTree extends FromSourceLocation {

    /**
     * Create the root of a TokenTree from an {@link IdlTokenizer}.
     *
     * @param tokenizer Tokenizer to traverse.
     * @return Returns the root of the created tree.
     */
    static TokenTree of(IdlTokenizer tokenizer) {
        CapturingTokenizer capturingTokenizer = new CapturingTokenizer(tokenizer);
        TreeType.IDL.parse(capturingTokenizer);
        return capturingTokenizer.getRoot();
    }

    /**
     * Create a leaf tree from a single token.
     *
     * @param token Token to wrap into a tree.
     * @return Returns the created tree.
     */
    static TokenTree of(CapturedToken token) {
        return new TokenTreeLeaf(token);
    }

    /**
     * Create an empty tree of a specific {@code type}.
     *
     * @param type Tree type to create.
     * @return Returns the created tree.
     */
    static TokenTree of(TreeType type) {
        return new TokenTreeNode(type);
    }

    /**
     * Create an error tree with the given {@code error} message.
     *
     * @param error Error message.
     * @return Returns the created tree.
     */
    static TokenTree fromError(String error) {
        return new TokenTreeError(error);
    }

    /**
     * Get the token tree type.
     *
     * @return Returns the type.
     */
    TreeType getType();

    /**
     * Get direct children of the tree.
     *
     * @return Returns direct children.
     */
    List<TokenTree> getChildren();

    /**
     * Detect if the tree is empty (that is, a non-leaf that has no children or tokens).
     *
     * @return Return true if the tree has no children or tokens.
     */
    boolean isEmpty();

    /**
     * Append a child to the tree.
     *
     * @param tree Tree to append.
     * @throws UnsupportedOperationException if the tree is a leaf.
     */
    void appendChild(TokenTree tree);

    /**
     * Remove a child tree by referential equality.
     *
     * @param tree Tree to remove.
     * @return Return true only if this child was found and removed.
     */
    boolean removeChild(TokenTree tree);

    /**
     * Replace a matching child with the given replacement using referential equality.
     *
     * @param find        Child to find and replace, using referential equality.
     * @param replacement Replacement to use instead.
     * @return Returns true only if a child was replaced.
     */
    boolean replaceChild(TokenTree find, TokenTree replacement);

    /**
     * Get a flattened stream of all captured tokens contained within the tree.
     *
     * @return Returns the contained tokens.
     */
    Stream<CapturedToken> tokens();

    /**
     * Get the tokens contained in the tree as a single concatenated string.
     *
     * @return Returns a concatenated string of tokens.
     */
    String concatTokens();

    /**
     * Get the error associated with the tree, or {@code null} if not present.
     *
     * @return Returns the nullable error message.
     */
    default String getError() {
        return null;
    }

    /**
     * Create a zipper for the current tree node, treating it as the root of the tree.
     *
     * @return Returns the zipper cursor for the current node.
     */
    default TreeCursor zipper() {
        return TreeCursor.of(this);
    }

    /**
     * Get the absolute start position of the tree, starting at 0.
     *
     * @return Returns the start position of this tree.
     */
    int getStartPosition();

    /**
     * Get the line the tree starts, starting at 1.
     *
     * @return Returns the start line.
     */
    int getStartLine();

    /**
     * Get the column the tree starts, starting at 1.
     *
     * @return Returns the start column.
     */
    int getStartColumn();

    /**
     * Get the line the tree ends, starting at 1.
     *
     * @return Returns the end line.
     */
    int getEndLine();

    /**
     * Get the column the tree ends, starting at 1.
     *
     * @return Returns the end column.
     */
    int getEndColumn();
}
