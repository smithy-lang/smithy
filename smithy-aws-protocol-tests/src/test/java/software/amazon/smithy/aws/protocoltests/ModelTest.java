package software.amazon.smithy.aws.protocoltests;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.ValidatedResult;

/**
 * TODO: fix gradle plugin and remove this code.
 */
public class ModelTest {
    @Test
    public void loadsModel() {
        ValidatedResult<Model> r = Model.assembler()
                .discoverModels()
                .assemble();
        System.out.println(r.getValidationEvents());
        r.unwrap();
    }
}
