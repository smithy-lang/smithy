package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.ListUtils;

public class ModelDiscoveryTest {
    @Test
    public void discoversModelsInManifests() throws MalformedURLException {
        URL manifest = getClass().getResource("manifest-valid");
        String prefix = manifest.toString().substring(0, manifest.toString().length() - "manifest".length());
        List<URL> models = ModelDiscovery.findModels(ListUtils.of(manifest));

        assertThat(models, contains(
                new URL(prefix + "foo.smithy"),
                new URL(prefix + "baz/bar/example.json"),
                new URL(prefix + "test"),
                new URL(prefix + "test2")));
    }

    @Test
    public void parsesEmptyManifest() {
        URL manifest = getClass().getResource("manifest-empty");
        List<URL> models = ModelDiscovery.findModels(ListUtils.of(manifest));

        assertThat(models, empty());
    }

    @Test
    public void prohibitsLeadingSlash() {
        Assertions.assertThrows(ModelManifestException.class, () -> {
            URL manifest = getClass().getResource("manifest-prohibits-leading-slash");
            ModelDiscovery.findModels(ListUtils.of(manifest));
        });
    }

    @Test
    public void prohibitsTrailingSlash() {
        Assertions.assertThrows(ModelManifestException.class, () -> {
            URL manifest = getClass().getResource("manifest-prohibits-trailing-slash");
            ModelDiscovery.findModels(ListUtils.of(manifest));
        });
    }

    @Test
    public void prohibitsEmptySegments() {
        Assertions.assertThrows(ModelManifestException.class, () -> {
            URL manifest = getClass().getResource("manifest-prohibits-empty-segments");
            ModelDiscovery.findModels(ListUtils.of(manifest));
        });
    }

    @Test
    public void prohibitsDotSegments() {
        Assertions.assertThrows(ModelManifestException.class, () -> {
            URL manifest = getClass().getResource("manifest-prohibits-dot-segments");
            ModelDiscovery.findModels(ListUtils.of(manifest));
        });
    }

    @Test
    public void prohibitsDotDotSegments() {
        Assertions.assertThrows(ModelManifestException.class, () -> {
            URL manifest = getClass().getResource("manifest-prohibits-dot-dot-segments");
            ModelDiscovery.findModels(ListUtils.of(manifest));
        });
    }

    @Test
    public void prohibitsSpecialCharacters() {
        Assertions.assertThrows(ModelManifestException.class, () -> {
            URL manifest = getClass().getResource("manifest-prohibits-special-characters");
            ModelDiscovery.findModels(ListUtils.of(manifest));
        });
    }
}
