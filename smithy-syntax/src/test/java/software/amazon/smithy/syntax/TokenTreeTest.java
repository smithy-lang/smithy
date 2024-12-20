/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.syntax;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.loader.IdlTokenizer;

public class TokenTreeTest {
    @Test
    public void createsFromType() {
        TokenTree tree = TokenTree.of(TreeType.WS);

        assertThat(tree.getType(), is(TreeType.WS));
        assertThat(tree.getChildren(), empty());
        assertThat(tree.getError(), nullValue());
    }

    @Test
    public void createsFromTokenizer() {
        IdlTokenizer tokenizer = IdlTokenizer.create("foo");
        TokenTree tree = TokenTree.of(tokenizer);

        assertThat(tree.getType(), is(TreeType.IDL));
        assertThat(tree.getChildren(), hasSize(3));
        assertThat(tree.getError(), nullValue());
        assertThat(tree.getChildren().get(0).getType(), equalTo(TreeType.CONTROL_SECTION));
        assertThat(tree.getChildren().get(1).getType(), equalTo(TreeType.METADATA_SECTION));
        assertThat(tree.getChildren().get(2).getType(), equalTo(TreeType.SHAPE_SECTION));
    }

    @Test
    public void createsFromTokenizerAndType() {
        IdlTokenizer tokenizer = IdlTokenizer.create("@foo");
        TokenTree tree = TokenTree.of(tokenizer, TreeType.TRAIT);

        assertThat(tree.getType(), is(TreeType.TRAIT));
        assertThat(tree.getChildren(), hasSize(2));
        assertThat(tree.getError(), nullValue());
        assertThat(tree.getChildren().get(0).getType(), equalTo(TreeType.TOKEN));
        assertThat(tree.getChildren().get(1).getType(), equalTo(TreeType.SHAPE_ID));
    }

    @Test
    public void createsFromCapturedToken() {
        IdlTokenizer tokenizer = IdlTokenizer.create("foo");
        CapturedToken token = CapturedToken.from(tokenizer);
        TokenTree tree = TokenTree.of(token);

        assertThat(tree.getType(), is(TreeType.TOKEN));
        assertThat(tree.getChildren(), hasSize(0));
    }

    @Test
    public void createsFromErrorString() {
        TokenTree tree = TokenTree.fromError("Foo");

        assertThat(tree.getType(), is(TreeType.ERROR));
        assertThat(tree.getError(), equalTo("Foo"));
    }

    @Test
    public void createsZipperForNode() {
        IdlTokenizer tokenizer = IdlTokenizer.create("foo");
        CapturedToken token = CapturedToken.from(tokenizer);
        TokenTree tree = TokenTree.of(token);
        TreeCursor cursor = tree.zipper();

        assertThat(tree, equalTo(cursor.getTree()));
    }

    @Test
    public void replacesChild() {
        TokenTree tree = TokenTree.of(TreeType.WS);
        TokenTree child1 = TokenTree.of(TreeType.COMMENT);
        TokenTree child2 = TokenTree.of(TreeType.COMMA);

        tree.appendChild(child1);

        assertThat(tree.replaceChild(child1, child2), is(true));
        assertThat(tree.replaceChild(child1, child2), is(false));
        assertThat(tree.zipper().getFirstChild(TreeType.COMMA).getTree(), is(child2));
    }

    @Test
    public void emptyTreeLocationsDontBubbleUpToParent() {
        // If a tree's first/last child is empty, meaning it has a location of
        // (0, 0) - (0, 0), that should not be considered the starting/ending
        // location of the tree.
        IdlTokenizer shapeStatementTokenizer = IdlTokenizer.create("structure Foo {}");
        TokenTree shapeStatement = TokenTree.of(shapeStatementTokenizer, TreeType.SHAPE_STATEMENT);
        assertThat(shapeStatement.getStartLine(), equalTo(1));
        assertThat(shapeStatement.getStartColumn(), equalTo(1));
        assertThat(shapeStatement.getEndLine(), equalTo(1));
        assertThat(shapeStatement.getEndColumn(), equalTo(17));

        // There are no traits, so this will be an empty tree with location
        // (0, 0) - (0, 0).
        TokenTree traitStatements = shapeStatement.getChildren().get(0);
        assertThat(traitStatements.getStartLine(), equalTo(0));
        assertThat(traitStatements.getStartColumn(), equalTo(0));
        assertThat(traitStatements.getEndLine(), equalTo(0));
        assertThat(traitStatements.getEndColumn(), equalTo(0));

        // The SHAPE_STATEMENT location should come from the SHAPE child.
        TokenTree shape = shapeStatement.getChildren().get(1);
        assertThat(shape.getStartLine(), equalTo(shapeStatement.getStartLine()));
        assertThat(shape.getStartColumn(), equalTo(shapeStatement.getStartColumn()));
        assertThat(shape.getEndLine(), equalTo(shapeStatement.getEndLine()));
        assertThat(shape.getEndColumn(), equalTo(shapeStatement.getEndColumn()));

        IdlTokenizer shapeSectionTokenizer = IdlTokenizer.create("namespace com.foo\n");
        TokenTree shapeSection = TokenTree.of(shapeSectionTokenizer, TreeType.SHAPE_SECTION);
        assertThat(shapeSection.getStartLine(), equalTo(1));
        assertThat(shapeSection.getStartColumn(), equalTo(1));
        assertThat(shapeSection.getEndLine(), equalTo(2));
        assertThat(shapeSection.getEndColumn(), equalTo(1));

        // There are shape statements, so this will be an empty tree with location
        // (0, 0) - (0, 0).
        TokenTree shapeStatements = shapeSection.getChildren().get(2);
        assertThat(shapeStatements.getStartLine(), equalTo(0));
        assertThat(shapeStatements.getStartColumn(), equalTo(0));
        assertThat(shapeStatements.getEndLine(), equalTo(0));
        assertThat(shapeStatements.getEndColumn(), equalTo(0));

        // The SHAPE_SECTION location should come from the NAMESPACE_STATEMENT child.
        TokenTree namespaceStatement = shapeSection.getChildren().get(0);
        assertThat(namespaceStatement.getStartLine(), equalTo(shapeSection.getStartLine()));
        assertThat(namespaceStatement.getStartColumn(), equalTo(shapeSection.getStartColumn()));
        assertThat(namespaceStatement.getEndLine(), equalTo(shapeSection.getEndLine()));
        assertThat(namespaceStatement.getEndColumn(), equalTo(shapeSection.getEndColumn()));
    }
}
