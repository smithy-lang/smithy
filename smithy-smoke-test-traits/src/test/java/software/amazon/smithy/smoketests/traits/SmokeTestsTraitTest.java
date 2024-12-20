/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.smoketests.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class SmokeTestsTraitTest {
    @Test
    public void successTest() {
        SmokeTestCase testCase = getSmokeTestCase("success-test.smithy");

        assertThat(testCase.toBuilder().build(), equalTo(testCase));
        assertThat(SmokeTestCase.fromNode(testCase.toNode()), equalTo(testCase));

        Expectation expectation = testCase.getExpectation();
        assertThat(Expectation.fromNode(expectation.toNode()), equalTo(expectation));
        assertTrue(expectation.isSuccess());
        assertFalse(expectation.isFailure());
        assertThat(expectation.getFailure(), equalTo(Optional.empty()));
    }

    @Test
    public void anyFailureTest() {
        SmokeTestCase testCase = getSmokeTestCase("any-failure-test.smithy");

        assertThat(testCase.toBuilder().build(), equalTo(testCase));
        assertThat(SmokeTestCase.fromNode(testCase.toNode()), equalTo(testCase));

        Expectation expectation = testCase.getExpectation();
        assertThat(Expectation.fromNode(expectation.toNode()), equalTo(expectation));
        assertTrue(expectation.isFailure());
        assertFalse(expectation.isSuccess());
        assertTrue(expectation.getFailure().isPresent());

        FailureExpectation failureExpectation = expectation.getFailure().get();
        assertThat(FailureExpectation.fromNode(failureExpectation.toNode()), equalTo(failureExpectation));
        assertThat(failureExpectation.getErrorId(), equalTo(Optional.empty()));
    }

    @Test
    public void specificFailureTest() {
        SmokeTestCase testCase = getSmokeTestCase("specific-failure-test.smithy");

        assertThat(testCase.toBuilder().build(), equalTo(testCase));
        assertThat(SmokeTestCase.fromNode(testCase.toNode()), equalTo(testCase));

        Expectation expectation = testCase.getExpectation();
        assertThat(Expectation.fromNode(expectation.toNode()), equalTo(expectation));
        assertTrue(expectation.isFailure());
        assertFalse(expectation.isSuccess());
        assertTrue(expectation.getFailure().isPresent());

        FailureExpectation failureExpectation = expectation.getFailure().get();
        assertThat(FailureExpectation.fromNode(failureExpectation.toNode()), equalTo(failureExpectation));
        assertTrue(failureExpectation.getErrorId().isPresent());
        assertThat(failureExpectation.getErrorId().get(), equalTo(ShapeId.from("smithy.example#SayHelloError")));
    }

    @Test
    public void withParams() {
        SmokeTestCase testCase = getSmokeTestCase("test-with-params.smithy");

        assertThat(testCase.toBuilder().build(), equalTo(testCase));
        assertThat(SmokeTestCase.fromNode(testCase.toNode()), equalTo(testCase));
        assertTrue(testCase.getParams().isPresent());
        assertThat(testCase.getParams().get(), notNullValue());
    }

    @Test
    public void withVendorParams() {
        SmokeTestCase testCase = getSmokeTestCase("test-with-vendor-params.smithy");

        assertThat(testCase.toBuilder().build(), equalTo(testCase));
        assertThat(SmokeTestCase.fromNode(testCase.toNode()), equalTo(testCase));
        assertTrue(testCase.getVendorParams().isPresent());
        assertThat(testCase.getVendorParams().get(), notNullValue());
        assertThat(testCase.getVendorParamsShape(), equalTo(Optional.empty()));
    }

    @Test
    public void withVendorParamsShape() {
        SmokeTestCase testCase = getSmokeTestCase("test-with-vendor-params-shape.smithy");

        assertThat(testCase.toBuilder().build(), equalTo(testCase));
        assertThat(SmokeTestCase.fromNode(testCase.toNode()), equalTo(testCase));
        assertTrue(testCase.getVendorParams().isPresent());
        assertThat(testCase.getVendorParams(), notNullValue());
        assertTrue(testCase.getVendorParamsShape().isPresent());
        assertThat(testCase.getVendorParamsShape().get(), equalTo(ShapeId.from("smithy.example#VendorParams")));
    }

    @Test
    public void withTags() {
        SmokeTestCase testCase = getSmokeTestCase("test-with-tags.smithy");

        assertThat(testCase.toBuilder().build(), equalTo(testCase));
        assertThat(SmokeTestCase.fromNode(testCase.toNode()), equalTo(testCase));

        assertThat(testCase.getTags(), contains("foo", "bar"));
        assertThat(testCase.toBuilder().build().getTags(), contains("foo", "bar"));
    }

    private static SmokeTestCase getSmokeTestCase(String filename) {
        Model model = Model.assembler()
                .addImport(SmokeTestsTraitTest.class.getResource(filename))
                .discoverModels()
                .assemble()
                .unwrap();
        return model.expectShape(ShapeId.from("smithy.example#SayHello"))
                .expectTrait(SmokeTestsTrait.class)
                .getTestCases()
                .get(0);
    }
}
