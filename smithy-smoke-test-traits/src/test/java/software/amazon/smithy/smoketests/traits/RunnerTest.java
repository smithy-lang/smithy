package software.amazon.smithy.smoketests.traits;

import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.validation.testrunner.SmithyTestCase;
import software.amazon.smithy.model.validation.testrunner.SmithyTestSuite;


public class RunnerTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("source")
    public void testRunner(String filename, Callable<SmithyTestCase.Result> callable) throws Exception {
        callable.call();
    }

    public static Stream<?> source() {
        return SmithyTestSuite.defaultParameterizedTestSource(RunnerTest.class);
    }
}
