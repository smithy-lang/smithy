package software.amazon.smithy.rulesengine.traits;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.utils.MapUtils;

public final class ClientContextParamsTraitTest {

    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("traits-test-model.smithy"))
                .assemble()
                .unwrap();
        ServiceShape service = result
                .expectShape(ShapeId.from("smithy.example#ExampleService"))
                .asServiceShape().get();

        ClientContextParamsTrait trait = service.getTrait(ClientContextParamsTrait.class).get();

        assertEquals(trait.getParameters(), MapUtils.of(
                "stringFoo", ClientContextParamDefinition.builder()
                        .type(ShapeType.STRING)
                        .documentation("a client string parameter")
                        .build(),
                "boolFoo", ClientContextParamDefinition.builder()
                        .type(ShapeType.BOOLEAN)
                        .documentation("a client boolean parameter")
                        .build()
        ));
    }
}
