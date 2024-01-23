package software.amazon.smithy.model.neighbor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import java.util.Collection;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;

public class NodeQueryTest {
    @Test
    public void noQueriesGivesNoResults() {
        Node node = Node.from("{}");

        Collection<Node> result = new NodeQuery().execute(node);

        assertThat(result, hasSize(0));
    }

    @Test
    public void self() {
        Node node = Node.from("{}");

        Collection<Node> result = new NodeQuery().self().execute(node);

        assertThat(result, containsInAnyOrder(node));
    }

    @Test
    public void selfCanBeAppliedMultipleTimes() {
        Node node = Node.from("{}");

        Collection<Node> result = new NodeQuery().self().self().self().execute(node);

        assertThat(result, containsInAnyOrder(node));
    }

    @Test
    public void member() {
        Node member = StringNode.from("bar");
        Node node = Node.objectNode().withMember("foo", member);

        Collection<Node> result = new NodeQuery().member("foo").execute(node);

        assertThat(result, containsInAnyOrder(member));
    }

    @Test
    public void anyMember() {
        Node member1 = StringNode.from("member-one");
        Node member2 = StringNode.from("member-two");
        Node node = Node.objectNode().withMember("one", member1).withMember("two", member2);

        Collection<Node> result = new NodeQuery().anyMember().execute(node);

        assertThat(result, containsInAnyOrder(member1, member2));
    }

    @Test
    public void anyElement() {
        Node element1 = StringNode.from("element-one");
        Node element2 = StringNode.from("element-two");
        Node node = Node.arrayNode(element1, element2);

        Collection<Node> result = new NodeQuery().anyElement().execute(node);

        assertThat(result, containsInAnyOrder(element1, element2));
    }

    @Test
    public void anyMemberName() {
        StringNode key1 = StringNode.from("one");
        StringNode key2 = StringNode.from("two");
        Node member1 = StringNode.from("member-one");
        Node member2 = StringNode.from("member-two");
        Node node = Node.objectNode().withMember(key1, member1).withMember(key2, member2);

        Collection<Node> result = new NodeQuery().anyMemberName().execute(node);

        assertThat(result, containsInAnyOrder(key1, key2));
    }

    @Test
    public void memberGivesNoResultsOnNonObjectNode() {
        Node node = Node.from("[{\"foo\": 0}]");

        Collection<Node> result = new NodeQuery().member("foo").execute(node);

        assertThat(result, hasSize(0));
    }

    @Test
    public void memberGivesNoResultsIfMemberNameNotFound() {
        Node node = Node.from("{\"a\": 0, \"b\": 0}");

        Collection<Node> result = new NodeQuery().member("foo").execute(node);

        assertThat(result, hasSize(0));
    }

    @Test
    public void anyMemberGivesNoResultsOnNonObjectNode() {
        Node node = Node.from("[{\"foo\": 0}]");

        Collection<Node> result = new NodeQuery().anyMember().execute(node);

        assertThat(result, hasSize(0));
    }

    @Test
    public void anyMemberGivesNoResultsOnEmptyObjectNode() {
        Node node = Node.from("{}");

        Collection<Node> result = new NodeQuery().anyMember().execute(node);

        assertThat(result, hasSize(0));
    }

    @Test
    public void anyElementGivesNoResultsOnNonArrayNode() {
        Node node = Node.from("{\"foo\": [0]}");

        Collection<Node> result = new NodeQuery().anyElement().execute(node);

        assertThat(result, hasSize(0));
    }

    @Test
    public void anyElementGivesNoResultsOnEmptyArrayNode() {
        Node node = Node.from("[]");

        Collection<Node> result = new NodeQuery().anyElement().execute(node);

        assertThat(result, hasSize(0));
    }

    @Test
    public void anyMemberNameGivesNoResultsOnNonObjectNode() {
        Node node = Node.from("1");

        Collection<Node> result = new NodeQuery().anyMemberName().execute(node);

        assertThat(result, hasSize(0));
    }

    @Test
    public void anyMemberNameGivesNoResultsOnEmptyObject() {
        Node node = Node.from("{}");

        Collection<Node> result = new NodeQuery().anyMemberName().execute(node);

        assertThat(result, hasSize(0));
    }

    @Test
    public void eachQueryExecuteOnResultOfPreviousQuery() {
        Node element1 = Node.from(0);
        Node element2 = Node.from("{}");
        Node element3 = Node.from("element3");
        Node obj = Node.objectNode().withMember("foo", Node.objectNode()
                .withMember("arr1", Node.arrayNode(element1))
                .withMember("arr2", Node.arrayNode(element2))
                .withMember("arr3", Node.arrayNode(element3)));
        Node node = Node.arrayNode(obj, obj);

        Collection<Node> result = new NodeQuery()
                .anyElement()
                .member("foo")
                .anyMember()
                .anyElement()
                .execute(node);

        assertThat(result, containsInAnyOrder(
                element1,
                element2,
                element3,
                element1,
                element2,
                element3));
    }
}
