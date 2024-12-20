/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class TraitTest {

    private static Model appliesToModel;

    @BeforeAll
    public static void before() {
        appliesToModel = Model.assembler()
                .discoverModels()
                .addImport(TraitTest.class.getResource("test-with-appliesto.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void simpleRequestTest() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("say-hello.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        HttpRequestTestCase testCase = model.expectShape(ShapeId.from("smithy.example#SayHello"))
                .getTrait(HttpRequestTestsTrait.class)
                .get()
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
                .getTrait(HttpResponseTestsTrait.class)
                .get()
                .getTestCases()
                .get(0);

        assertThat(testCase.toBuilder().build(), equalTo(testCase));
        assertThat(HttpResponseTestCase.fromNode(testCase.toNode()), equalTo(testCase));
    }

    @Test
    public void messageHasTags() {
        Model model = Model.assembler()
                .discoverModels()
                .addImport(getClass().getResource("test-with-tags.smithy"))
                .assemble()
                .unwrap();
        HttpRequestTestCase request = model.expectShape(ShapeId.from("smithy.example#SaySomething"))
                .getTrait(HttpRequestTestsTrait.class)
                .get()
                .getTestCases()
                .get(0);
        HttpResponseTestCase response = model.expectShape(ShapeId.from("smithy.example#SaySomething"))
                .getTrait(HttpResponseTestsTrait.class)
                .get()
                .getTestCases()
                .get(0);

        assertThat(request.getTags(), contains("foo", "bar"));
        assertThat(request.toBuilder().build().getTags(), contains("foo", "bar"));
        assertThat(response.getTags(), contains("baz", "qux"));
        assertThat(response.toBuilder().build().getTags(), contains("baz", "qux"));

        assertThat(request.toBuilder().build(), equalTo(request));
        assertThat(HttpRequestTestCase.fromNode(request.toNode()), equalTo(request));

        assertThat(response.toBuilder().build(), equalTo(response));
        assertThat(HttpResponseTestCase.fromNode(response.toNode()), equalTo(response));
    }

    @Test
    public void messageHasAppliesTo() {
        HttpRequestTestsTrait requestTrait = appliesToModel.expectShape(ShapeId.from("smithy.example#SaySomething"))
                .expectTrait(HttpRequestTestsTrait.class);
        HttpRequestTestCase request = requestTrait.getTestCases().get(1);

        assertThat(request.getAppliesTo().isPresent(), is(true));
        assertThat(request.getAppliesTo().get(), equalTo(AppliesTo.CLIENT));
        assertThat(request.toBuilder().build(), equalTo(request));
        assertThat(HttpRequestTestCase.fromNode(request.toNode()), equalTo(request));

        HttpResponseTestsTrait responseTrait = appliesToModel.expectShape(ShapeId.from("smithy.example#SaySomething"))
                .expectTrait(HttpResponseTestsTrait.class);
        HttpResponseTestCase response = responseTrait.getTestCases().get(1);

        assertThat(response.getAppliesTo().isPresent(), is(true));
        assertThat(response.getAppliesTo().get(), equalTo(AppliesTo.CLIENT));
        assertThat(response.toBuilder().build(), equalTo(response));
        assertThat(HttpResponseTestCase.fromNode(response.toNode()), equalTo(response));
    }

    @Test
    public void canFilterTestsByAppliesTo() {
        HttpRequestTestsTrait requestTrait = appliesToModel.expectShape(ShapeId.from("smithy.example#SaySomething"))
                .expectTrait(HttpRequestTestsTrait.class);
        HttpResponseTestsTrait responseTrait = appliesToModel.expectShape(ShapeId.from("smithy.example#SaySomething"))
                .expectTrait(HttpResponseTestsTrait.class);

        assertThat(getCaseIds(requestTrait.getTestCasesFor(AppliesTo.CLIENT)),
                containsInAnyOrder("say_hello_all", "say_hello_client"));
        assertThat(getCaseIds(requestTrait.getTestCasesFor(AppliesTo.SERVER)),
                containsInAnyOrder("say_hello_all", "say_hello_server"));

        assertThat(getCaseIds(responseTrait.getTestCasesFor(AppliesTo.CLIENT)),
                containsInAnyOrder("say_goodbye_all", "say_goodbye_client"));
        assertThat(getCaseIds(responseTrait.getTestCasesFor(AppliesTo.SERVER)),
                containsInAnyOrder("say_goodbye_all", "say_goodbye_server"));
    }

    private List<String> getCaseIds(List<? extends HttpMessageTestCase> cases) {
        return cases.stream()
                .map(HttpMessageTestCase::getId)
                .collect(Collectors.toList());
    }
}
