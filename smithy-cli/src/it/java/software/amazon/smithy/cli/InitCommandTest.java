package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;

import java.io.IOException;
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
    public void usesDefaultTemplate() {
        IntegUtils.withProject(PROJECT_NAME, templatesDir -> {
            setupTemplatesDirectory(templatesDir);

            IntegUtils.withTempDir("defaultTemplate", dir -> {
                RunResult result = IntegUtils.run(
                        dir, ListUtils.of("init", "-u", templatesDir.toString()));
                assertThat(result.getOutput(),
                        containsString("Smithy project created in directory: quickstart-cli"));
                assertThat(result.getExitCode(), is(0));
            });
        });
    }

    @Test
    public void missingTemplate() {
        IntegUtils.withProject(PROJECT_NAME, templatesDir -> {
            setupTemplatesDirectory(templatesDir);

            IntegUtils.withTempDir("emptyTemplateName", dir -> {
                RunResult result = IntegUtils.run(
                    dir, ListUtils.of("init", "-t", "", "-u", templatesDir.toString()));
                assertThat(result.getOutput(),
                    containsString("Please specify a template name using `--template` or `-t`"));
                assertThat(result.getExitCode(), is(1));
            });
        });
    }

    @Test
    public void includedFileJson() {
        IntegUtils.withProject(PROJECT_NAME, templatesDir -> {
            setupTemplatesDirectory(templatesDir);

            IntegUtils.withTempDir("includedFiles", dir -> {
                RunResult result = IntegUtils.run(
                    dir, ListUtils.of("init", "-t", "included-file-json", "-u", templatesDir.toString()));
                assertThat(result.getOutput(),
                    containsString("Smithy project created in directory: "));
                assertThat(result.getExitCode(), is(0));
                assertThat(Files.exists(Paths.get(dir.toString(), "included-file-json")), is(true));
                assertThat(Files.exists(Paths.get(dir.toString(), "included-file-json/smithy-build.json")), is(true));
            });
        });
    }

    @Test
    public void includedFileGradle() {
        IntegUtils.withProject(PROJECT_NAME, templatesDir -> {
            setupTemplatesDirectory(templatesDir);

            IntegUtils.withTempDir("includedFilesGradle", dir -> {
                RunResult result = IntegUtils.run(
                    dir, ListUtils.of("init", "-t", "included-files-gradle", "-u", templatesDir.toString()));
                assertThat(result.getOutput(),
                    containsString("Smithy project created in directory: "));
                assertThat(result.getExitCode(), is(0));
                assertThat(Files.exists(Paths.get(dir.toString(), "included-files-gradle")), is(true));
                try {
                    Files.walk(Paths.get(dir.toString())).forEach(System.out::println);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                assertThat(Files.exists(Paths.get(dir.toString(), "included-files-gradle/gradle.properties")), is(true));
                assertThat(Files.exists(Paths.get(dir.toString(), "included-files-gradle/gradlew")), is(true));
                assertThat(Files.exists(Paths.get(dir.toString(), "included-files-gradle/gradle")), is(true));
                assertThat(Files.exists(Paths.get(dir.toString(), "included-files-gradle/gradle/wrapper")), is(true));
                assertThat(Files.exists(Paths.get(dir.toString(), "included-files-gradle/gradle/wrapper/gradle-wrapper.jar")), is(true));
                assertThat(Files.exists(Paths.get(dir.toString(), "included-files-gradle/gradle/wrapper/gradle-wrapper.properties")), is(true));
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

                String expectedOutput = new StringBuilder()
                        .append("Invalid template `blabla`. `Smithy-Examples` provides the following templates:")
                        .append(System.lineSeparator())
                        .append(System.lineSeparator())
                        .append("─────────────────────  ─────────────────────────────────────────────────────────────────────────────")
                        .append(System.lineSeparator())
                        .append("NAME                   DOCUMENTATION")
                        .append(System.lineSeparator())
                        .append("─────────────────────  ─────────────────────────────────────────────────────────────────────────────")
                        .append(System.lineSeparator())
                        .append("bad-include-path       Template with incorrect include path. Should throw error.")
                        .append(System.lineSeparator())
                        .append("bad-template-path      Template with incorrect path. Should throw error.")
                        .append(System.lineSeparator())
                        .append("included-file-json     Smithy Quickstart example with json file included.")
                        .append(System.lineSeparator())
                        .append("included-files-gradle  Smithy Quickstart example with gradle files included.")
                        .append(System.lineSeparator())
                        .append("quickstart-cli         Smithy Quickstart example weather service.")
                        .append(System.lineSeparator())
                        .toString();

                assertThat(result.getOutput(), containsString(expectedOutput));
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

            IntegUtils.withTempDir("withNestedDirectoryArg", dir -> {
                RunResult result = IntegUtils.run(dir, ListUtils.of(
                    "init", "-t", "quickstart-cli", "-o", "./hello/world", "-u", templatesDir.toString()));
                assertThat(result.getOutput(),
                    containsString("Smithy project created in directory: ./hello/world"));
                assertThat(result.getExitCode(), is(0));
                assertThat(Files.exists(Paths.get(dir.toString(), "./hello/world")), is(true));
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

    @Test
    public void withListArg() {
        IntegUtils.withProject(PROJECT_NAME, templatesDir -> {
            setupTemplatesDirectory(templatesDir);

            IntegUtils.withTempDir("withListArg", dir -> {
                RunResult result = IntegUtils.run(dir, ListUtils.of(
                    "init", "--list", "--url", templatesDir.toString()));

                String expectedOutput = new StringBuilder()
                        .append("─────────────────────  ─────────────────────────────────────────────────────────────────────────────")
                        .append(System.lineSeparator())
                        .append("NAME                   DOCUMENTATION")
                        .append(System.lineSeparator())
                        .append("─────────────────────  ─────────────────────────────────────────────────────────────────────────────")
                        .append(System.lineSeparator())
                        .append("bad-include-path       Template with incorrect include path. Should throw error.")
                        .append(System.lineSeparator())
                        .append("bad-template-path      Template with incorrect path. Should throw error.")
                        .append(System.lineSeparator())
                        .append("included-file-json     Smithy Quickstart example with json file included.")
                        .append(System.lineSeparator())
                        .append("included-files-gradle  Smithy Quickstart example with gradle files included.")
                        .append(System.lineSeparator())
                        .append("quickstart-cli         Smithy Quickstart example weather service.")
                        .append(System.lineSeparator())
                        .toString();

                assertThat(result.getOutput(), containsString(expectedOutput));
                assertThat(result.getExitCode(), is(0));
            });
        });
    }

    @Test
    public void outputDirectoryAlreadyExists() {
        IntegUtils.withProject(PROJECT_NAME, templatesDir -> {
            setupTemplatesDirectory(templatesDir);

            IntegUtils.withTempDir("existingOutputDir", dir -> {
                Path existingPath = null;
                RunResult result;
                try {
                    existingPath = Files.createDirectory(dir.resolve("quickstart-cli"));
                    result = IntegUtils.run(
                            dir, ListUtils.of("init", "-t", "quickstart-cli", "-u", templatesDir.toString()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    IoUtils.rmdir(existingPath);
                }

                assertThat(result.getOutput(), containsString("Output directory `quickstart-cli` already exists."));
                assertThat(result.getExitCode(), is(1));
            });
        });
    }

    @Test
    public void executesInitSuccessQuiet() {
        IntegUtils.withProject(PROJECT_NAME, templatesDir -> {
            setupTemplatesDirectory(templatesDir);

            IntegUtils.withTempDir("exitZeroQuiet", dir -> {
                RunResult result = IntegUtils.run(
                        dir, ListUtils.of("init", "--quiet", "-t", "quickstart-cli", "-u", templatesDir.toString()));

                assertThat(result.getOutput().trim(), emptyString());
                assertThat(result.getExitCode(), is(0));
                assertThat(Files.exists(Paths.get(dir.toString(), "quickstart-cli")), is(true));
            });
        });
    }

    @Test
    public void badTemplatePathFailureExpected() {
        IntegUtils.withProject(PROJECT_NAME, templatesDir -> {
            setupTemplatesDirectory(templatesDir);

            IntegUtils.withTempDir("badTemplatePath", dir -> {
                RunResult result = IntegUtils.run(
                        dir, ListUtils.of("init", "-t", "bad-template-path", "-u", templatesDir.toString()));
                assertThat(Files.exists(Paths.get(dir.toString(), "bad-template-path")), is(false));
                assertThat(result.getExitCode(), is(1));
                assertThat(result.getOutput(),
                        containsString("Template path `getting-started-example/does-not-exist` for template"
                                +" \"bad-template-path\" is invalid."));
            });
        });
    }

    @Test
    public void badIncludePathFailureExpected() {
        IntegUtils.withProject(PROJECT_NAME, templatesDir -> {
            setupTemplatesDirectory(templatesDir);

            IntegUtils.withTempDir("badIncludePath", dir -> {
                RunResult result = IntegUtils.run(
                        dir, ListUtils.of("init", "-t", "bad-include-path", "-u", templatesDir.toString()));
                assertThat(Files.exists(Paths.get(dir.toString(), " bad-include-path")), is(false));
                assertThat(result.getExitCode(), is(1));
                assertThat(result.getOutput(),
                        containsString("File or directory `getting-started-example/does-not-exist` is marked"
                                + " for inclusion in template \"bad-include-path\", but was not found"));
            });
        });
    }

    @Test
    public void cacheCreatedOnFirstCreationOfTemplate() {
        IntegUtils.clearCacheDirIfExists();
        IntegUtils.withProject(PROJECT_NAME, root -> {
            try {
                RunResult resultFirst = IntegUtils.run(root, ListUtils.of("init", "-o", "hello-world"));
                assertTrue(Files.exists(IntegUtils.SMITHY_TEMPLATE_CACHE_PATH)
                        && Files.isDirectory(IntegUtils.SMITHY_TEMPLATE_CACHE_PATH));
                assertThat(resultFirst.getExitCode(), equalTo(0));
                assertThat(resultFirst.getOutput(),
                        containsString("template repo cloned"));
                assertThat(resultFirst.getOutput(),
                        containsString("Smithy project created in directory: hello-world"));
                IoUtils.rmdir(root.resolve("hello-world"));

                RunResult resultSecond = IntegUtils.run(root, ListUtils.of("init", "-o", "hello-world"));
                assertTrue(Files.exists(IntegUtils.SMITHY_TEMPLATE_CACHE_PATH)
                        && Files.isDirectory(IntegUtils.SMITHY_TEMPLATE_CACHE_PATH));
                assertThat(resultSecond.getExitCode(), equalTo(0));
                assertThat(resultSecond.getOutput(), not(containsString("template repo cloned")));
                assertThat(resultSecond.getOutput(),
                        containsString("Smithy project created in directory: hello-world"));
            } finally {
                IntegUtils.clearCacheDirIfExists();
            }
        });
    }

    @Test
    public void noCacheCreatedWhenLocalRepo() {
        IntegUtils.clearCacheDirIfExists();
        IntegUtils.withProject(PROJECT_NAME, templatesDir -> {
            setupTemplatesDirectory(templatesDir);

            IntegUtils.withTempDir("exitZero", dir -> {
                RunResult result = IntegUtils.run(
                        dir, ListUtils.of("init", "-t", "quickstart-cli", "-u", templatesDir.toString()));
                assertThat(result.getOutput(),
                        containsString("Smithy project created in directory: quickstart-cli"));
                assertThat(result.getExitCode(), is(0));
                assertThat(Files.exists(Paths.get(dir.toString(), "quickstart-cli")), is(true));

                assertFalse(Files.exists(IntegUtils.SMITHY_TEMPLATE_CACHE_PATH));
                assertFalse(Files.exists(IntegUtils.SMITHY_ROOT_CACHE_PATH));
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
