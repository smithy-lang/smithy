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
}
