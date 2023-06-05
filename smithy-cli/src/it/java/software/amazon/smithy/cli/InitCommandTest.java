package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class InitCommandTest {
    private static final String PROJECT_NAME = "smithy-templates";

    @Test
    public void init() {
        IntegUtils.withProject(PROJECT_NAME, templatesDir -> {
            setupTemplatesDirectory(templatesDir);

            IntegUtils.withTempDir("exitZero", dir -> {
                RunResult result = IntegUtils.run(
                        dir, ListUtils.of("init", "-t", "quickstart-cli", "-u", templatesDir.toString()));
                assertThat(result.getOutput(),
                    containsString("Smithy project created in directory: quickstart-cli"));
                assertThat(result.getExitCode(), is(0));
                assertThat(Files.exists(Paths.get(dir.toString(), "quickstart-cli")), is(true));
            });
        });
    }

    @Test
    public void missingTemplate() {
        IntegUtils.withProject(PROJECT_NAME, templatesDir -> {
            setupTemplatesDirectory(templatesDir);

            IntegUtils.withTempDir("missingTemplate", dir -> {
                RunResult result = IntegUtils.run(
                    dir, ListUtils.of("init", "-u", templatesDir.toString()));
                assertThat(result.getOutput(),
                    containsString("Please specify a template using `--template` or `-t`"));
                assertThat(result.getExitCode(), is(1));
            });

            IntegUtils.withTempDir("emptyTemplateName", dir -> {
                RunResult result = IntegUtils.run(
                    dir, ListUtils.of("init", "-t", "", "-u", templatesDir.toString()));
                assertThat(result.getOutput(),
                    containsString("Please specify a template using `--template` or `-t`"));
                assertThat(result.getExitCode(), is(1));
            });
        });
    }

    @Test
    public void unexpectedTemplate() {
        IntegUtils.withProject(PROJECT_NAME, templatesDir -> {
            setupTemplatesDirectory(templatesDir);

            IntegUtils.withTempDir("unexpectedTemplate", dir -> {
                RunResult result = IntegUtils.run(
                    dir, ListUtils.of("init", "-t", "blabla", "-u", templatesDir.toString()));
                assertThat(result.getOutput(),
                    containsString("Missing expected member `blabla` from `templates` object ([3, 18])"));
                assertThat(result.getExitCode(), is(1));
            });
        });
    }

    @Test
    public void withDirectoryArg() {
        IntegUtils.withProject(PROJECT_NAME, templatesDir -> {
            setupTemplatesDirectory(templatesDir);

            IntegUtils.withTempDir("withDirectoryArg", dir -> {
                RunResult result = IntegUtils.run(dir, ListUtils.of(
                    "init", "-t", "quickstart-cli", "-o", "hello-world", "-u", templatesDir.toString()));
                assertThat(result.getOutput(),
                    containsString("Smithy project created in directory: hello-world"));
                assertThat(result.getExitCode(), is(0));
                assertThat(Files.exists(Paths.get(dir.toString(), "hello-world")), is(true));
            });
        });
    }

    @Test
    public void withLongHandArgs() {
        IntegUtils.withProject(PROJECT_NAME, templatesDir -> {
            setupTemplatesDirectory(templatesDir);

            IntegUtils.withTempDir("withLongHandArgs", dir -> {
                RunResult result = IntegUtils.run(dir, ListUtils.of(
                    "init", "--template", "quickstart-cli", "--output", "hello-world", "--url",
                        templatesDir.toString()));
                assertThat(result.getOutput(),
                        containsString("Smithy project created in directory: hello-world"));
                assertThat(result.getExitCode(), is(0));
                assertThat(Files.exists(Paths.get(dir.toString(), "hello-world")), is(true));
            });
        });
    }

    private static void run(List<String> args, Path root) {
        StringBuilder output = new StringBuilder();
        int result = IoUtils.runCommand(args, root, output, Collections.emptyMap());
        if (result != 0) {
            throw new RuntimeException("Error running command: " + args + ": " + output);
        }
    }

    private void setupTemplatesDirectory(Path dir) {
        run(ListUtils.of("git", "init"), dir);
        run(ListUtils.of("git", "config", "user.email", "you@example.com"), dir);
        run(ListUtils.of("git", "config", "user.name", "Your Name"), dir);
        run(ListUtils.of("git", "checkout", "-b", "main"), dir);
        run(ListUtils.of("git", "add", "-A"), dir);
        run(ListUtils.of("git", "commit", "-m", "Foo"), dir);
    }
}
