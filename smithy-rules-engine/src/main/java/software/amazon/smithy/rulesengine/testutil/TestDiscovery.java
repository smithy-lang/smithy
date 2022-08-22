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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.EndpointRuleset;
import software.amazon.smithy.rulesengine.language.EndpointTest;
import software.amazon.smithy.rulesengine.language.EndpointTestSuite;
import software.amazon.smithy.utils.IoUtils;

public class TestDiscovery {
    private static String prettyPath(Path path) {
        return path.subpath(path.getNameCount() - 2, path.getNameCount())
                .toString();
    }

    private List<Node> listDirectory(String manifest, String base) {
        List<Node> filenames = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(this.getClass().getResourceAsStream(manifest)), StandardCharsets.UTF_8))) {
            String resource;

            while ((resource = br.readLine()) != null) {
                String path = Paths.get(base, resource)
                        .toString();
                filenames.add(Node.parse(IoUtils.toUtf8String(Objects.requireNonNull(this.getClass()
                        .getResourceAsStream(path))), path));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return filenames;
    }

    private String serviceId(FromSourceLocation source) {
        return source.getSourceLocation().getFilename().split("/")[1];
    }

    public Stream<RulesTestSuite> testSuites() {
        List<Node> rulesetNodes = listDirectory("valid-rules/manifest.txt", "valid-rules");
        List<Node> testSuiteFiles = listDirectory("test-cases/manifest.txt", "test-cases");
        List<EndpointRuleset> rulesets = rulesetNodes.stream()
                .map(EndpointRuleset::fromNode)
                .collect(Collectors.toList());
        List<String> rulesetIds = rulesets.stream()
                .map(this::serviceId)
                .collect(Collectors.toList());
        if (rulesetIds.stream()
                    .distinct()
                    .count() != rulesets.size()) {
            throw new RuntimeException(String.format("Duplicate service ids discovered: %s", rulesets.stream()
                    .map(EndpointRuleset::getSourceLocation)
                    .sorted()
                    .collect(Collectors.toList())));
        }

        List<EndpointTestSuite> testSuites = testSuiteFiles.stream()
                .map(EndpointTestSuite::fromNode)
                .collect(Collectors.toList());
        testSuites.stream()
                .filter(testSuite -> !rulesetIds.contains(serviceId(testSuite)))
                .forEach(bad -> {
                    throw new RuntimeException("did not find service for " + serviceId(bad));
                });
        return rulesets.stream()
                .map(ruleset -> {
                    List<EndpointTestSuite> matchingTestSuites = testSuites.stream()
                            .filter(test -> serviceId(test).equals(serviceId(ruleset)))
                            .collect(Collectors.toList());
                    EndpointTestSuite suite;
                    if (matchingTestSuites.size() > 1) {
                        throw new RuntimeException("found duplicate test suites...");
                    } else if (matchingTestSuites.size() == 1) {
                        suite = matchingTestSuites.get(0);
                    } else {
                        suite = EndpointTestSuite.builder().build();
                    }
                    ruleset.typecheck();
                    return new RulesTestSuite(ruleset, suite);
                });
    }

    public RulesTestSuite getTestSuite(String name) {
        return new RulesTestSuite(rulesetFromPath(name), testSuiteFromPath(name));
    }

    private EndpointRuleset rulesetFromPath(String name) {
        return EndpointRuleset.fromNode(Node.parse(Objects.requireNonNull(this.getClass()
                .getResourceAsStream(String.format("valid-rules/%s", name))), name));
    }

    private EndpointTestSuite testSuiteFromPath(String name) {
        return EndpointTestSuite.fromNode(Node.parse(Objects.requireNonNull(this.getClass()
                .getResourceAsStream(String.format("test-cases/%s", name))), name));
    }

    public static final class RulesTestcase {
        private final EndpointRuleset ruleset;
        private final EndpointTest testcase;

        public RulesTestcase(EndpointRuleset ruleset, EndpointTest testcase) {
            this.ruleset = ruleset;
            this.testcase = testcase;
        }

        public EndpointRuleset ruleset() {
            return ruleset;
        }

        public EndpointTest testcase() {
            return testcase;
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
            RulesTestcase that = (RulesTestcase) obj;
            return Objects.equals(this.ruleset, that.ruleset)
                   && Objects.equals(this.testcase, that.testcase);
        }

        @Override
        public String toString() {
            return String.format("%s (%s:%s)", testcase.getDocumentation().orElse(""), testcase
                    .getSourceLocation()
                    .getFilename(), testcase.getSourceLocation()
                    .getLine());
        }

    }

    public static final class RulesTestSuite {
        private final EndpointRuleset ruleset;
        private final EndpointTestSuite testSuite;

        public RulesTestSuite(EndpointRuleset ruleset, EndpointTestSuite testSuite) {
            this.ruleset = ruleset;
            this.testSuite = testSuite;
        }

        public EndpointRuleset ruleset() {
            return ruleset;
        }

        public EndpointTestSuite testSuite() {
            return testSuite;
        }

        @Override
        public int hashCode() {
            return Objects.hash(ruleset, testSuite);
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
            return Objects.equals(this.ruleset, that.ruleset)
                   && Objects.equals(this.testSuite, that.testSuite);
        }

        @Override
        public String toString() {
            return ruleset.getSourceLocation()
                    .toString();
        }

    }


}
