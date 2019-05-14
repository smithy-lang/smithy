package software.amazon.smithy.build;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.utils.ListUtils;

public class PluginContextTest {
    @Test
    public void usesExplicitProjectionName() {
        PluginContext context = PluginContext.builder()
                .projection(Projection.builder().name("foo").build())
                .fileManifest(new MockManifest())
                .model(Model.builder().build())
                .build();

        assertThat(context.getProjectionName(), equalTo("foo"));
    }

    @Test
    public void usesImplicitProjectionName() {
        PluginContext context = PluginContext.builder()
                .fileManifest(new MockManifest())
                .model(Model.builder().build())
                .build();

        assertThat(context.getProjectionName(), equalTo("source"));
    }

    @Test
    public void hasSources() {
        PluginContext context = PluginContext.builder()
                .fileManifest(new MockManifest())
                .model(Model.builder().build())
                .sources(ListUtils.of(Paths.get("/foo/baz")))
                .build();

        assertThat(context.getSources(), contains(Paths.get("/foo/baz")));
    }
}
