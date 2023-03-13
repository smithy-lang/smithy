package software.amazon.smithy.build;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

public class PluginArtifactIdTest {
    @Test
    public void doesNotRequireArtifactId() {
        PluginId id = PluginId.from("foo");

        assertThat(id.hasArtifactName(), is(false));
        assertThat(id.toString(), equalTo("foo"));
        assertThat(id.getPluginName(), equalTo("foo"));
        assertThat(id.getArtifactName(), equalTo("foo"));
    }

    @Test
    public void allowsArtifactId() {
        PluginId id = PluginId.from("bar::foo");

        assertThat(id.hasArtifactName(), is(true));
        assertThat(id.toString(), equalTo("bar::foo"));
        assertThat(id.getPluginName(), equalTo("bar"));
        assertThat(id.getArtifactName(), equalTo("foo"));
    }

    @Test
    public void supportsEqualsAndHashCode() {
        PluginId id1 = PluginId.from("bar::foo");
        PluginId id2 = PluginId.from("bar::foo");
        PluginId id3 = PluginId.from("bam::foo");
        PluginId id4 = PluginId.from("foo::bam");
        PluginId id5 = PluginId.from("bam");

        assertThat(id1.hashCode(), equalTo(id2.hashCode()));
        assertThat(id1.hashCode(), not(equalTo(id3.hashCode())));
        assertThat(id1, equalTo(id2));
        assertThat(id1, equalTo(id1));
        assertThat(id1, not(equalTo(id3)));
        assertThat(id1, not(equalTo("foo")));
        assertThat(id3, not(equalTo(id4)));
        assertThat(id3, not(equalTo(id5)));
    }
}
