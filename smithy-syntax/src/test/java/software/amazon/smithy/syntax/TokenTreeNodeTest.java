package software.amazon.smithy.syntax;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.loader.IdlTokenizer;

public class TokenTreeNodeTest {
    @Test
    public void hasChildren() {
        TokenTree tree = TokenTree.of(TreeType.BR);
        TokenTree a = TokenTree.of(TreeType.WS);
        TokenTree b = TokenTree.of(TreeType.TOKEN);

        tree.appendChild(a);
        tree.appendChild(b);

        assertThat(tree.getChildren(), contains(a, b));

        tree.removeChild(a);

        assertThat(tree.getChildren(), contains(b));
    }

    @Test
    public void hasTokens() {
        IdlTokenizer tokenizer = IdlTokenizer.create("foo");
        CapturedToken capture = CapturedToken.from(tokenizer);
        TokenTree tree = TokenTree.of(capture);

        assertThat(tree.tokens().collect(Collectors.toList()), contains(capture));
        assertThat(tree.getStartPosition(), equalTo(0));
        assertThat(tree.getStartLine(), equalTo(1));
        assertThat(tree.getStartColumn(), equalTo(1));
        assertThat(tree.getEndColumn(), equalTo(4)); // the column is exclusive (it goes to 3, but 4 is the _next_ col)
        assertThat(tree.getEndLine(), equalTo(1));
    }

    @Test
    public void equalsAndHashCodeWork() {
        IdlTokenizer tokenizer = IdlTokenizer.create("foo");
        CapturedToken capture = CapturedToken.from(tokenizer);
        TokenTree tree = TokenTree.of(capture);

        assertThat(tree, equalTo(tree));
        assertThat(tree, not(equalTo("hi")));
        assertThat(tree, not(equalTo(TokenTree.fromError("Foo"))));
        assertThat(tree.hashCode(), is(not(0)));
    }
}
