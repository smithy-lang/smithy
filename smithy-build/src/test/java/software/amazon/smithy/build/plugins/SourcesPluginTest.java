package software.amazon.smithy.build.plugins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SourcesConflictException;
import software.amazon.smithy.build.model.ProjectionConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.utils.ListUtils;

public class SourcesPluginTest {
    @Test
    public void copiesFilesForSourceProjection() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("sources/a.smithy").getPath())
                .addImport(getClass().getResource("sources/b.smithy").getPath())
                .addImport(getClass().getResource("sources/c/c.json"))
                .addImport(getClass().getResource("notsources/d.smithy"))
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        PluginContext context = PluginContext.builder()
                .fileManifest(manifest)
                .model(model)
                .originalModel(model)
                .sources(ListUtils.of(Paths.get(getClass().getResource("sources/a.smithy").getPath()).getParent()))
                .build();
        new SourcesPlugin().execute(context);
        String manifestString = manifest.getFileString("manifest").get();

        assertThat(manifestString, containsString("a.smithy\n"));
        assertThat(manifestString, containsString("b.smithy\n"));
        assertThat(manifestString, containsString("c/c.json\n"));
        assertThat(manifestString, not(containsString("d.json")));
        assertThat(manifest.getFileString("a.smithy").get(), containsString("AString"));
        assertThat(manifest.getFileString("b.smithy").get(), containsString("BString"));
        assertThat(manifest.getFileString("c/c.json").get(), containsString("CString"));
    }

    @Test
    public void copiesModelFromJarWithSourceProjection() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("sources/jar-import.jar").getPath())
                .addImport(getClass().getResource("notsources/d.smithy"))
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        PluginContext context = PluginContext.builder()
                .fileManifest(manifest)
                .model(model)
                .originalModel(model)
                .sources(ListUtils.of(Paths.get(getClass().getResource("sources/jar-import.jar").getPath())))
                .build();
        new SourcesPlugin().execute(context);
        String manifestString = manifest.getFileString("manifest").get();

        assertThat(manifestString, containsString("jar-import/a.smithy\n"));
        assertThat(manifestString, containsString("jar-import/b/b.smithy\n"));
        assertThat(manifestString, containsString("jar-import/b/c/c.json\n"));
        assertThat(manifestString, not(containsString("jar-import/d.json")));
        assertThat(manifest.getFileString("jar-import/a.smithy").get(), containsString("string A"));
        assertThat(manifest.getFileString("jar-import/b/b.smithy").get(), containsString("string B"));
        assertThat(manifest.getFileString("jar-import/b/c/c.json").get(), containsString("\"foo.baz#C\""));
    }

    @Test
    public void copiesModelFromJarWithNonSourceProjection() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("sources/jar-import.jar").getPath())
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
                .sources(ListUtils.of(Paths.get(getClass().getResource("sources/jar-import.jar").getPath())))
                .build();
        new SourcesPlugin().execute(context);
        String manifestString = manifest.getFileString("manifest").get();

        assertThat(manifestString, containsString("model.json"));
        assertThat(manifestString, not(containsString("jar-import")));
        assertThat(manifest.getFileString("model.json").get(), containsString("\"foo.baz#A\""));
        assertThat(manifest.getFileString("model.json").get(), containsString("\"foo.baz#B\""));
        assertThat(manifest.getFileString("model.json").get(), containsString("\"foo.baz#C\""));
    }

    @Test
    public void copiesOnlyFilesFromSourcesForProjection() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("sources/a.smithy").getPath())
                .addImport(getClass().getResource("sources/b.smithy").getPath())
                .addImport(getClass().getResource("sources/c/c.json").getPath())
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
                .sources(ListUtils.of(Paths.get(getClass().getResource("sources/a.smithy").getPath()).getParent()))
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
    public void treatsNewlyAddedShapesAsNewSources() {
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
                .sources(ListUtils.of(Paths.get(getClass().getResource("sources/a.smithy").getPath()).getParent()))
                .build();
        new SourcesPlugin().execute(context);
        String modelString = manifest.getFileString("model.json").get();

        assertThat(modelString, containsString("MyString"));
    }

    @Test
    public void doesNotAllowConflicts() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("sources/a.smithy").getPath())
                .addImport(getClass().getResource("conflicting/a.smithy").getPath())
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        PluginContext context = PluginContext.builder()
                .fileManifest(manifest)
                .model(model)
                .originalModel(model)
                .sources(ListUtils.of(
                        Paths.get(getClass().getResource("sources/a.smithy").getPath()),
                        Paths.get(getClass().getResource("conflicting/a.smithy").getPath())))
                .build();

        Assertions.assertThrows(SourcesConflictException.class, () -> new SourcesPlugin().execute(context));
    }
}
