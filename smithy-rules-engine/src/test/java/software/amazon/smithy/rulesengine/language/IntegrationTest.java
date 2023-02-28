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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.traits.EndpointTestCase;
import software.amazon.smithy.rulesengine.validators.StandaloneRulesetValidator;
import software.amazon.smithy.rulesengine.validators.ValidationError;
import software.amazon.smithy.utils.IoUtils;

public class IntegrationTest {
    public static Stream<ValidationTestCase> validRules() throws IOException {
        return TestDiscovery.getValidRuleSetFiles()
                .map(path -> new ValidationTestCase(path, path.resolve("../" + path.getFileName())));
    }

    public static Stream<ValidationTestCase> invalidRules() throws IOException {
        return TestDiscovery.getInvalidRuleSetFiles().map(path -> new ValidationTestCase(path, null));
    }

    public static List<TestDiscovery.RulesTestCase> checkableTestCases() throws IOException {
        List<TestDiscovery.RulesTestCase> testCases = new ArrayList<>();
        for (TestDiscovery.RulesTestSuite testSuite : TestDiscovery.getValidTestSuites().values()) {
            for (EndpointTestCase testCase : testSuite.getTestSuite().getTestCases()) {
                testCases.add(new TestDiscovery.RulesTestCase(testSuite.getRuleSet(), testCase));
            }
        }
        return testCases;
    }

    @Test
    public void checkTestSuites() throws IOException {
        for (TestDiscovery.RulesTestSuite rulesTestSuite : TestDiscovery.getValidTestSuites().values()) {
            assertEquals(Collections.emptyList(),
                    StandaloneRulesetValidator.validate(rulesTestSuite.getRuleSet(), rulesTestSuite.getTestSuite())
                            .collect(Collectors.toList()));
        }
    }

    public static Stream<ValidationTestCase> invalidStandaloneValidationTestCases() {
        URL url = IntegrationTest.class.getResource("invalid-standalone-validation-rules");
        assert url != null;
        return Arrays.stream(Objects.requireNonNull(new File(url.getPath()).listFiles()))
                .map(path -> new ValidationTestCase(path.toPath(), null));
    }

    @ParameterizedTest
    @MethodSource("validRules")
    public void checkValidRules(ValidationTestCase validationTestCase) {
        EndpointRuleSet ruleset = EndpointRuleSet.fromNode(validationTestCase.contents());
        List<ValidationError> errors = StandaloneRulesetValidator.validate(ruleset, null).collect(Collectors.toList());
        assertEquals(Collections.emptyList(), errors);
        ruleset.typeCheck(new Scope<>());
    }

    @ParameterizedTest
    @MethodSource("validRules")
    public void rulesRoundTripViaJson(ValidationTestCase validationTestCase) {
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
    public void executeTestSuite(TestDiscovery.RulesTestCase testcase) {
        testcase.execute();
    }

    @ParameterizedTest
    @MethodSource("invalidRules")
    public void checkInvalidRules(ValidationTestCase validationTestCase) throws IOException {
        RuleError error = assertThrows(RuleError.class, () -> {
            EndpointRuleSet.fromNode(validationTestCase.contents());
        });

        assertEquals(validationTestCase.comments(), error.toString());
    }

    @ParameterizedTest
    @MethodSource("invalidStandaloneValidationTestCases")
    void checkInvalidStandaloneValidationRules(ValidationTestCase validationTestCase) throws IOException {
        EndpointRuleSet ruleset = EndpointRuleSet.fromNode(validationTestCase.contents());
        List<ValidationError> errors = StandaloneRulesetValidator.validate(ruleset, null)
                .collect(Collectors.toList());
        assertFalse(errors.isEmpty());

        String concatenatedErrors = errors.stream()
                .map(ValidationError::error)
                .collect(Collectors.joining(" "))
                .replaceAll("\\s+", " ").trim();
        assertEquals(
                validationTestCase.comments().replaceAll("\\s+", " ").trim(),
                concatenatedErrors);
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

        String comments() throws IOException {
            try (Stream<String> lines = Files.lines(path)) {
                return lines.filter(line -> line.startsWith("// "))
                               .map(line -> line.substring(3))
                               .collect(Collectors.joining("\n"));
            }
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
            return path.getFileName().toString();
        }

    }

}
