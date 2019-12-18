package software.amazon.smithy.aws.protocoltests;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;

/**
 * This entire repo doesn't use the Gradle plugin, but rather just manually
 * defines Smithy models and their manifest files. The drawback of this
 * approach is that it means that models aren't validated when they're
 * placed in the built JAR. This test just loads the models in the package
 * to make sure they're valid.
 */
public class ModelCheckTest {
    @Test
    public void modelsAreValid() {
        Model.assembler().discoverModels().assemble().unwrap();
    }
}
