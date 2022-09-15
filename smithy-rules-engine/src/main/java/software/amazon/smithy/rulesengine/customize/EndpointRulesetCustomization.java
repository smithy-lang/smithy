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

package software.amazon.smithy.rulesengine.customize;

import software.amazon.smithy.rulesengine.language.EndpointRuleset;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;

/**
 * Customize generated EndpointRulesets and EndpointTests.
 * <p>
 * To customize the Endpoint rules for a service, implement this interface
 * and update the `rule-set-synthesis-configuration.json` to add the
 * fully qualified classname of your implementation as `customization` on the service definition.
 */
public interface EndpointRulesetCustomization {

    /**
     * Customize the base, generated EndpointRuleset (the ruleset generated from endpoints.json).
     * <p>
     * Most Customizations will need to add additional rules and parameters.  The generated rules
     * contains a single, top level TreeRule which you can use to insert additional rules into:
     * <pre>{@code
     *     public EndpointRuleset customizeRuleset(EndpointRuleset ruleset) {
     *         assert ruleset.getRules().size() == 1;
     *         TreeRule rootRule = (TreeRule) ruleset.getRules().get(0);
     *
     *         List<Rule> subRules = new ArrayList<>();
     *         // Add our new custom rules first
     *         subRules.add(myNewCustomRules());
     *         // ensure the base/generated rule are added
     *         subRules.addAll(ruleset.getRules());
     *
     *         // create a new root rule from the old rule (with our new subRules)
     *         Rule newRootRule = TreeRule.builder()
     *                 .conditions(rootRule.getConditions())
     *                 .treeRule(subRules);
     *
     *         // add a new custom parameter
     *         Parameters newParameters = ruleset.getParameters().toBuilder()
     *                 .addParameter(myNewParameter()).build();
     *
     *         // use the Rulset builder with our modified parameters and rules
     *         return EndpointRuleset.builder()
     *                 .parameters(newParameters)
     *                 .addRule(newRootRule)
     *                 .build();
     *     }
     * }</pre>
     *
     * @param ruleset the base generated ruleset
     * @return ruleset customized for the service
     */
    default EndpointRuleset customizeRuleset(EndpointRuleset ruleset) {
        return ruleset;
    }

    /**
     * Customize the base, generated Test Suite (generated from endpoints.json).
     * <p>
     * Most customizations will simply need to add additional test cases:
     * <pre>{@code
     *     public EndpointTestsTrait customizeTestSuite(EndpointTestsTrait testSuite) {
     *         List<EndpointTestCase> testCases = new ArrayList<>();
     *         testCases.addAll(testSuite.getTestCases());
     *         testCases.add(myCustomTestCase());
     *         return ((EndpointTestsTrait.Builder)testSuite.toBuilder()).testCases(testCases).build();
     *     }
     * }</pre>
     *
     * @param testSuite the base generated test suite
     * @return test suite customized for the service
     */
    default EndpointTestsTrait customizeTestSuite(EndpointTestsTrait testSuite) {
        return testSuite;
    }

    /**
     * If a service has private/internal/development only features, return true.
     * The {@link #developmentRuleset(EndpointRuleset)} developmentRuleset} and
     * {@link #developmentTestSuite(EndpointTestsTrait)} methods will be called
     * and the resulting rules/tests will be saved in a separate output folder.
     *
     * @return true if the service has development (internal only) endpoint rules
     */
    default boolean hasDevelopmentFeatures() {
        return false;
    }

    /**
     * Called when hasDevelopmentFeatures is true to produce rules with internal/development
     * features enabled.
     * See {@link #customizeRuleset(EndpointRuleset)}
     *
     * @param ruleset the base generated ruleset
     * @return ruleset customized for the service including development (internal) features
     */
    default EndpointRuleset developmentRuleset(EndpointRuleset ruleset) {
        return ruleset;
    }

    /**
     * Called when hasDevelopmentFeatures is true to produce test cases with internal/development
     * features enabled.
     * See {@link #customizeTestSuite(EndpointTestsTrait)}
     *
     * @param testSuite the base generated test suite
     * @return test suite customized for the service including development (internal) features
     */
    default EndpointTestsTrait developmentTestSuite(EndpointTestsTrait testSuite) {
        return testSuite;
    }
}
