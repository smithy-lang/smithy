package software.amazon.smithy.build.plugins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.SmithyBuildResult;
import software.amazon.smithy.build.SourcesConflictException;
import software.amazon.smithy.build.model.ProjectionConfig;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.utils.ListUtils;

public class SourcesPluginTest {
    @Test
    public void copiesFilesForSourceProjection() throws URISyntaxException {
        Model model = Model.assembler()
                .addImport(getClass().getResource("sources/a.smithy"))
                .addImport(getClass().getResource("sources/b.smithy"))
                .addImport(getClass().getResource("sources/c/c.json"))
                .addImport(getClass().getResource("notsources/d.smithy"))
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        PluginContext context = PluginContext.builder()
                .fileManifest(manifest)
                .model(model)
                .originalModel(model)
                .sources(ListUtils.of(Paths.get(getClass().getResource("sources/a.smithy").toURI()).getParent()))
                .build();
        new SourcesPlugin().execute(context);
        String manifestString = manifest.getFileString("manifest").get();
        // Normalize for Windows.
        manifestString = manifestString.replace("\\", "/");

        assertThat(manifestString, containsString("a.smithy\n"));
        assertThat(manifestString, containsString("b.smithy\n"));
        assertThat(manifestString, containsString("c/c.json\n"));
        assertThat(manifestString, not(containsString("d.json")));
        assertThat(manifest.getFileString("a.smithy").get(), containsString("AString"));
        assertThat(manifest.getFileString("b.smithy").get(), containsString("BString"));
        assertThat(manifest.getFileString("c/c.json").get(), containsString("CString"));
    }

    @Test
    public void copiesModelFromJarWithSourceProjection() throws URISyntaxException {
        Model model = Model.assembler()
                .addImport(getClass().getResource("sources/jar-import.jar"))
                .addImport(getClass().getResource("notsources/d.smithy"))
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        PluginContext context = PluginContext.builder()
                .fileManifest(manifest)
                .model(model)
                .originalModel(model)
                .sources(ListUtils.of(Paths.get(getClass().getResource("sources/jar-import.jar").toURI())))
                .build();
        new SourcesPlugin().execute(context);
        String manifestString = manifest.getFileString("manifest").get();
        // Normalize for Windows.
        manifestString = manifestString.replace("\\", "/");

        assertThat(manifestString, containsString("jar-import/a.smithy\n"));
        assertThat(manifestString, containsString("jar-import/b/b.smithy\n"));
        assertThat(manifestString, containsString("jar-import/b/c/c.json\n"));
        assertThat(manifestString, not(containsString("jar-import/d.json")));
        assertThat(manifest.getFileString("jar-import/a.smithy").get(), containsString("string A"));
        assertThat(manifest.getFileString("jar-import/b/b.smithy").get(), containsString("string B"));
        assertThat(manifest.getFileString("jar-import/b/c/c.json").get(), containsString("\"foo.baz#C\""));
    }

    @Test
    public void copiesModelFromJarWithNonSourceProjection() throws URISyntaxException {
        Model model = Model.assembler()
                .addImport(getClass().getResource("sources/jar-import.jar"))
                .addImport(getClass().getResource("notsources/d.smithy"))
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        ProjectionConfig projection = ProjectionConfig.builder().build();
        PluginContext context = PluginContext.builder()
                .fileManifest(manifest)
                .projection("foo", projection)
                .model(model)
                .originalModel(model)
                .sources(ListUtils.of(Paths.get(getClass().getResource("sources/jar-import.jar").toURI())))
                .build();
        new SourcesPlugin().execute(context);
        String manifestString = manifest.getFileString("manifest").get();
        // Normalize for Windows.
        manifestString = manifestString.replace("\\", "/");

        assertThat(manifestString, containsString("model.json"));
        assertThat(manifestString, not(containsString("jar-import")));
        assertThat(manifest.getFileString("model.json").get(), containsString("\"foo.baz#A\""));
        assertThat(manifest.getFileString("model.json").get(), containsString("\"foo.baz#B\""));
        assertThat(manifest.getFileString("model.json").get(), containsString("\"foo.baz#C\""));
    }

    @Test
    public void copiesOnlyFilesFromSourcesForProjection() throws URISyntaxException {
        Model model = Model.assembler()
                .addImport(getClass().getResource("sources/a.smithy"))
                .addImport(getClass().getResource("sources/b.smithy"))
                .addImport(getClass().getResource("sources/c/c.json"))
                .addImport(getClass().getResource("notsources/d.smithy"))
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        ProjectionConfig projection = ProjectionConfig.builder().build();
        PluginContext context = PluginContext.builder()
                .projection("foo", projection)
                .fileManifest(manifest)
                .model(model)
                .originalModel(model)
                .sources(ListUtils.of(Paths.get(getClass().getResource("sources/a.smithy").toURI()).getParent()))
                .build();
        new SourcesPlugin().execute(context);
        String manifestString = manifest.getFileString("manifest").get();
        String modelString = manifest.getFileString("model.json").get();

        assertThat(manifestString, equalTo("model.json\n"));
        assertThat(modelString, containsString("AString"));
        assertThat(modelString, containsString("BString"));
        assertThat(modelString, containsString("CString"));
        assertThat(modelString, containsString("metadata-a"));
        assertThat(modelString, containsString("metadata-b"));
        assertThat(modelString, containsString("metadata-c"));
        assertThat(modelString, containsString("Atrait"));
        assertThat(modelString, containsString("Btrait"));
        assertThat(modelString, containsString("Ctrait"));
        assertThat(modelString, not(containsString("DString")));
        assertThat(modelString, not(containsString("metadata-d")));
        assertThat(modelString, not(containsString("Dtrait")));
    }

    @Test
    public void treatsNewlyAddedShapesAsNewSources() throws URISyntaxException {
        Model originalModel = Model.assembler().assemble().unwrap();
        Model newModel = Model.assembler()
                .addShape(StringShape.builder().id("a.b#MyString").build())
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        ProjectionConfig projection = ProjectionConfig.builder().build();
        PluginContext context = PluginContext.builder()
                .projection("foo", projection)
                .fileManifest(manifest)
                .originalModel(originalModel)
                .model(newModel)
                .sources(ListUtils.of(Paths.get(getClass().getResource("sources/a.smithy").toURI()).getParent()))
                .build();
        new SourcesPlugin().execute(context);
        String modelString = manifest.getFileString("model.json").get();

        assertThat(modelString, containsString("MyString"));
    }

    @Test
    public void doesNotAllowConflicts() throws URISyntaxException {
        Model model = Model.assembler()
                .addImport(getClass().getResource("sources/a.smithy"))
                .addImport(getClass().getResource("conflicting/a.smithy"))
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        PluginContext context = PluginContext.builder()
                .fileManifest(manifest)
                .model(model)
                .originalModel(model)
                .sources(ListUtils.of(
                        Paths.get(getClass().getResource("sources/a.smithy").toURI()),
                        Paths.get(getClass().getResource("conflicting/a.smithy").toURI())))
                .build();

        Assertions.assertThrows(SourcesConflictException.class, () -> new SourcesPlugin().execute(context));
    }

    @Test
    public void copiesModelsDefinedInConfigAsSources() throws URISyntaxException {
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("sources/copiesModelsDefinedInConfigAsSources.json").toURI()))
                .build();
        SmithyBuild b = new SmithyBuild();
        b.fileManifestFactory(p -> new MockManifest());
        b.config(config);
        SmithyBuildResult result = b.build();

        MockManifest manifest = (MockManifest) result
                .getProjectionResult("source")
                .get()
                .getPluginManifest("sources")
                .get();

        assertThat(manifest.getFileString("manifest").get(), containsString("a.smithy"));
        assertThat(manifest.getFileString("manifest").get(), not(containsString("d.smithy")));
        assertThat(manifest.hasFile("a.smithy"), is(true));
        assertThat(manifest.hasFile("d.smithy"), is(false));
    }

    // When the sources plugin sees a file it does not support, it is ignored.
    @Test
    public void omitsUnsupportedFilesFromManifest() throws URISyntaxException {
        Model model = Model.assembler()
                .addImport(getClass().getResource("sources-ignores-unrecognized-files/a.smithy"))
                .addImport(getClass().getResource("sources-ignores-unrecognized-files/foo.md"))
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        PluginContext context = PluginContext.builder()
                .fileManifest(manifest)
                .model(model)
                .originalModel(model)
                .sources(ListUtils.of(Paths.get(getClass().getResource("sources-ignores-unrecognized-files").toURI())))
                .build();
        new SourcesPlugin().execute(context);
        String manifestString = manifest.getFileString("manifest").get();
        // Normalize for Windows.
        manifestString = manifestString.replace("\\", "/");

        assertThat(manifestString, containsString("a.smithy\n"));
        assertThat(manifestString, not(containsString("foo.md")));
    }
}
