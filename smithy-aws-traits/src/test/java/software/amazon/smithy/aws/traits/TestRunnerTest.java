package software.amazon.smithy.aws.traits;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.validation.testrunner.SmithyTestSuite;

public class TestRunnerTest {
    @Test
    public void testRunner() {
        ModelAssembler assembler = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader());

        System.out.println(SmithyTestSuite.runner()
                .setModelAssemblerFactory(assembler::copy)
                .addTestCasesFromUrl(TestRunnerTest.class.getResource("errorfiles"))
                .run());
    }
}
