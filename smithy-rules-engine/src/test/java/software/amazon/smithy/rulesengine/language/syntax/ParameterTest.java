package software.amazon.smithy.rulesengine.language.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Deprecated;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;

public class ParameterTest {
    @Test
    void parameterToBuilderRoundTrips() {
        Parameter p = Parameter.builder()
                .name("test")
                .builtIn("Test::BuiltIn")
                .required(true)
                .defaultValue(Value.booleanValue(true))
                .type(ParameterType.BOOLEAN)
                .deprecated(new Deprecated("message", "I wrote this test"))
                .value(Node.from(true))
                .documentation("here are some docs")
                .build();
        assertEquals(p, p.toBuilder().build());
    }
}
