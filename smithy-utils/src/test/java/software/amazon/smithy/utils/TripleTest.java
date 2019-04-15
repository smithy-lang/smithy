package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class TripleTest {
    @Test
    public void createsTriple() {
        Triple<String, Integer, Boolean> triple = Triple.of("a", 10, true);

        assertThat(triple.getLeft(), equalTo("a"));
        assertThat(triple.getMiddle(), equalTo(10));
        assertThat(triple.getRight(), equalTo(true));
        assertThat(triple, equalTo(triple));
        assertThat(triple.toString(), equalTo("(a, 10, true)"));
    }

    @Test
    public void createsTripleFromPair() {
        Triple<String, Integer, Boolean> triple = Triple.fromPair(Pair.of("a", 10), true);

        assertThat(triple.getLeft(), equalTo("a"));
        assertThat(triple.getMiddle(), equalTo(10));
        assertThat(triple.getRight(), equalTo(true));
        assertThat(triple, equalTo(triple));
        assertThat(triple.toString(), equalTo("(a, 10, true)"));
    }
}
