package software.amazon.smithy.rulesengine.language;

import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.validation.testrunner.SmithyTestCase;
import software.amazon.smithy.model.validation.testrunner.SmithyTestSuite;
import software.amazon.smithy.utils.IoUtils;

public class TestRunnerTest {
    public static Stream<?> source() {
        return SmithyTestSuite.defaultParameterizedTestSource(TestRunnerTest.class);
    }

    public static EndpointRuleSet getMinimalEndpointRuleSet() {
        return EndpointRuleSet.fromNode(Node.parse(IoUtils.readUtf8Resource(
                TestRunnerTest.class, "minimal-ruleset.json")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("source")
    public void testRunner(String filename, Callable<SmithyTestCase.Result> callable) throws Exception {
        callable.call();
    }
}
