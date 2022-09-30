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

package software.amazon.smithy.rulesengine.testutil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.eval.TestEvaluator;
import software.amazon.smithy.rulesengine.traits.EndpointTestCase;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 *
 */
@SmithyUnstableApi
public final class TestDiscovery {

    private static String getRegexSafeFileSeparator() {
        return File.separatorChar == '\\' ? "\\\\" : File.separator;
    }

    private List<Node> listDirectory(String manifest, String base) {
        List<Node> filenames = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream(manifest)), StandardCharsets.UTF_8))) {
            String resource;

            while ((resource = br.readLine()) != null) {
                String path = Paths.get(base, resource)
                        .toString();
                filenames.add(Node.parse(IoUtils.toUtf8String(Objects.requireNonNull(getClass()
                        .getResourceAsStream(path))), path));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return filenames;
    }

    private String serviceId(FromSourceLocation source) {
        return source.getSourceLocation().getFilename().split(getRegexSafeFileSeparator())[1];
    }

    /**
     * Provides a stream of {@link RulesTestSuite} that can be used a rule-set engine's implementation compliance.
     *
     * @return a stream of {@link RulesTestSuite}.
     */
    public Stream<RulesTestSuite> testSuites() {
        List<Node> rulesetNodes = listDirectory("valid-rules/manifest.txt", "valid-rules");
        List<Node> testSuiteFiles = listDirectory("test-cases/manifest.txt", "test-cases");
        List<EndpointRuleSet> ruleSets = rulesetNodes.stream()
                .map(EndpointRuleSet::fromNode)
                .collect(Collectors.toList());
        List<String> rulesetIds = ruleSets.stream()
                .map(this::serviceId)
                .collect(Collectors.toList());
        if (rulesetIds.stream()
                    .distinct()
                    .count() != ruleSets.size()) {
            throw new RuntimeException(String.format("Duplicate service ids discovered: %s", ruleSets.stream()
                    .map(EndpointRuleSet::getSourceLocation)
                    .sorted()
                    .collect(Collectors.toList())));
        }

        List<EndpointTestsTrait> testSuites = testSuiteFiles.stream()
                .map(EndpointTestsTrait::fromNode)
                .collect(Collectors.toList());
        testSuites.stream()
                .filter(testSuite -> !rulesetIds.contains(serviceId(testSuite)))
                .forEach(bad -> {
                    throw new RuntimeException("did not find service for " + serviceId(bad));
                });
        return ruleSets.stream()
                .map(ruleset -> {
                    List<EndpointTestsTrait> matchingTestSuites = testSuites.stream()
                            .filter(test -> serviceId(test).equals(serviceId(ruleset)))
                            .collect(Collectors.toList());
                    EndpointTestsTrait suite;
                    if (matchingTestSuites.size() > 1) {
                        throw new RuntimeException("found duplicate test suites...");
                    } else if (matchingTestSuites.size() == 1) {
                        suite = matchingTestSuites.get(0);
                    } else {
                        suite = EndpointTestsTrait.builder().version("1.0").build();
                    }
                    return new RulesTestSuite(ruleset, suite);
                });
    }

    /**
     * Retrieves a {@link RulesTestSuite} by name.
     *
     * @param name the test name.
     * @return the {@link RulesTestSuite}>
     */
    public RulesTestSuite getTestSuite(String name) {
        return new RulesTestSuite(rulesetFromPath(name), testSuiteFromPath(name));
    }

    private EndpointRuleSet rulesetFromPath(String name) {
        return EndpointRuleSet.fromNode(Node.parse(Objects.requireNonNull(this.getClass()
                .getResourceAsStream(String.format("valid-rules/%s", name))), name));
    }

    private EndpointTestsTrait testSuiteFromPath(String name) {
        return EndpointTestsTrait.fromNode(Node.parse(Objects.requireNonNull(this.getClass()
                .getResourceAsStream(String.format("test-cases/%s", name))), name));
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

        public EndpointRuleSet ruleSet() {
            return ruleSet;
        }

        public EndpointTestsTrait testSuite() {
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
            return Objects.equals(this.ruleSet, that.ruleSet)
                   && Objects.equals(this.testSuite, that.testSuite);
        }

        @Override
        public String toString() {
            return ruleSet.getSourceLocation()
                    .toString();
        }

    }


}
