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
import software.amazon.smithy.model.loader.IdlTokenizer;

/**
 * Provides a labeled tree of tokens returned from {@link IdlTokenizer}.
 *
 * <p>This abstraction is a kind of parse tree based on lexer tokens. Each consumed token is present in the tree,
 * and grouped together into nodes with labels defined by {@link TreeType}.
 */
public interface TokenTree {

    /**
     * Create a TokenTree from a {@link IdlTokenizer}.
     *
     * @param tokenizer Tokenizer to traverse.
     * @return Returns the created tree.
     */
    static TokenTree parse(IdlTokenizer tokenizer) {
        CapturingTokenizer capturingTokenizer = new CapturingTokenizer(tokenizer);
        TreeType.IDL.parse(capturingTokenizer);
        return capturingTokenizer.getRoot();
    }

    /**
     * Create a tree from a single token.
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
     * Create an error tree.
     *
     * @param error Error message.
     * @return Returns the created tree.
     */
    static TokenTree error(String error) {
        return new TokenTreeError(error);
    }

    /**
     * Gets the token tree type.
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
     * Check if the tree has an immediate child of the given type.
     *
     * @param type Type to check.
     * @return Return true if the tree has a child of the given type.
     */
    default boolean hasChild(TreeType type) {
        for (TokenTree tree : getChildren()) {
            if (tree.getType() == type) {
                return true;
            }
        }
        return false;
    }

    /**
     * Append a child to the tree.
     *
     * @param tree Tree to append.
     */
    void appendChild(TokenTree tree);

    /**
     * Remove a token tree.
     *
     * @param tree Tree to remove.
     * @return Return true if this tree was found and removed.
     */
    boolean removeChild(TokenTree tree);

    /**
     * Get a flattened stream of all captured tokens contained within the tree.
     *
     * @return Returns the contained tokens.
     */
    Stream<CapturedToken> tokens();

    /**
     * Gets the error associated with the tree, or null if not present.
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
        return TreeCursor.fromRoot(this);
    }

    /**
     * Get the absolute start position, starting at 0.
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
     * Get the column the tree end, starting at 1.
     *
     * @return Returns the end column.
     */
    int getEndColumn();

    /**
     * Get the tokens contains in the tree as a single concatenated string.
     *
     * @return Returns the concatenated string of tokens.
     */
    default String concatTokens() {
        StringBuilder result = new StringBuilder();
        tokens().forEach(token -> result.append(token.getLexeme()));
        return result.toString();
    }
}
