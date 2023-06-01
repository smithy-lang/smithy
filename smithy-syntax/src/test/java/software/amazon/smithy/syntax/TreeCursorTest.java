package software.amazon.smithy.syntax;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.loader.IdlTokenizer;
import software.amazon.smithy.utils.IoUtils;

public class TreeCursorTest {
    @Test
    public void hasChildren() {
        TokenTree tree = createTree();
        TreeCursor cursor = tree.zipper();
        List<TreeCursor> children = new ArrayList<>();
        cursor.getChildren().forEach(children::add);

        assertThat(cursor.getFirstChild(TreeType.CONTROL_SECTION), is(not(nullValue())));
        assertThat(cursor.getFirstChild(TreeType.METADATA_SECTION), is(not(nullValue())));
        assertThat(cursor.getFirstChild(TreeType.SHAPE_SECTION), is(not(nullValue())));

        assertThat(children, hasSize(3));
        assertThat(children.get(0).getTree().getType(), is(TreeType.CONTROL_SECTION));
        assertThat(children.get(1).getTree().getType(), is(TreeType.METADATA_SECTION));
        assertThat(children.get(2).getTree().getType(), is(TreeType.SHAPE_SECTION));

        assertThat(cursor.getFirstChild(), equalTo(children.get(0)));
        assertThat(cursor.getLastChild(), equalTo(children.get(2)));
    }

    @Test
    public void hasParentAndSiblings() {
        TokenTree tree = createTree();
        TreeCursor cursor = tree.zipper();
        List<TreeCursor> children = new ArrayList<>();
        cursor.getChildren().forEach(children::add);

        assertThat(cursor.getFirstChild(TreeType.CONTROL_SECTION).getParent(), equalTo(cursor));
        assertThat(cursor.getFirstChild(TreeType.METADATA_SECTION).getParent(), equalTo(cursor));
        assertThat(cursor.getFirstChild(TreeType.SHAPE_SECTION).getParent(), equalTo(cursor));

        assertThat(cursor.getFirstChild(TreeType.CONTROL_SECTION).getNextSibling(), equalTo(children.get(1)));
        assertThat(cursor.getFirstChild(TreeType.METADATA_SECTION).getNextSibling(), equalTo(children.get(2)));
        assertThat(cursor.getFirstChild(TreeType.SHAPE_SECTION).getNextSibling(), nullValue());

        assertThat(cursor.getFirstChild(TreeType.CONTROL_SECTION).getPreviousSibling(), nullValue());
        assertThat(cursor.getFirstChild(TreeType.METADATA_SECTION).getPreviousSibling(), equalTo(children.get(0)));
        assertThat(cursor.getFirstChild(TreeType.SHAPE_SECTION).getPreviousSibling(), equalTo(children.get(1)));
    }

    @Test
    public void findsNodeAtPosition() {
        TokenTree tree = createTree();
        TreeCursor cursor = tree.zipper();
        TreeCursor click = cursor.findAt(3, 17);

        assertThat(click, notNullValue());
        assertThat(click.getTree().getType(), is(TreeType.TOKEN));
        assertThat(click.getTree().tokens().iterator().next().getLexeme().toString(), equalTo("\"hello\""));
        assertThat(click.getRoot(), equalTo(cursor));
    }

    private TokenTree createTree() {
        String model = IoUtils.readUtf8Url(getClass().getResource("simple-model.smithy"));
        IdlTokenizer tokenizer = IdlTokenizer.create(model);
        return TokenTree.parse(tokenizer);
    }

    @Test
    public void getPathToCurrentTree() {
        TokenTree tree = createTree();
        TreeCursor cursor = tree.zipper();
        TreeCursor click = cursor.findAt(3, 17);
        List<TreeCursor> path = click.getPathToCursor();

        for (TreeCursor c : path) {
            System.out.println(c.getTree().getType());
        }
    }
}
