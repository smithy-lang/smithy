$version: "2"
namespace smithy.example

service SmithyExample {
    operations: [
        OperationOne
    ]
}

@http(code: 200, method: "GET", uri: "/example/{greedyOne+}/bar/{nonGreedyLabel}")
@readonly
operation OperationOne {
    input: OperationOneInput
    output: OperationOneOutput
}

structure OperationOneInput {
    @httpLabel
    @required
    greedyOne: String

    @httpLabel
    @required
    nonGreedyLabel: String
}

structure OperationOneOutput {
}
