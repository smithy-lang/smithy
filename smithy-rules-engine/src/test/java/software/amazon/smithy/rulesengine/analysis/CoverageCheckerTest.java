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

package software.amazon.smithy.rulesengine.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.TestDiscovery;
import software.amazon.smithy.rulesengine.language.eval.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.utils.MapUtils;

class CoverageCheckerTest {
    @Test
    void checkCoverage() throws IOException {
        TestDiscovery.RulesTestSuite suite = TestDiscovery.getTestSuite("local-region-override.json");
        CoverageChecker checker = new CoverageChecker(suite.getRuleSet());
        assertEquals((int) checker.checkCoverage().count(), 2);

        checker.evaluateInput(MapUtils.of(Identifier.of("Region"), Value.stringValue("local")));
        assertEquals(1, (int) checker.checkCoverage().count());
        checker.evaluateInput(MapUtils.of(Identifier.of("Region"), Value.stringValue("notlocal")));
        assertEquals(0, (int) checker.checkCoverage().count());
    }

    @Test
    void checkCoverageOfTestSuite() throws IOException {
        TestDiscovery.RulesTestSuite testSuites = TestDiscovery.getTestSuite("local-region-override.json");
        CoverageChecker checker = new CoverageChecker(testSuites.getRuleSet());
        assertEquals(2, checker.checkCoverage().count());
        testSuites.getTestSuite().getTestCases().forEach(checker::evaluateTestCase);
        assertEquals(0, checker.checkCoverage().count());
    }

}
