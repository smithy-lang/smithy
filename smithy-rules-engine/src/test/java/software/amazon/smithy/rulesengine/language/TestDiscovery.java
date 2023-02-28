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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.eval.TestEvaluator;
import software.amazon.smithy.rulesengine.traits.EndpointTestCase;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 *
 */
@SmithyUnstableApi
public final class TestDiscovery {
    private static final Map<String, EndpointRuleSet> VALID_RULE_SETS = new TreeMap<>();
    private static final Map<String, RulesTestSuite> VALID_TEST_SUITES = new TreeMap<>();

    static Stream<Path> getValidRuleSetFiles() throws IOException {
        return Files.list(Paths.get(TestDiscovery.class.getResource("valid-rules").getPath()));
    }

    static Map<String, EndpointRuleSet> getValidRuleSets() throws IOException {
        if (VALID_RULE_SETS.isEmpty()) {
            try (Stream<Path> paths = getValidRuleSetFiles()) {
                    paths.forEach(path -> {
                        EndpointRuleSet ruleSet = EndpointRuleSet.fromNode(
                                Node.parse(IoUtils.readUtf8File(path), path.toString()));
                        String fileName = path.getFileName().toString();

                        if (VALID_RULE_SETS.containsKey(fileName)) {
                            throw new RuntimeException("Duplicate service ids for valid rule sets discovered: " + fileName);
                        }
                        VALID_RULE_SETS.put(fileName, ruleSet);
                    });
            }
        }

        return VALID_RULE_SETS;
    }

    static Stream<Path> getInvalidRuleSetFiles() throws IOException {
        return Files.list(Paths.get(TestDiscovery.class.getResource("invalid-rules").getPath()));
    }

    /**
     * Provides a stream of {@link RulesTestSuite} that can be used a rule-set engine's implementation compliance.
     *
     * @return a stream of {@link RulesTestSuite}.
     */
    static Map<String, RulesTestSuite> getValidTestSuites() throws IOException {
        if (VALID_TEST_SUITES.isEmpty()) {
            // Load all the test cases into an id keyed map for later lookup, fail on duplicates.
            Set<String> fileNames = new HashSet<>();
            Map<String, EndpointTestsTrait> testSuites = Files.list(Paths.get(
                            TestDiscovery.class.getResource("test-cases").getPath()))
                    .map(path -> {
                        EndpointTestsTrait testsTrait = EndpointTestsTrait.fromNode(
                                Node.parse(IoUtils.readUtf8File(path), path.toString()));
                        String fileName = path.getFileName().toString();

                        if (!fileNames.add(fileName)) {
                            throw new RuntimeException("Duplicate service ids for test cases discovered: " + fileName);
                        }

                        return MapUtils.entry(fileName, testsTrait);
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // Build test suites for valid rule sets, fail on duplicate rule sets.

            EndpointTestsTrait emptyTestsTrait = EndpointTestsTrait.builder().version("1.0").build();
            for (Map.Entry<String, EndpointRuleSet> endpointRuleSet : getValidRuleSets().entrySet()) {
                EndpointTestsTrait suite = testSuites.getOrDefault(endpointRuleSet.getKey(), emptyTestsTrait);
                VALID_TEST_SUITES.put(endpointRuleSet.getKey(), new RulesTestSuite(endpointRuleSet.getValue(), suite));
            }
            }
        return VALID_TEST_SUITES;
    }

    public static RulesTestSuite getTestSuite(String name) throws IOException {
        return getValidTestSuites().get(name);
    }

    public static EndpointRuleSet getMinimalEndpointRuleSet() throws IOException {
        return getValidRuleSets().get("minimal-ruleset.json");
    }

    public static final class RulesTestCase {
        private final EndpointRuleSet ruleset;
        private final EndpointTestCase testcase;

        public RulesTestCase(EndpointRuleSet ruleset, EndpointTestCase testcase) {
            this.ruleset = ruleset;
            this.testcase = testcase;
        }

        public EndpointRuleSet ruleset() {
            return ruleset;
        }

        public EndpointTestCase testcase() {
            return testcase;
        }

        public void execute() {
            TestEvaluator.evaluate(ruleset, testcase);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ruleset, testcase);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            RulesTestCase that = (RulesTestCase) obj;
            return Objects.equals(this.ruleset, that.ruleset)
                   && Objects.equals(this.testcase, that.testcase);
        }

        @Override
        public String toString() {
            return String.format("%s (%s:%s)", testcase.getDocumentation().orElse(""),
                    testcase.getSourceLocation().getFilename(), testcase.getSourceLocation().getLine());
        }
    }

    public static final class RulesTestSuite {
        private final EndpointRuleSet ruleSet;
        private final EndpointTestsTrait testSuite;

        public RulesTestSuite(EndpointRuleSet ruleSet, EndpointTestsTrait testSuite) {
            this.ruleSet = ruleSet;
            this.testSuite = testSuite;
        }

        public EndpointRuleSet getRuleSet() {
            return ruleSet;
        }

        public EndpointTestsTrait getTestSuite() {
            return testSuite;
        }

        @Override
        public int hashCode() {
            return Objects.hash(ruleSet, testSuite);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            RulesTestSuite that = (RulesTestSuite) obj;
            return Objects.equals(this.ruleSet, that.ruleSet) && Objects.equals(this.testSuite, that.testSuite);
        }

        @Override
        public String toString() {
            return ruleSet.getSourceLocation().toString();
        }
    }
}
