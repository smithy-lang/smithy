$version: "2.0"

namespace com.amazonaws.machinelearning

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsJson1_1
use smithy.test#httpRequestTests

@service(
    sdkId: "Machine Learning",
    arnNamespace: "machinelearning",
    cloudFormationName: "MachineLearning",
    cloudTrailEventSource: "machinelearning.amazonaws.com",
    endpointPrefix: "machinelearning",
)
@sigv4(
    name: "machinelearning",
)
@awsJson1_1
@title("Amazon Machine Learning")
@xmlNamespace(
    uri: "http://machinelearning.amazonaws.com/doc/2014-12-12/",
)
service AmazonML_20141212 {
    version: "2014-12-12",
    operations: [
        Predict,
    ],
}

@httpRequestTests([
    {
        id: "MachinelearningPredictEndpoint",
        documentation: """
            MachineLearning's api makes use of generated endpoints that the
            customer is then expected to use for the Predict operation. Having
            to alter the endpoint for a specific operation would be cumbersome,
            so an AWS client should be able to do it for them.""",
        protocol: awsJson1_1,
        method: "POST",
        uri: "/",
        host: "example.com",
        resolvedHost: "custom.example.com",
        body: "{\"MLModelId\": \"foo\", \"Record\": {}, \"PredictEndpoint\": \"https://custom.example.com/\"}",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/x-amz-json-1.1"},
        params: {
            MLModelId: "foo",
            Record: {},
            PredictEndpoint: "https://custom.example.com/",
        }
    }
])
operation Predict {
    input: PredictInput,
    output: PredictOutput,
    errors: [
        InternalServerException,
        InvalidInputException,
        LimitExceededException,
        PredictorNotMountedException,
        ResourceNotFoundException,
    ],
}

@error("server")
@httpError(500)
structure InternalServerException {
    message: ErrorMessage,
    code: ErrorCode,
}

@error("client")
@httpError(400)
structure InvalidInputException {
    message: ErrorMessage,
    code: ErrorCode,
}

@error("client")
@httpError(417)
structure LimitExceededException {
    message: ErrorMessage,
    code: ErrorCode,
}

@input
structure PredictInput {
    @required
    MLModelId: EntityId,
    @required
    Record: Record,
    @required
    PredictEndpoint: VipURLUnvalidated,
}

structure Prediction {
    predictedLabel: Label,
    predictedValue: floatLabel,
    predictedScores: ScoreValuePerLabelMap,
    details: DetailsMap,
}

@error("client")
@httpError(400)
structure PredictorNotMountedException {
    message: ErrorMessage,
}

@output
structure PredictOutput {
    Prediction: Prediction,
}

@error("client")
@httpError(404)
structure ResourceNotFoundException {
    message: ErrorMessage,
    code: ErrorCode,
}

map DetailsMap {
    key: DetailsAttributes,
    value: DetailsValue,
}

map Record {
    key: VariableName,
    value: VariableValue,
}

map ScoreValuePerLabelMap {
    key: Label,
    value: ScoreValue,
}

enum DetailsAttributes {
    PREDICTIVE_MODEL_TYPE = "PredictiveModelType"
    ALGORITHM = "Algorithm"
}

@length(
    min: 1,
)
string DetailsValue

@length(
    min: 1,
    max: 64,
)
@pattern("^[a-zA-Z0-9_.-]+$")
string EntityId

integer ErrorCode

@length(
    min: 0,
    max: 2048,
)
string ErrorMessage

float floatLabel

@length(
    min: 1,
)
string Label

float ScoreValue

string VariableName

string VariableValue

string VipURLUnvalidated
