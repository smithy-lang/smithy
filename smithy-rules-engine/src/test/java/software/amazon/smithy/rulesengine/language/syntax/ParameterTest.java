package software.amazon.smithy.rulesengine.language.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;

public class ParameterTest {
    @Test
    void parameterToBuilderRoundTrips() {
        Parameter p = Parameter.builder()
                .name("test")
                .builtIn("Test::BuiltIn")
                .required(true)
                .defaultValue(Value.bool(true))
                .type(ParameterType.BOOLEAN)
                .deprecated(new Parameter.Deprecated("message", "I wrote this test"))
                .build();
        assertEquals(p, p.toBuilder().build());
    }
}
