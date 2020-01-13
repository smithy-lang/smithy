package software.amazon.smithy.aws.protocoltests;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;

/**
 * TODO: fix gradle plugin and remove this code.
 */
public class ModelTest {
    @Test
    public void loadsModel() {
        Model.assembler()
                .discoverModels()
                .assemble()
                .unwrap();
    }
}
