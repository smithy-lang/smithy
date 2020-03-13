package software.amazon.smithy.protocoltests.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class TraitTest {
    @Test
    public void simpleRequestTest() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("say-hello.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        HttpRequestTestCase testCase = model.expectShape(ShapeId.from("smithy.example#SayHello"))
                .expectTrait(HttpRequestTestsTrait.class)
                .getTestCases()
                .get(0);

        assertThat(testCase.toBuilder().build(), equalTo(testCase));
        assertThat(HttpRequestTestCase.fromNode(testCase.toNode()), equalTo(testCase));
    }

    @Test
    public void simpleResponseTest() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("say-goodbye.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        HttpResponseTestCase testCase = model.expectShape(ShapeId.from("smithy.example#SayGoodbye"))
                .expectTrait(HttpResponseTestsTrait.class)
                .getTestCases()
                .get(0);

        assertThat(testCase.toBuilder().build(), equalTo(testCase));
        assertThat(HttpResponseTestCase.fromNode(testCase.toNode()), equalTo(testCase));
    }
}
