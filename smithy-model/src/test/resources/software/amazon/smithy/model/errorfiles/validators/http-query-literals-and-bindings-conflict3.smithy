$version: "2"
namespace smithy.example

// OperationOne and OperationTwo define a query literal `code` and
// `code=bar` that conflicts with the query param defined in the input
// structure.

service SmithyExample {
    operations: [
        OperationOne
        OperationTwo
    ]
}

@http(code: 200, method: "GET", uri: "/one?code")
@readonly
operation OperationOne {
    input: OperationOneInput
    output: OperationOneOutput
}

@http(code: 200, method: "GET", uri: "/two?code=bar")
@readonly
operation OperationTwo {
    input: OperationOneInput
    output: OperationOneOutput
}

structure OperationOneInput {
    @httpQuery("code")
    code: String

    @httpQuery("state")
    state: String
}

structure OperationOneOutput {
    @required
    @httpHeader("Content-Type")
    contentType: String

    @required
    @httpPayload
    content: Blob
}
