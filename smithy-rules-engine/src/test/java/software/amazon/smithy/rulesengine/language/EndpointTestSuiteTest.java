package software.amazon.smithy.rulesengine.language;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.lang.Identifier;
import software.amazon.smithy.rulesengine.language.lang.parameters.Builtins;
import software.amazon.smithy.utils.MapUtils;

public class EndpointTestSuiteTest {
    @Test
    public void toNode_maximal() {
        Value.Endpoint endpoint = Value.Endpoint.builder()
                .url("myservice.aws")
                .addHeader("header1", "value1")
                .addProperty("property1", Value.str("property1Value"))
                .build();

        EndpointTest.Expectation endpointExpectation = new EndpointTest.Expectation.Endpoint(endpoint);
        EndpointTest.Expectation errorExpectation = new EndpointTest.Expectation.Error("Something went wrong!");

        Value.Record params = Value.record(MapUtils.of(
                Identifier.of("boolParam"), Value.bool(false)
        ));

        EndpointTest test1 = EndpointTest.builder()
                .documentation("test1")
                .expectation(endpointExpectation)
                .params(params)
                .operationInput(EndpointTest.OperationInput.builder()
                        .operationName(Identifier.of("PutObject"))
                        .operationParameter(Identifier.of("Bucket"), Value.str("bucket-name"))
                        .builtInParameter(Builtins.REGION.getBuiltIn().get(), Value.str("us-west-2")).build())
                .build();

        EndpointTest test2 = EndpointTest.builder()
                .documentation("test2")
                .expectation(errorExpectation)
                .params(params)
                .build();

        EndpointTestSuite testSuite = EndpointTestSuite.builder()
                .addTestCase(test1)
                .addTestCase(test2)
                .build();

        String expectedJson = "{\n" +
                              "    \"testCases\": [\n" +
                              "        {\n" +
                              "            \"documentation\": \"test1\",\n" +
                              "            \"params\": {\n" +
                              "                \"boolParam\": false\n" +
                              "            },\n" +
                              "            \"expect\": { \"endpoint\": {\n" +
                              "                \"url\": \"myservice.aws\",\n" +
                              "                \"properties\": {\n" +
                              "                    \"property1\": \"property1Value\"\n" +
                              "                },\n" +
                              "                \"headers\": {\n" +
                              "                    \"header1\": [\n" +
                              "                        \"value1\"\n" +
                              "                    ]\n" +
                              "                }\n" +
                              "            }},\n" +
                              "            \"operationInputs\": [\n" +
                              "                {\n" +
                              "                    \"operationName\": \"PutObject\",\n" +
                              "                    \"clientParams\": {},\n" +
                              "                    \"operationParams\": {\n" +
                              "                        \"Bucket\": \"bucket-name\"\n" +
                              "                    },\n" +
                              "                    \"builtinParams\": {\n" +
                              "                        \"AWS::Region\": \"us-west-2\"\n" +
                              "                    }\n" +
                              "                }\n" +
                              "            ]\n" +
                              "        },\n" +
                              "        {\n" +
                              "            \"documentation\": \"test2\",\n" +
                              "            \"params\": {\n" +
                              "                \"boolParam\": false\n" +
                              "            },\n" +
                              "            \"expect\": {\n" +
                              "                \"error\": \"Something went wrong!\"\n" +
                              "            },\n" +
                              "            \"operationInputs\": []\n" +
                              "        }\n" +
                              "    ]\n" +
                              "}";
        assertThat(Collections.emptyList(), equalTo(Node.diff(testSuite.toNode(), Node.parse(expectedJson))));
    }

    @Test
    public void fromNode_maximal() {
        Value.Endpoint endpoint = Value.Endpoint.builder()
                .url("myservice.aws")
                .addHeader("header1", "value1")
                .addProperty("property1", Value.str("property1Value"))
                .build();

        EndpointTest.Expectation endpointExpectation = new EndpointTest.Expectation.Endpoint(endpoint);
        EndpointTest.Expectation errorExpectation = new EndpointTest.Expectation.Error("Something went wrong!");

        Value.Record params = Value.record(MapUtils.of(
                Identifier.of("boolParam"), Value.bool(false)
        ));

        EndpointTest test1 = EndpointTest.builder()
                .documentation("test1")
                .expectation(endpointExpectation)
                .params(params)
                .operationInput(EndpointTest.OperationInput.builder()
                        .operationName(Identifier.of("PutObject"))
                        .operationParameter(Identifier.of("Bucket"), Value.str("bucket-name"))
                        .builtInParameter(Builtins.REGION.getBuiltIn().get(), Value.str("us-west-2")).build())
                .build();

        EndpointTest test2 = EndpointTest.builder()
                .documentation("test2")
                .expectation(errorExpectation)
                .params(params)
                .build();

        EndpointTestSuite testSuite = EndpointTestSuite.builder()
                .addTestCase(test1)
                .addTestCase(test2)
                .build();

        String expectedJson = "{\n" +
                              "    \"testCases\": [\n" +
                              "        {\n" +
                              "            \"documentation\": \"test1\",\n" +
                              "            \"params\": {\n" +
                              "                \"boolParam\": false\n" +
                              "            },\n" +
                              "            \"expect\": { \"endpoint\": {\n" +
                              "                \"url\": \"myservice.aws\",\n" +
                              "                \"properties\": {\n" +
                              "                    \"property1\": \"property1Value\"\n" +
                              "                },\n" +
                              "                \"headers\": {\n" +
                              "                    \"header1\": [\n" +
                              "                        \"value1\"\n" +
                              "                    ]\n" +
                              "                }\n" +
                              "            }},\n" +
                              "            \"operationInputs\": [\n" +
                              "                {\n" +
                              "                    \"operationName\": \"PutObject\",\n" +
                              "                    \"clientParams\": {},\n" +
                              "                    \"operationParams\": {\n" +
                              "                        \"Bucket\": \"bucket-name\"\n" +
                              "                    },\n" +
                              "                    \"builtinParams\": {\n" +
                              "                        \"AWS::Region\": \"us-west-2\"\n" +
                              "                    }\n" +
                              "                }\n" +
                              "            ]\n" +
                              "        },\n" +
                              "        {\n" +
                              "            \"documentation\": \"test2\",\n" +
                              "            \"params\": {\n" +
                              "                \"boolParam\": false\n" +
                              "            },\n" +
                              "            \"expect\": {\n" +
                              "                \"error\": \"Something went wrong!\"\n" +
                              "            }\n" +
                              "        }\n" +
                              "    ]\n" +
                              "}\n";

        assertThat(EndpointTestSuite.fromNode(Node.parse(expectedJson)), equalTo(testSuite));
    }
}
