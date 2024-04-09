package software.amazon.smithy.rulesengine.traits;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

public final class EndpointTestsTest {
    @Test
    public void loadsFromTheModel() {
        Model model = Model.assembler()
                .discoverModels(EndpointTestsTest.class.getClassLoader())
                .addImport(EndpointTestsTest.class.getResource("traits-test-model.smithy"))
                .assemble()
                .unwrap();
        ServiceShape serviceShape = model.expectShape(ShapeId.from("smithy.example#ExampleService"),
                ServiceShape.class);
        EndpointTestsTrait ruleSetTrait = serviceShape.getTrait(EndpointTestsTrait.class).get();
        List<EndpointTestCase> testCases = ruleSetTrait.getTestCases();

        assertThat(2, equalTo(testCases.size()));
        assertThat(EndpointTestCase.builder()
                .documentation("a documentation string")
                .params(ObjectNode.builder()
                        .withMember("stringFoo", "a b")
                        .withMember("boolFoo", false)
                        .build())
                .expect(EndpointTestExpectation.builder()
                        .error("endpoint error")
                        .build())
                .build(), equalTo(testCases.get(0)));
        assertThat(EndpointTestCase.builder()
                .params(ObjectNode.builder()
                        .withMember("stringFoo", "c d")
                        .withMember("boolFoo", true)
                        .build())
                .operationInputs(ListUtils.of(
                        EndpointTestOperationInput.builder()
                                .operationName("GetThing")
                                .clientParams(ObjectNode.builder()
                                        .withMember("stringFoo", "client value")
                                        .build())
                                .operationParams(ObjectNode.builder()
                                        .withMember("buzz", "a buzz value")
                                        .withMember("fizz", "a required value")
                                        .build())
                                .builtInParams(ObjectNode.builder()
                                        .withMember("SDK::Endpoint", "https://custom.example.com")
                                        .build())
                                .build()
                ))
                .expect(EndpointTestExpectation.builder()
                        .endpoint(ExpectedEndpoint.builder()
                                .url("https://example.com")
                                .properties(MapUtils.of())
                                .headers(MapUtils.of(
                                        "single", ListUtils.of("foo"),
                                        "multi", ListUtils.of("foo", "bar", "baz")
                                ))
                                .build())
                        .build())
                .build(), equalTo(testCases.get(1)));
    }

    @Test
    public void roundTrips() {
        Node expectedNode = Node.parse(
                "{\"version\":\"1.0\",\"testCases\":[{\"documentation\":\"foo bar\",\"params\":{\"foo\":\"bar\""
                + ",\"bar\":\"foo\"},\"operationInputs\":[{\"operationName\":\"GetThing\",\"clientParams\":"
                + "{\"stringFoo\":\"client value\"},\"operationParams\":{\"buzz\":\"a buzz value\"},\"builtInParams\":"
                + "{\"SDK::Endpoint\":\"https://custom.example.com\"}}],\"expect\":{\"endpoint\":{\"url\":"
                + "\"example.com\",\"headers\":{\"single\":[\"one\"],\"multi\":[\"one\",\"two\"]},\"properties\":"
                + "{\"foo\":{\"bar\":\"thing\",\"baz\":false}}}}},{\"documentation\":\"bar foo\",\"params\":{\"foo\":"
                + "\"foo\"},\"expect\":{\"error\":\"error string\"}}]}");

        TraitFactory traitFactory = TraitFactory.createServiceFactory();
        EndpointTestsTrait expectedTrait = (EndpointTestsTrait) traitFactory.createTrait(EndpointTestsTrait.ID,
                ShapeId.from("ns.example#Foo"), expectedNode).get();
        EndpointTestsTrait actualTrait = expectedTrait.toBuilder().build();

        Node.assertEquals(actualTrait.toNode(), expectedNode);
        assertThat(expectedTrait, equalTo(actualTrait));
    }
}
