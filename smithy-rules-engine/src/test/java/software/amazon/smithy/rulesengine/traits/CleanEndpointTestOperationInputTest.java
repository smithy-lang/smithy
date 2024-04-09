package software.amazon.smithy.rulesengine.traits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.utils.ListUtils;

public class CleanEndpointTestOperationInputTest {
    private static final ShapeId SERVICE_ID = ShapeId.from("smithy.example#ExampleService");
    private static final ShapeId GET_THING = ShapeId.from("smithy.example#GetThing");
    private static final ShapeId PING = ShapeId.from("smithy.example#Ping");
    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .discoverModels(ContextIndexTest.class.getClassLoader())
                .addImport(ContextIndexTest.class.getResource("traits-test-model.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void retainsTestsIfOperationRemains() {
        Model transformed = ModelTransformer.create().filterShapes(model, shape -> !shape.getId().equals(PING));

        assertFalse(transformed.getShape(PING).isPresent());
        assertTrue(transformed.getShape(SERVICE_ID).isPresent());

        ServiceShape mainService = model.expectShape(SERVICE_ID, ServiceShape.class);
        assertTrue(mainService.hasTrait(EndpointTestsTrait.class));
        ServiceShape transformedService = transformed.expectShape(SERVICE_ID, ServiceShape.class);
        assertTrue(transformedService.hasTrait(EndpointTestsTrait.class));

        Node.assertEquals(transformedService.expectTrait(EndpointTestsTrait.class).toNode(),
                mainService.expectTrait(EndpointTestsTrait.class).toNode());
    }

    @Test
    public void removesTestsIfOperationRemoved() {
        Model transformed = ModelTransformer.create().filterShapes(model, shape -> !shape.getId().equals(GET_THING));

        assertFalse(transformed.getShape(GET_THING).isPresent());
        assertTrue(transformed.getShape(SERVICE_ID).isPresent());

        ServiceShape transformedService = transformed.expectShape(SERVICE_ID, ServiceShape.class);
        assertTrue(transformedService.hasTrait(EndpointTestsTrait.class));

        EndpointTestsTrait trait = transformedService.expectTrait(EndpointTestsTrait.class);
        assertEquals(1, trait.getTestCases().size());
        assertTrue(trait.getTestCases().get(0).getOperationInputs().isEmpty());
    }

    @Test
    public void retainsTraitIfAllTestsRemoved() {
        ServiceShape serviceShape = model.expectShape(SERVICE_ID, ServiceShape.class);
        EndpointTestsTrait replacementTrait = serviceShape.expectTrait(EndpointTestsTrait.class);

        // Hack out the test case without operation input.
        ModelTransformer modelTransformer = ModelTransformer.create();
        replacementTrait = replacementTrait.toBuilder().removeTestCase(replacementTrait.getTestCases().get(0)).build();
        Model transformed = modelTransformer.replaceShapes(model, ListUtils.of(
                serviceShape.toBuilder().addTrait(replacementTrait).build()));

        // Then do the filtering.
        transformed = modelTransformer.filterShapes(transformed, shape -> !shape.getId().equals(GET_THING));

        assertFalse(transformed.getShape(GET_THING).isPresent());
        assertTrue(transformed.getShape(SERVICE_ID).isPresent());

        ServiceShape transformedService = transformed.expectShape(SERVICE_ID, ServiceShape.class);
        assertTrue(transformedService.hasTrait(EndpointTestsTrait.class));

        EndpointTestsTrait trait = transformedService.expectTrait(EndpointTestsTrait.class);
        assertEquals(0, trait.getTestCases().size());
    }
}
