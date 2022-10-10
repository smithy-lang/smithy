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

package software.amazon.smithy.rulesengine.language.syntax.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.eval.RuleEvaluator;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.utils.MapUtils;

class RuleTest {
    @Test
    void validateAndErrorsTest() {
        Parameter p1 = Parameter.builder().name("param1").type(ParameterType.STRING).required(true).build();
        Parameter p2 = Parameter.builder().name("param2").type(ParameterType.STRING).required(true).build();
        Parameter p3 = Parameter.builder().name("param3").type(ParameterType.STRING).required(true).build();
        Rule rule = Rule.builder()
                .validateOrElse(p1.toExpression().equal("a"), "param1 value is not a")
                .errorOrElse("param2 is b", p2.toExpression().equal("b"))
                .validateOrElse(p3.toExpression().equal("c"), "param3 value is not c")
                .errorOrElse("param2 is b", p2.toExpression().equal("b"))
                .validateOrElse(p3.toExpression().parseArn().condition("p3Arn"), "p3 is not an arn")
                .treeRule(Rule.builder().error("rule matched: {p3Arn#region}"));
        System.out.println(rule);
        Parameters parameters = Parameters.builder().addParameter(p1).addParameter(p2).addParameter(p3).build();
        EndpointRuleSet ruleset = EndpointRuleSet.builder().version("1.1").parameters(parameters).addRule(rule).build();
        ruleset.typecheck();
        assertEquals(RuleEvaluator.evaluate(ruleset, MapUtils.of(
                        Identifier.of("param1"), Value.string("a"),
                        Identifier.of("param2"), Value.string("c"),
                        Identifier.of("param3"), Value.string("c"))),
                Value.string("p3 is not an arn"));
        assertEquals(RuleEvaluator.evaluate(ruleset, MapUtils.of(
                        Identifier.of("param1"), Value.string("a"),
                        Identifier.of("param2"), Value.string("b"),
                        Identifier.of("param3"), Value.string("c")))
                , Value.string("param2 is b"));
        assertEquals(RuleEvaluator.evaluate(ruleset, MapUtils.of(
                        Identifier.of("param1"), Value.string("a"),
                        Identifier.of("param2"), Value.string("c"),
                        Identifier.of("param3"), Value.string("d")))
                , Value.string("param3 value is not c"));
    }

}
