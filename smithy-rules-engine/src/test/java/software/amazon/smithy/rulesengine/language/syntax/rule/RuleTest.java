package software.amazon.smithy.rulesengine.language.syntax.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.evaluation.RuleEvaluator;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionNode;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.utils.MapUtils;

public class RuleTest {
    @Test
    public void validateAndErrorsTest() {
        Parameter p1 = Parameter.builder().name("param1").type(ParameterType.STRING).required(true).build();
        Parameter p2 = Parameter.builder().name("param2").type(ParameterType.STRING).required(true).build();
        Parameter p3 = Parameter.builder().name("param3").type(ParameterType.STRING).required(true).build();
        StringEquals equalsA = new StringEquals.Definition().createFunction(FunctionNode.ofExpressions(StringEquals.ID,
                p1.toExpression(), Expression.of("a")));
        StringEquals equalsB = new StringEquals.Definition().createFunction(FunctionNode.ofExpressions(StringEquals.ID,
                p2.toExpression(), Expression.of("b")));
        StringEquals equalsC = new StringEquals.Definition().createFunction(FunctionNode.ofExpressions(StringEquals.ID,
                p3.toExpression(), Expression.of("c")));
        Rule rule = Rule.builder()
                .validateOrElse("param1 value is not a", condition(equalsA))
                .errorOrElse("param2 is b", condition(equalsB))
                .validateOrElse("param3 value is not c", condition(equalsC))
                .errorOrElse("param2 is b", condition(equalsB))
                .treeRule(Rule.builder().error("rule matched: p3"));
        Parameters parameters = Parameters.builder().addParameter(p1).addParameter(p2).addParameter(p3).build();
        EndpointRuleSet ruleset = EndpointRuleSet.builder().version("1.1").parameters(parameters).addRule(rule).build();
        ruleset.typeCheck(new Scope<>());
        assertEquals(RuleEvaluator.evaluate(ruleset, MapUtils.of(
                        Identifier.of("param1"), Value.stringValue("a"),
                        Identifier.of("param2"), Value.stringValue("c"),
                        Identifier.of("param3"), Value.stringValue("c"))),
                Value.stringValue("rule matched: p3"));
        assertEquals(RuleEvaluator.evaluate(ruleset, MapUtils.of(
                        Identifier.of("param1"), Value.stringValue("b"),
                        Identifier.of("param2"), Value.stringValue("c"),
                        Identifier.of("param3"), Value.stringValue("c"))),
                Value.stringValue("param1 value is not a"));
        assertEquals(RuleEvaluator.evaluate(ruleset, MapUtils.of(
                        Identifier.of("param1"), Value.stringValue("a"),
                        Identifier.of("param2"), Value.stringValue("b"),
                        Identifier.of("param3"), Value.stringValue("c"))),
                Value.stringValue("param2 is b"));
        assertEquals(RuleEvaluator.evaluate(ruleset, MapUtils.of(
                        Identifier.of("param1"), Value.stringValue("a"),
                        Identifier.of("param2"), Value.stringValue("c"),
                        Identifier.of("param3"), Value.stringValue("d"))),
                Value.stringValue("param3 value is not c"));
    }

    private Condition condition(LibraryFunction libraryFunction) {
        return Condition.builder().fn(libraryFunction).build();
    }
}
