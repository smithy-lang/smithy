$version: "2"

namespace com.example

structure NotAResource {
    id: String
}

@createsResources([
    {
        resource: "com.example#NotAResource"
        identifiers: { id: { path: "result.id" } }
    }
])
operation BadOperation {
    input: BadOperationInput
    output: BadOperationOutput
}

@input
structure BadOperationInput {}

structure BadOperationOutput {
    result: ResultData
}

structure ResultData {
    id: String
}
