$version: "2"
namespace smithy.example

// OperationOne defines a query literal `code` that conflicts with the
// query param defined in the input structure.

service SmithyExample {
    operations: [
        OperationOne
    ]
}

@http(code: 200, method: "GET", uri: "/example?code")
@readonly
operation OperationOne {
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
