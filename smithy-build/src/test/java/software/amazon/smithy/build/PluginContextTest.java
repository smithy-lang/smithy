package software.amazon.smithy.build;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.model.ProjectionConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.utils.ListUtils;

public class PluginContextTest {
    @Test
    public void usesExplicitProjectionName() {
        PluginContext context = PluginContext.builder()
                .projection("foo", ProjectionConfig.builder().build())
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

    @Test
    public void createsNonTraitModel() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("simple-model.json"))
                .assemble()
                .unwrap();
        Model scrubbed = ModelTransformer.create().getModelWithoutTraitShapes(model);
        PluginContext context = PluginContext.builder()
                .fileManifest(new MockManifest())
                .model(model)
                .sources(ListUtils.of(Paths.get("/foo/baz")))
                .build();

        assertThat(context.getModelWithoutTraitShapes(), equalTo(scrubbed));
        assertThat(context.getModelWithoutTraitShapes(), equalTo(scrubbed)); // trigger loading from cache
    }

    @Test
    public void convertsToBuilder() {
        PluginContext context = PluginContext.builder()
                .projection("foo", ProjectionConfig.builder().build())
                .fileManifest(new MockManifest())
                .model(Model.builder().build())
                .originalModel(Model.builder().build())
                .settings(Node.objectNode().withMember("foo", "bar"))
                .build();
        PluginContext context2 = context.toBuilder().build();

        assertThat(context.getProjectionName(), equalTo(context2.getProjectionName()));
        assertThat(context.getProjection(), equalTo(context2.getProjection()));
        assertThat(context.getModel(), equalTo(context2.getModel()));
        assertThat(context.getOriginalModel(), equalTo(context2.getOriginalModel()));
        assertThat(context.getFileManifest(), is(context2.getFileManifest()));
        assertThat(context.getSources(), equalTo(context2.getSources()));
        assertThat(context.getEvents(), equalTo(context2.getEvents()));
    }

    @Test
    public void roundTripsDefaults() {
        PluginContext before = PluginContext.builder()
                .model(Model.builder().build())
                .fileManifest(new MockManifest())
                .build();
        PluginContext after = before.toBuilder().build();
        assertThat(before.getModel(), equalTo(after.getModel()));
        assertThat(before.getFileManifest(), equalTo(after.getFileManifest()));
        assertThat(before.getProjectionName(), equalTo(after.getProjectionName()));
    }
}
