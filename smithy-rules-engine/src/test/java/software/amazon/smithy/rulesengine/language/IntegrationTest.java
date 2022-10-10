/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.rulesengine.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.smithy.rulesengine.language.util.StringUtils.lines;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.testutil.TestDiscovery;
import software.amazon.smithy.rulesengine.validators.StandaloneRulesetValidator;
import software.amazon.smithy.rulesengine.validators.ValidationError;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.StringUtils;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {

    private Stream<ValidationTestCase> validTestcases() {
        URL url = Thread.currentThread()
                .getContextClassLoader()
                .getResource("software/amazon/smithy/rulesengine/testutil/valid-rules");
        File testCases = new File(Thread.currentThread()
                .getContextClassLoader()
                .getResource("software/amazon/smithy/rulesengine/testutil/test-cases")
                .getPath());
        assert url != null;
        return Arrays.stream(Objects.requireNonNull(new File(url.getPath()).listFiles()))
                .filter(path -> path.toString()
                        .endsWith(".json"))
                .map(path -> new ValidationTestCase(path.toPath(), Paths.get(testCases.toPath()
                        .toString(), path.getName())));
    }

    private Stream<TestDiscovery.RulesTestCase> checkableTestCases() {
        return new TestDiscovery().testSuites()
                .flatMap(
                        suite -> suite.testSuite().getTestCases()
                                .stream()
                                .map(tc -> new TestDiscovery.RulesTestCase(suite.ruleSet(), tc)));
    }

    private Stream<ValidationTestCase> invalidTestCases() {
        URL url = getClass().getResource("invalid-rules");
        assert url != null;
        return Arrays.stream(Objects.requireNonNull(new File(url.getPath()).listFiles()))
                .map(path -> new ValidationTestCase(path.toPath(), null));
    }

    @ParameterizedTest
    @MethodSource("validTestcases")
    void checkValidRules(ValidationTestCase validationTestCase) {
        EndpointRuleSet ruleset = EndpointRuleSet.fromNode(validationTestCase.contents());
        List<ValidationError> errors = StandaloneRulesetValidator.validate(ruleset, null).collect(Collectors.toList());
        assertEquals(Collections.emptyList(), errors);
        ruleset.typeCheck(new Scope<>());
    }

    @ParameterizedTest
    @MethodSource("validTestcases")
    void rulesRoundTripViaJson(ValidationTestCase validationTestCase) {
        EndpointRuleSet ruleset = EndpointRuleSet.fromNode(validationTestCase.contents());
        Node serialized = ruleset.toNode();
        EndpointRuleSet deserialized = EndpointRuleSet.fromNode(serialized);
        if (!deserialized.equals(ruleset)) {
            assertEquals(ruleset.toString(), deserialized.toString());
            assertEquals(ruleset, deserialized);
        }

    }

    @ParameterizedTest
    @MethodSource("checkableTestCases")
    void checkTestSuites(TestDiscovery.RulesTestCase testcase) {
        new TestDiscovery().testSuites().forEach(rulesTestSuite -> {
            assertEquals(Collections.emptyList(), StandaloneRulesetValidator.validate(rulesTestSuite.ruleSet(),
                    rulesTestSuite.testSuite()).collect(Collectors.toList()));
        });
    }

    @ParameterizedTest
    @MethodSource("checkableTestCases")
    void executeTestSuite(TestDiscovery.RulesTestCase testcase) {
        testcase.execute();
    }

    @ParameterizedTest
    @MethodSource("invalidTestCases")
    void checkInvalidRules(ValidationTestCase validationTestCase) throws IOException {
        RuleError error = assertThrows(RuleError.class, () -> {
            EndpointRuleSet ruleset = EndpointRuleSet.fromNode(validationTestCase.contents());
            ruleset.typeCheck(new Scope<>());
        });
        //validationTestCase.overrideComments(error.toString());
        assertEquals(
                validationTestCase.comments().replaceAll("\\s+", " ").trim(),
                error.toString().replaceAll("\\s+", " ").trim());
    }

    public static final class ValidationTestCase {
        private final Path path;
        private final Path testCase;

        public ValidationTestCase(Path path, Path testCase) {
            System.out.println("Loading path: " + path);
            this.path = path;
            this.testCase = testCase;
        }

        Node contents() {
            try {
                return Node.parseJsonWithComments(IoUtils.toUtf8String(new FileInputStream(path.toFile())), path.subpath(path.getNameCount() - 2, path.getNameCount())
                        .toString().replace("\\", "/"));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        Optional<Node> testNode() {
            return Optional.ofNullable(testCase)
                    .filter(name -> name.toFile()
                            .exists())
                    .map(testPath -> {
                        try {
                            return Node.parseJsonWithComments(IoUtils.toUtf8String(new FileInputStream(testPath.toFile())), testPath.subpath(testPath.getNameCount() - 2, testPath.getNameCount())
                                    .toString());
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        String comments() throws FileNotFoundException {
            return lines(IoUtils.toUtf8String(new FileInputStream(path.toFile()))).stream()
                    .filter(line -> line.startsWith("// "))
                    .map(line -> line.substring(3))
                    .collect(Collectors.joining("\n"));
        }

        /**
         * Write back the current error message into the test case
         */
        void overrideComments(String comments) throws IOException {
            String commentSection = lines(comments).stream()
                                            .map(line -> "// " + line)
                                            .collect(Collectors.joining("\n")) + "\n";
            String newContents = commentSection + lines(IoUtils.toUtf8String(new FileInputStream(path.toFile())))
                    .stream()
                    .filter(line -> !line.startsWith("// "))
                    .collect(Collectors.joining("\n"));
            Path realPath = Paths.get(path.toString().replaceFirst("/build/resources/test/",
                    "/src/test/resources/"));
            if (!Files.exists(realPath)) {
                throw new RuntimeException("writeback path must exist! %s " + realPath);
            }
            Files.delete(realPath);
            Files.write(realPath, newContents.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW, StandardOpenOption.SYNC);
        }

        public Path path() {
            return path;
        }

        public Path testCase() {
            return testCase;
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, testCase);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            ValidationTestCase that = (ValidationTestCase) obj;
            return Objects.equals(this.path, that.path) &&
                   Objects.equals(this.testCase, that.testCase);
        }

        @Override
        public String toString() {
            return path.getFileName()
                    .toString();
        }

    }

}
