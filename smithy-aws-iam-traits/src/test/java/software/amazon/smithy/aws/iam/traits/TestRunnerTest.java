package software.amazon.smithy.aws.iam.traits;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.validation.testrunner.SmithyTestCase;
import software.amazon.smithy.model.validation.testrunner.SmithyTestSuite;

import java.util.concurrent.Callable;
import java.util.stream.Stream;

public class TestRunnerTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("source")
    public void testRunner(String filename, Callable<SmithyTestCase.Result> callable) throws Exception {
        callable.call();
    }

    public static Stream<?> source() {
        return SmithyTestSuite.defaultParameterizedTestSource(TestRunnerTest.class);
    }
}
