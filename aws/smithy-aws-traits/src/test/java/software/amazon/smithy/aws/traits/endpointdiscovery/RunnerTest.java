package software.amazon.smithy.aws.traits.endpointdiscovery;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.validation.testrunner.SmithyTestSuite;

public class RunnerTest {
    @Test
    public void testRunner() {
        ModelAssembler assembler = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader());

        System.out.println(SmithyTestSuite.runner()
                .setModelAssemblerFactory(assembler::copy)
                .addTestCasesFromUrl(getClass().getResource("errorfiles"))
                .run());
    }
}
